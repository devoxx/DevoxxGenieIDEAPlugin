package com.devoxx.genie.ui.util;

import javax.swing.*;
import java.awt.*;

public class ScaledIcon implements Icon {
    private final Icon originalIcon;
    private final int width;
    private final int height;

    public ScaledIcon(Icon originalIcon, int width) {
        this.originalIcon = originalIcon;
        this.width = width;
        this.height = width;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale((double) width / originalIcon.getIconWidth(), (double) height / originalIcon.getIconHeight());
        originalIcon.paintIcon(c, g2, 0, 0);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
