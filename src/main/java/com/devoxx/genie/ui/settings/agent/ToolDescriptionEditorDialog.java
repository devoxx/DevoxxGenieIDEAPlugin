package com.devoxx.genie.ui.settings.agent;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for the LLM-facing description of a single built-in agent tool.
 *
 * <p>This is the text the model actually receives in the tool specification — not the short
 * label shown next to the tool's settings checkbox. Rewriting it lets a user steer the agent,
 * e.g. disable {@code edit_file} and state that all edits must go through {@code run_command}.
 *
 * <p>The dialog reports "no override" ({@code null}) when the text is left at, or reset to, the
 * shipped default, so an unchanged tool never stores a redundant copy of its own description.
 */
public class ToolDescriptionEditorDialog extends DialogWrapper {

    private final String defaultDescription;
    private final JBTextArea descriptionArea;

    /**
     * @param toolName          the tool being edited, e.g. {@code edit_file}
     * @param defaultDescription the shipped description, used for the reset action
     * @param currentOverride   the user's current override, or {@code null} when on the default
     */
    public ToolDescriptionEditorDialog(@NotNull String toolName,
                                       @NotNull String defaultDescription,
                                       @Nullable String currentOverride) {
        super(true);
        this.defaultDescription = defaultDescription;
        this.descriptionArea = new JBTextArea(currentOverride != null ? currentOverride : defaultDescription);
        setTitle("Edit Tool Description: " + toolName);
        setOKButtonText("Save");
        init();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(620, 320));

        JBLabel intro = new JBLabel("<html>This text is sent to the LLM as the tool's description — "
                + "it is what the model weighs when choosing between tools. Use it to say when this "
                + "tool should be used, or to steer the model toward a different one.</html>");
        panel.add(intro, BorderLayout.NORTH);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(JBUI.Borders.empty(4));
        panel.add(new JBScrollPane(descriptionArea), BorderLayout.CENTER);

        JBLabel hint = new JBLabel("Leaving this empty, or resetting it, restores the built-in description.");
        hint.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return descriptionArea;
    }

    /** A "Reset to default" button on the left of the OK/Cancel row. */
    @Override
    protected Action @NotNull [] createLeftSideActions() {
        AbstractAction resetAction = new AbstractAction("Reset to Default") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                descriptionArea.setText(defaultDescription);
                descriptionArea.setCaretPosition(0);
            }
        };
        return new Action[]{resetAction};
    }

    /**
     * The description to store, or {@code null} when the tool should keep using the shipped
     * default (text left unchanged, reset, or cleared).
     */
    public @Nullable String getOverride() {
        String text = descriptionArea.getText().trim();
        if (text.isEmpty() || text.equals(defaultDescription.trim())) {
            return null;
        }
        return text;
    }
}
