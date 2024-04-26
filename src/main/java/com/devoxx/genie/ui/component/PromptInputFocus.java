package com.devoxx.genie.ui.component;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class PromptInputFocus extends FocusAdapter {

    private final JTextArea promptInputArea;

    public PromptInputFocus(JTextArea promptInputArea) {
        this.promptInputArea = promptInputArea;
    }

    @Override
    public void focusGained(FocusEvent e) {
        promptInputArea.setBorder(BorderFactory.createLineBorder(
            new JBColor(new Color(37, 150, 190), new Color(37, 150, 190)))
        );
        promptInputArea.requestFocusInWindow();
    }

    @Override
    public void focusLost(FocusEvent e) {
        promptInputArea.setBorder(null);
    }
}
