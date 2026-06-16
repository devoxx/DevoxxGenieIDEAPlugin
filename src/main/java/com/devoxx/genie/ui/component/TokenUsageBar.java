package com.devoxx.genie.ui.component;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

public class TokenUsageBar extends JComponent {
    private int maxTokens = 100;
    private int usedTokens;

    public TokenUsageBar() {
        setPreferredSize(new Dimension(200, 20));
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        repaint();
    }

    public void setTokens(int usedTokens, int maxTokens) {
        this.usedTokens = usedTokens;
        this.maxTokens = maxTokens;
        repaint();
    }

    public void reset() {
        this.usedTokens = 0;
        this.maxTokens = 100;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();

        double ratio = maxTokens <= 0 ? 0 : Math.min(1.0, (double) usedTokens / maxTokens);
        int usageWidth = (int) (ratio * width);
        g.setColor(getColor(ratio));
        g.fillRect(0, 0, usageWidth, height);
    }

    /**
     * Colors the bar by how full the context window is: green below 50%, yellow up to 80%,
     * red beyond that to warn the user before the window overflows.
     */
    private Color getColor(double ratio) {
        if (ratio < 0.5) {
            return JBColor.GREEN;
        } else if (ratio < 0.8) {
            return JBColor.YELLOW;
        } else {
            return JBColor.RED;
        }
    }
}
