package com.devoxx.genie.ui.component.button;

import com.devoxx.genie.ui.panel.FileSelectionPanelFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.scale.JBUIScale;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.devoxx.genie.model.Constant.ADD_FILES_TO_CONTEXT_TOOLTIP;
import static com.devoxx.genie.model.Constant.FILTER_AND_DOUBLE_CLICK_TO_ADD_TO_PROMPT_CONTEXT;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.AddFileIcon;

/**
 * Reusable "Add files to prompt context" button. Opens the same file-picker popup
 * regardless of where it's hosted; callers control what happens with the selected
 * file via the {@code onSelected} consumer (chat panel registers it with the
 * {@code FileListManager}; the Agentic Platform panel inserts the path into its
 * prompt area).
 */
public class AddFilesToContextButton extends CustomButton {

    /** How the popup is positioned relative to the button. */
    @FunctionalInterface
    public interface PopupAnchor extends BiConsumer<JBPopup, AddFilesToContextButton> {
    }

    private static final PopupAnchor DEFAULT_ANCHOR = (popup, btn) -> popup.showUnderneathOf(btn);

    private final transient Project project;
    private final transient Consumer<VirtualFile> onSelected;
    private final transient PopupAnchor anchor;

    public AddFilesToContextButton(@NotNull Project project,
                                   @NotNull Consumer<VirtualFile> onSelected) {
        this(project, onSelected, DEFAULT_ANCHOR);
    }

    public AddFilesToContextButton(@NotNull Project project,
                                   @NotNull Consumer<VirtualFile> onSelected,
                                   @NotNull PopupAnchor anchor) {
        super("");
        this.project = project;
        this.onSelected = onSelected;
        this.anchor = anchor;

        // Match the icon-only styling that ButtonFactory.createActionButton applies, so this
        // button sits flush with the submit button instead of showing a stock JButton border.
        setIcon(AddFileIcon);
        int buttonSize = JBUIScale.scale(28);
        Dimension square = new Dimension(buttonSize, buttonSize);
        setPreferredSize(square);
        setMinimumSize(square);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setToolTipText(ADD_FILES_TO_CONTEXT_TOOLTIP);
        setMnemonic('A');
        getAccessibleContext().setAccessibleDescription("Add files to context");
        addActionListener(e -> openPicker());
    }

    /**
     * Opens the file-picker popup. Public so external callbacks (e.g. drag-and-drop on the
     * prompt area) can trigger the same flow without going through a button click.
     */
    public void openPicker() {
        if (!isShowing()) {
            return;
        }
        List<VirtualFile> sortedFiles = new ArrayList<>(List.of(
                FileEditorManager.getInstance(project).getOpenFiles()));
        sortedFiles.sort(Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));

        JPanel fileSelectionPanel = FileSelectionPanelFactory.createPanel(project, sortedFiles, onSelected);
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(fileSelectionPanel, null)
                .setTitle(FILTER_AND_DOUBLE_CLICK_TO_ADD_TO_PROMPT_CONTEXT)
                .setRequestFocus(true)
                .setResizable(true)
                .setMovable(false)
                .setMinSize(new Dimension(300, 350))
                .createPopup();

        anchor.accept(popup, this);

        // Focus the filter field once the popup is realised on screen.
        SwingUtilities.invokeLater(() -> {
            Component focusable = findFocusableComponent(fileSelectionPanel);
            if (focusable != null) {
                focusable.requestFocusInWindow();
            }
        });
    }

    private static Component findFocusableComponent(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField && component.isFocusable()) {
                return component;
            }
            if (component instanceof Container nested) {
                Component found = findFocusableComponent(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
