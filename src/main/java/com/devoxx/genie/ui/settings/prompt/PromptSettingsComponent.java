package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.SettingsComponent;
import com.intellij.util.ui.FormBuilder;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import com.intellij.ui.components.JBLabel;
import javax.swing.*;

public class PromptSettingsComponent implements SettingsComponent {

    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    @Getter
    private final JTextArea systemPromptField = new JTextArea(stateService.getSystemPrompt());
    @Getter
    private final JTextArea testPromptField = new JTextArea(stateService.getTestPrompt());
    @Getter
    private final JTextArea explainPromptField = new JTextArea(stateService.getExplainPrompt());
    @Getter
    private final JTextArea reviewPromptField = new JTextArea(stateService.getReviewPrompt());
    @Getter
    private final JTextArea customPromptField = new JTextArea(stateService.getCustomPrompt());

    public PromptSettingsComponent() {
        addListeners();
    }

    @Override
    public JPanel createSettingsPanel() {
        return FormBuilder.createFormBuilder()
            .addComponent(new JXTitledSeparator("Prompts"))
            .addVerticalGap(5)
            .addLabeledComponentFillVertically(
                "System prompt",
                systemPromptField
            )
            .addLabeledComponent(
                new JBLabel("Test prompt"),
                testPromptField,
                10,
                true
            )
            .addLabeledComponent(
                new JBLabel("Explain prompt"),
                explainPromptField,
                10,
                true
            )
            .addLabeledComponent(
                new JBLabel("Review prompt"),
                reviewPromptField,
                10,
                true
            )
            .addLabeledComponent(
                new JBLabel("Custom prompt"),
                customPromptField,
                10,
                true
            )
            .getPanel();
    }

    @Override
    public void addListeners() {

    }
}
