package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.service.settings.PromptSettingsStateService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

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
