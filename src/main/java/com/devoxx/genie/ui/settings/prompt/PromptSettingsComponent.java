package com.devoxx.genie.ui.settings.prompt;

import com.intellij.util.ui.FormBuilder;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import com.intellij.ui.components.JBLabel;
import javax.swing.*;

public class PromptSettingsComponent {

    public PromptSettingsStateService promptSettingsStateService = PromptSettingsStateService.getInstance();

    @Getter
    private final JTextArea systemPromptField = new JTextArea(promptSettingsStateService.getSystemPrompt());
    @Getter
    private final JTextArea testPromptField = new JTextArea(promptSettingsStateService.getTestPrompt());
    @Getter
    private final JTextArea explainPromptField = new JTextArea(promptSettingsStateService.getExplainPrompt());
    @Getter
    private final JTextArea reviewPromptField = new JTextArea(promptSettingsStateService.getReviewPrompt());
    @Getter
    private final JTextArea customPromptField = new JTextArea(promptSettingsStateService.getCustomPrompt());

    @Getter
    private final JPanel panel;

    public PromptSettingsComponent() {
        panel = FormBuilder.createFormBuilder()
            .addComponent(new JXTitledSeparator("Prompts"))
            .addVerticalGap(5)
            .addLabeledComponentFillVertically(
                "System Prompt",
                systemPromptField
            )
            .addLabeledComponent(
                new JBLabel("Test Prompt"),
                testPromptField,
                10,
                true
                )
            .addLabeledComponent(
                new JBLabel("Explain Prompt"),
                explainPromptField,
                10,
                true
            )
            .addLabeledComponent(
                new JBLabel("Review Prompt"),
                reviewPromptField,
                10,
                true
            )
            .addLabeledComponent(
                new JBLabel("Custom Prompt"),
                customPromptField,
                10,
                true
            )
            .getPanel();
    }
}
