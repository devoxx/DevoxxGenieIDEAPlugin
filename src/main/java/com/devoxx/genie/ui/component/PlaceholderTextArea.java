package com.devoxx.genie.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class PlaceholderTextArea extends JTextArea {

    private String placeholder;

    public PlaceholderTextArea(int rows, int columns) {
        super(rows, columns);
        this.addFocusListener(new FocusAdapter());
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (placeholder.isEmpty() || !getText().isEmpty()) {
            return;
        }

        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        g2d.setColor(getDisabledTextColor());
        g2d.drawString(placeholder, getInsets().left, g.getFontMetrics()
            .getMaxAscent() + getInsets().top);
    }

    private class FocusAdapter implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            repaint();
        }
    }
}
