package com.devoxx.genie.ui.listener;

import com.devoxx.genie.ui.component.PlaceholderTextArea;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class PromptInputFocusListener extends FocusAdapter {

    private final PlaceholderTextArea promptInputArea;

    public PromptInputFocusListener(PlaceholderTextArea promptInputArea) {
        this.promptInputArea = promptInputArea;
    }

    @Override
    public void focusGained(FocusEvent e) {
        promptInputArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new JBColor(new Color(37, 150, 190), new Color(37, 150, 190))),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        promptInputArea.requestFocusInWindow();
    }

    @Override
    public void focusLost(FocusEvent e) {
        promptInputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
}
