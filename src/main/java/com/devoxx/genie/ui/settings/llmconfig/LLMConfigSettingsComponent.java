package com.devoxx.genie.ui.settings.llmconfig;

import com.devoxx.genie.service.settings.prompts.PromptSettingsStateService;
import com.intellij.util.ui.FormBuilder;
import lombok.Getter;

import javax.swing.*;

public class LLMConfigSettingsComponent {

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

    public LLMConfigSettingsComponent() {
        panel = FormBuilder.createFormBuilder()
            .addComponent(new JLabel("System prompt :"))
            .addComponent(systemPromptField)
            .addComponent(new JLabel("Test prompt :"))
            .addComponent(testPromptField)
            .addComponent(new JLabel("Explain prompt :"))
            .addComponent(explainPromptField)
            .addComponent(new JLabel("Review prompt :"))
            .addComponent(reviewPromptField)
            .addComponent(new JLabel("Custom prompt :"))
            .addComponent(customPromptField)
            .getPanel();
    }
}
