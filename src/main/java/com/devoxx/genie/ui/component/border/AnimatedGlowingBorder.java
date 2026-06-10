package com.devoxx.genie.ui.component.border;

import com.devoxx.genie.ui.listener.GlowingListener;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Breathing glow border shown around the submit area while a prompt is executing.
 *
 * <p>NOTE (TASK-235): this 50ms {@link javax.swing.Timer} is the <b>only remaining
 * Swing animation</b> in the plugin — all other UI transitions are Compose-based
 * (see {@code com.devoxx.genie.ui.compose}). Migrate this to Compose when
 * {@code ActionButtonsPanel}/{@code SubmitPanel} move to Compose.</p>
 */
public class AnimatedGlowingBorder implements Border, GlowingListener {
    private final Timer glowTimer;
    private final GlowingBorder glowingBorder;
    private boolean isGlowing = false;
    private final JComponent component;
    private final Border defaultBorder;

    public AnimatedGlowingBorder(@NotNull JComponent component) {
        this.component = component;
        this.defaultBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        this.glowingBorder = new GlowingBorder(
                new JBColor(new Color(0, 120, 215), new Color(0, 120, 213)));
        this.glowTimer = createGlowTimer();
    }

    private Timer createGlowTimer() {
        return new Timer(50, new ActionListener() {
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
                component.repaint();
            }
        });
    }

    @Override
    public void startGlowing() {
        if (!isGlowing) {
            isGlowing = true;
            component.setBorder(glowingBorder);
            glowTimer.start();
        }
    }

    @Override
    public void stopGlowing() {
        // Always stop the timer, even when no glow is currently showing. stopGlowing()
        // is invoked from every execution-end path (complete, error, user stop — all
        // funnel through PromptExecutionController.endPromptExecution →
        // ActionButtonsPanel.enableButtons) and must be idempotent so the 50ms
        // breathing timer can never keep running behind a hidden border.
        glowTimer.stop();
        if (isGlowing) {
            isGlowing = false;
            component.setBorder(defaultBorder);
            component.repaint();
        }
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (isGlowing) {
            glowingBorder.paintBorder(c, g, x, y, width, height);
        } else {
            defaultBorder.paintBorder(c, g, x, y, width, height);
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return isGlowing ? glowingBorder.getBorderInsets(c) : defaultBorder.getBorderInsets(c);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
