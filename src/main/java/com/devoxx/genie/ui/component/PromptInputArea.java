package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.devoxx.genie.ui.panel.SearchOptionsPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

public class PromptInputArea extends JPanel {
    private final CommandAutoCompleteTextField inputField;
    private final GlowingBorder glowingBorder;
    private final Timer glowTimer;
    private boolean isGlowing = false;

    public PromptInputArea(@NotNull ResourceBundle resourceBundle, Project project) {
        super(new BorderLayout());

        // Create main input area panel
        JPanel inputAreaPanel = new JPanel(new BorderLayout());
        inputField = new CommandAutoCompleteTextField(project);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.addFocusListener(new PromptInputFocusListener(inputField));
        inputField.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

        // Set minimum size for 2 lines
        FontMetrics fontMetrics = inputField.getFontMetrics(inputField.getFont());
        int lineHeight = fontMetrics.getHeight();
        int minHeight = (lineHeight * 3) +
                inputField.getInsets().top +
                inputField.getInsets().bottom +
                10; // Additional padding

        Dimension minimumSize = new Dimension(0, minHeight);
        inputField.setMinimumSize(minimumSize);
        inputField.setPreferredSize(minimumSize);

        glowingBorder = new GlowingBorder(new JBColor(new Color(0, 120, 215), new Color(0, 120, 213)));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Add components to main panel
        inputAreaPanel.add(new SearchOptionsPanel(), BorderLayout.NORTH);
        inputAreaPanel.add(inputField, BorderLayout.CENTER);

        add(inputAreaPanel, BorderLayout.CENTER);

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

    public void requestInputFocus() {
        ApplicationManager.getApplication().invokeLater(() -> {
            inputField.requestFocusInWindow();
            inputField.setCaretPosition(inputField.getText().length());
        });
    }
}
