package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class PromptInputArea extends JPanel {

    private final CommandAutoCompleteTextField inputField;

    public PromptInputArea(ResourceBundle resourceBundle) {
        super(new BorderLayout());

        inputField = new CommandAutoCompleteTextField();
        inputField.setRows(3);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setAutoscrolls(false);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.setMinimumSize(new Dimension(0, 75));
        inputField.addFocusListener(new PromptInputFocusListener(inputField));
        inputField.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

        JScrollPane scrollPane = new JScrollPane(inputField);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
    }

    public String getText() {
        return inputField.getText();
    }

    public void setText(String text) {
        inputField.setText(text);
    }

    public void clear() {
        inputField.setText("");
    }

    public void setEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
    }

    public boolean requestFocusInWindow() {
        return inputField.requestFocusInWindow();
    }

    public void setPlaceholder(String placeholder) {
        inputField.setPlaceholder(placeholder);
    }
}
