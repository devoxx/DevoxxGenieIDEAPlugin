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
        this.repaint();
    }

    public void setUsedTokens(int usedTokens) {
        this.usedTokens = usedTokens;
        this.repaint();
    }

    public void reset() {
        this.usedTokens = 0;
        this.maxTokens = 100;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();

        int usageWidth = (int) ((double) usedTokens / maxTokens * width);
        g.setColor(getColor(usageWidth));
        g.fillRect(0, 0, usageWidth, height);
    }

    private Color getColor(int usageWidth) {

        if (usageWidth < 50) {
            return JBColor.GREEN;
        } else if (usageWidth < 80) {
            return JBColor.YELLOW;
        } else {
            return JBColor.BLUE;
        }
    }
}
