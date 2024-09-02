package com.devoxx.genie.ui.listener;

import com.devoxx.genie.ui.component.RoundBorder;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_INPUT_BORDER;

public class PromptInputFocusListener extends FocusAdapter {

    private final JBTextArea promptInputArea;

    public PromptInputFocusListener(JBTextArea promptInputArea) {
        this.promptInputArea = promptInputArea;
    }

    @Override
    public void focusGained(FocusEvent e) {
        promptInputArea.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(PROMPT_INPUT_BORDER, 1, 5),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        promptInputArea.requestFocusInWindow();
    }

    @Override
    public void focusLost(FocusEvent e) {
        promptInputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
}
