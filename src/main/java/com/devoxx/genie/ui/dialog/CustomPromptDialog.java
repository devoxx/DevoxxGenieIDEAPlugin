package com.devoxx.genie.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CustomPromptDialog extends DialogWrapper {
    private final JBTextField nameField;
    private final JBTextArea promptArea;

    public CustomPromptDialog(Project project) {
        this(project, "", "");
    }

    // New constructor for editing existing prompts
    public CustomPromptDialog(Project project, @NotNull String initialCommand, String initialPrompt) {
        super(project);
        setTitle(initialCommand.isEmpty() ? "Add Custom Prompt" : "Edit Custom Prompt");

        nameField = new JBTextField(20);
        promptArea = new JBTextArea(10, 40);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);

        // Set initial values for editing
        nameField.setText(initialCommand);
        promptArea.setText(initialPrompt);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));

        // Name input
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(new JLabel("Command Name:"), BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);

        // Prompt input
        JPanel promptPanel = new JPanel(new BorderLayout());
        promptPanel.add(new JLabel("Prompt:"), BorderLayout.NORTH);
        JBScrollPane scrollPane = new JBScrollPane(promptArea);
        promptPanel.add(scrollPane, BorderLayout.CENTER);

        dialogPanel.add(namePanel, BorderLayout.NORTH);
        dialogPanel.add(promptPanel, BorderLayout.CENTER);

        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        if (validateInput()) {
            super.doOKAction();
        }
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            Messages.showErrorDialog("The command name cannot be empty.", "Invalid Name");
            return false;
        }
        if (promptArea.getText().trim().isEmpty()) {
            Messages.showErrorDialog("The prompt cannot be empty.", "Invalid Prompt");
            return false;
        }
        return true;
    }

    public String getCommandName() {
        return nameField.getText().trim();
    }

    public String getPrompt() {
        return promptArea.getText().trim();
    }
}
