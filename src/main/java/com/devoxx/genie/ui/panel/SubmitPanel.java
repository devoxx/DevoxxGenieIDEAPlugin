package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class SubmitPanel extends JBPanel<SubmitPanel> {

    public static final int MIN_INPUT_HEIGHT = 200;
    private final Project project;
    private final DevoxxGenieToolWindowContent toolWindowContent;
    @Getter
    private final PromptInputArea promptInputArea;
    @Getter
    private ActionButtonsPanel actionButtonsPanel;

    /**
     * The Submit Panel constructor.
     *
     * @param toolWindowContent the tool window content
     */
    public SubmitPanel(DevoxxGenieToolWindowContent toolWindowContent)
    {
        super(new BorderLayout());
        this.toolWindowContent = toolWindowContent;
        this.project = toolWindowContent.getProject();
        ResourceBundle resourceBundle = toolWindowContent.getResourceBundle();

        PromptContextFileListPanel promptContextFileListPanel = new PromptContextFileListPanel(project);
        promptInputArea = new PromptInputArea(resourceBundle, project);

        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.setMinimumSize(new Dimension(0, MIN_INPUT_HEIGHT));
        submitPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, MIN_INPUT_HEIGHT));

        submitPanel.add(promptContextFileListPanel, BorderLayout.NORTH);
        submitPanel.add(new JBScrollPane(promptInputArea), BorderLayout.CENTER);
        submitPanel.add(createActionButtonsPanel(), BorderLayout.SOUTH);

        add(submitPanel);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, 150);
    }

    /**
     * The bottom action buttons panel (Submit, Search buttons and Add Files)
     *
     * @return the action buttons panel
     */
    @Contract(" -> new")
    private @NotNull JPanel createActionButtonsPanel() {
        actionButtonsPanel = new ActionButtonsPanel(project,
                promptInputArea,
                toolWindowContent.getPromptOutputPanel(),
                toolWindowContent.getLlmProviderPanel().getModelProviderComboBox(),
                toolWindowContent.getLlmProviderPanel().getModelNameComboBox(),
                toolWindowContent);
        return actionButtonsPanel;
    }
}
