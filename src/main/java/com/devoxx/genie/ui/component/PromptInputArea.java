package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

public class PromptInputArea extends JPanel {

    private final CommandAutoCompleteTextField inputField;
    private final GlowingBorder glowingBorder;
    private final Timer glowTimer;
    private boolean isGlowing = false;

    public PromptInputArea(@NotNull ResourceBundle resourceBundle) {
        super(new BorderLayout());

        inputField = new CommandAutoCompleteTextField();
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.addFocusListener(new PromptInputFocusListener(inputField));
        inputField.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

        glowingBorder = new GlowingBorder(new Color(0, 120, 215)); // You can change this color
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4)); // To accommodate the glowing border

        add(inputField, BorderLayout.CENTER);

        glowTimer = new Timer(50, new ActionListener() {
            private float direction = 0.05f;
            @Override
            public void actionPerformed(ActionEvent e) {
                float alpha = glowingBorder.getAlpha();
                alpha += direction;
                if (alpha > 1.0f) {
                    alpha = 1.0f;
                    direction = -0.05f;
                } else if (alpha < 0.3f) {
                    alpha = 0.3f;
                    direction = 0.05f;
                }
                glowingBorder.setAlpha(alpha);
                repaint();
            }
        });
    }

    public void startGlowing() {
        if (!isGlowing) {
            isGlowing = true;
            setBorder(glowingBorder);
            glowTimer.start();
        }
    }

    public void stopGlowing() {
        if (isGlowing) {
            isGlowing = false;
            glowTimer.stop();
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            repaint();
        }
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
}
