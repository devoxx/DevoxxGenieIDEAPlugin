package com.devoxx.genie.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
public class JHoverButton extends JButton {

    private final Color hoverBackgroundColor;
    private final Color normalBackgroundColor;

    public JHoverButton(Icon icon) {
        super(icon);
        normalBackgroundColor = new Color(0, 0, 0, 0); // Transparent
        hoverBackgroundColor = new Color(192, 192, 192, 50); // Semi-transparent light grey
        initUI();
    }

    private void initUI() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setOpaque(true);
                setBackground(hoverBackgroundColor);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setOpaque(false);
                setBackground(normalBackgroundColor);
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getModel().isSelected()) {
            Icon icon = getIcon();
            if (icon != null) {
                int iconWidth = icon.getIconWidth();
                int iconHeight = icon.getIconHeight();
                int x = (getWidth() - iconWidth) / 2;
                int y = (getHeight() - iconHeight) / 2;
                g.setColor(getBackground());
                g.fillRect(x, y, iconWidth, iconHeight);
            }
        }
        super.paintComponent(g);
    }
}
