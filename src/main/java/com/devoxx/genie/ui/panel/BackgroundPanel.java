package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.component.RoundBorder;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieColors.DEFAULT_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieColors.GRAY_COLOR;

public class BackgroundPanel extends JBPanel<BackgroundPanel> {

    /**
     * The background panel.
     * @param name the name of the panel
     */
    public BackgroundPanel(String name) {
        super.setName(name);
        setName(name);
        setBackground(DEFAULT_BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(GRAY_COLOR, 1, 5),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2d.dispose();
    }
}
