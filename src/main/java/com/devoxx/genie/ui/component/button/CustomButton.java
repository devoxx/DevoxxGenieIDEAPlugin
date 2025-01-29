package com.devoxx.genie.ui.component.button;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.HOVER_BG_COLOR;

public class CustomButton extends JButton {
    private boolean isHovered = false;

    public CustomButton(String text) {
        super(text);
        setupButton();
    }

    private void setupButton() {
        setContentAreaFilled(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(@NotNull Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (isHovered) {
            g2.setColor(HOVER_BG_COLOR);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g2);
        g2.dispose();
    }
}
