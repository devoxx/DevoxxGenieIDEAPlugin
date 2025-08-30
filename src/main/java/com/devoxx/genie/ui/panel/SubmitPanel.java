package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.window.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.listener.GlowingListener;
import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

@Slf4j
public class SubmitPanel extends JBPanel<SubmitPanel>  implements GlowingListener {

    private static final int MIN_INPUT_HEIGHT = 300;

    private final Project project;
    private final DevoxxGenieToolWindowContent toolWindowContent;

    @Getter
    private final PromptInputArea promptInputArea;

    @Getter
    private final ActionButtonsPanel actionButtonsPanel;

    /**
     * The Submit Panel constructor.
     *
     * @param toolWindowContent the tool window content
     */
    public SubmitPanel(@NotNull DevoxxGenieToolWindowContent toolWindowContent) {
        super(new BorderLayout());

        this.toolWindowContent = toolWindowContent;
        this.project = toolWindowContent.getProject();
        ResourceBundle resourceBundle = toolWindowContent.getResourceBundle();

        promptInputArea = new PromptInputArea(project, resourceBundle);
        
        // Check if JCEF is available and disable input if not
        if (!JCEFChecker.isJCEFAvailable()) {
            log.warn("JCEF is not available, disabling prompt input");
            promptInputArea.setEnabled(false);
            
            // Add warning text to the placeholder
            String disabledMessage = "Prompt input is disabled because JCEF is not available. " +
                "Please enable JCEF in your IDE settings to use DevoxxGenie.";
            promptInputArea.setText(disabledMessage);
            promptInputArea.setForeground(Color.RED);
        }
        
        actionButtonsPanel = createActionButtonsPanel();
        
        // Set up file selection callback for @ key trigger
        promptInputArea.setFileSelectionCallback(() -> 
            actionButtonsPanel.selectFilesForPromptContext());
        
        // Disable action buttons if JCEF is not available
        if (!JCEFChecker.isJCEFAvailable()) {
            actionButtonsPanel.setEnabled(false);
        }

        add(createSubmitPanel(actionButtonsPanel), BorderLayout.CENTER);
    }

    /**
     * The submit panel with the prompt input area and action buttons.
     * @return the submit panel
     */
    private @NotNull JPanel createSubmitPanel(ActionButtonsPanel actionButtonsPanel) {
        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.setMinimumSize(new Dimension(0, MIN_INPUT_HEIGHT));
        submitPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, MIN_INPUT_HEIGHT));
        submitPanel.add(new PromptContextFileListPanel(project), BorderLayout.NORTH);
        submitPanel.add(new JBScrollPane(promptInputArea), BorderLayout.CENTER);
        submitPanel.add(actionButtonsPanel, BorderLayout.SOUTH);
        return submitPanel;
    }

    /**
     * The bottom action buttons panel (Submit, Search buttons and Add Files)
     * @return the action buttons panel
     */
    private @NotNull ActionButtonsPanel createActionButtonsPanel() {
        return new ActionButtonsPanel(project,
                this,
                promptInputArea,
                toolWindowContent.getPromptOutputPanel(),
                toolWindowContent.getLlmProviderPanel().getModelProviderComboBox(),
                toolWindowContent.getLlmProviderPanel().getModelNameComboBox(),
                toolWindowContent);
    }

    @Override
    public void startGlowing() {
        this.toolWindowContent.startGlowing();
    }

    @Override
    public void stopGlowing() {
        this.toolWindowContent.stopGlowing();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, 150);
    }
}
