package com.devoxx.genie.ui.component.button;

import com.intellij.ui.scale.JBUIScale;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ButtonFactory {

    // Set minimum size for buttons to prevent them from becoming too small
    private final static Dimension minSize = new Dimension(100, 30);
    private final static Dimension maxSize = new Dimension(200, 30);

    public static @NotNull JButton createActionButton(String label,
                                                      @NotNull Consumer<ActionEvent> actionListener) {
        return createActionButton(label, null, "", actionListener);
    }

    public static @NotNull JButton createActionButton(Icon icon,
                                                      @NotNull Consumer<ActionEvent> actionListener) {
        return createActionButton("", icon, "", actionListener);
    }

    public static @NotNull JButton createActionButton(Icon icon,
                                                      String tooltipText,
                                                      @NotNull Consumer<ActionEvent> actionListener) {
        return createActionButton("", icon, tooltipText, actionListener);
    }

    public static @NotNull JButton createActionButton(String label,
                                                      Icon icon,
                                                      @NotNull Consumer<ActionEvent> actionListener) {
        return createActionButton(label, icon, "", actionListener);
    }

    public static @NotNull JButton createActionButton(String label,
                                                Icon icon,
                                                String tooltipText,
                                                @NotNull Consumer<ActionEvent> actionListener) {
        JButton button = new CustomButton(label);
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setToolTipText(tooltipText);
        button.addActionListener(actionListener::accept);
        button.setMinimumSize(minSize);
        button.setMaximumSize(maxSize);
        button.setPreferredSize(minSize);
        button.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (label == null || label.isEmpty()) {
            // For icon-only buttons, ensure they have enough space at all zoom levels
            int buttonSize = JBUIScale.scale(28);
            button.setPreferredSize(new Dimension(buttonSize, buttonSize));
            button.setMinimumSize(new Dimension(buttonSize, buttonSize));
        }
        
        return button;
    }
}
