package com.devoxx.genie.ui.component;

import com.intellij.ui.components.JBTextArea;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

@Setter
@Getter
public class PlaceholderTextArea extends JBTextArea {

    private String placeholder;

    public PlaceholderTextArea() {
        super();
        this.addFocusListener(new FocusAdapter());
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (placeholder == null || placeholder.isEmpty() || !getText().isEmpty()) {
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
