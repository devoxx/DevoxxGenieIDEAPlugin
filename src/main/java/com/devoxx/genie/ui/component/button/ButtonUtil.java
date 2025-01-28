package com.devoxx.genie.ui.component.button;

import com.intellij.ui.scale.JBUIScale;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.HOVER_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.TRANSPARENT_COLOR;

public class ButtonUtil {

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
        JButton button = new JButton(label);
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setToolTipText(tooltipText);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.addMouseListener(new ButtonHoverEffect(button));
        button.addActionListener(actionListener::accept);
        button.setMinimumSize(minSize);
        button.setMaximumSize(maxSize);
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        if (label == null || label.isEmpty()) {
            int fontSize = (int) JBUIScale.scale(14f) + 6;
            button.setPreferredSize(new Dimension(fontSize, 30));
        }
        
        return button;
    }

    static class ButtonHoverEffect extends MouseAdapter {
        private final JButton button;
        private final Color originalColor;

        public ButtonHoverEffect(@NotNull JButton button) {
            this.button = button;
            this.originalColor = button.getBackground();
            this.button.setBackground(TRANSPARENT_COLOR);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            button.setBackground(HOVER_BG_COLOR);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(originalColor);
        }
    }

}
