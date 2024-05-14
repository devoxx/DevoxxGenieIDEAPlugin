package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class PromptInputArea extends PlaceholderTextArea {

    /**
     * The prompt input component
     * @param resourceBundle the resource bundle
     */
    public PromptInputArea(ResourceBundle resourceBundle) {
        super();

        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(0, getPreferredSize().height));

        setLineWrap(true);
        setWrapStyleWord(true);
        setRows(3);
        setAutoscrolls(false);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setMinimumSize(new Dimension(0, 75));
        addFocusListener(new PromptInputFocusListener(this));
        setPlaceholder(resourceBundle.getString("prompt.placeholder"));
    }

    /**
     * Clear the text
     */
    public void clear() {
        setText("");
    }
}
