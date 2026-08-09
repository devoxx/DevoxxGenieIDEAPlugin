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

/**
 * Dialog for adding or editing a persona (a named system prompt) in the Prompts settings.
 */
public class PersonaDialog extends DialogWrapper {
    private final JBTextField nameField;
    private final JBTextArea promptArea;

    public PersonaDialog(Project project) {
        this(project, "", "");
    }

    public PersonaDialog(Project project, @NotNull String initialName, String initialPrompt) {
        super(project);
        setTitle(initialName.isEmpty() ? "Add Persona" : "Edit Persona");

        nameField = new JBTextField(20);
        promptArea = new JBTextArea(10, 40);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);

        nameField.setText(initialName);
        promptArea.setText(initialPrompt);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));

        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(new JLabel("Persona Name:"), BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);

        JPanel promptPanel = new JPanel(new BorderLayout());
        promptPanel.add(new JLabel("System Prompt:"), BorderLayout.NORTH);
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
            Messages.showErrorDialog("The persona name cannot be empty.", "Invalid Name");
            return false;
        }
        if (promptArea.getText().trim().isEmpty()) {
            Messages.showErrorDialog("The system prompt cannot be empty.", "Invalid Prompt");
            return false;
        }
        return true;
    }

    public String getPersonaName() {
        return nameField.getText().trim();
    }

    public String getPrompt() {
        return promptArea.getText().trim();
    }
}
