package com.devoxx.genie.ui.component;

import lombok.Getter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class InputSwitch extends JPanel {

    private final Timer timer;
    private float location;
    @Getter
    private boolean selected;
    private boolean mouseOver;
    private static final float SPEED = 0.1f;
    private final transient List<EventSwitchSelected> events;
    private final JLabel textLabel;
    private final SwitchButton switchButton;

    public InputSwitch(String label, String tooltip) {
        setLayout(new BorderLayout(5, 0));
        events = new ArrayList<>();

        // Create the switch button as a separate component
        switchButton = new SwitchButton();
        textLabel = new JLabel(label);

        // Add components
        add(switchButton, BorderLayout.WEST);
        add(textLabel, BorderLayout.CENTER);
        // Add some padding after the switch
        textLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        // Set tooltip for both components
        textLabel.setToolTipText(tooltip);
        switchButton.setToolTipText(tooltip);

        // Initialize timer
        timer = new Timer(0, new MyActionListener());
    }

    public void addEventSelected(EventSwitchSelected event) {
        events.add(event);
    }

    // Inner class for the actual switch button
    private class SwitchButton extends JComponent {

        public SwitchButton() {
            setPreferredSize(new Dimension(30, 10));
            setBackground(new Color(255, 165, 0));
            setForeground(Color.WHITE);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            location = 2;

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent me) {
                    mouseOver = true;
                }

                @Override
                public void mouseExited(MouseEvent me) {
                    mouseOver = false;
                }

                @Override
                public void mouseReleased(MouseEvent me) {
                    if (SwingUtilities.isLeftMouseButton(me)) {
                        if (mouseOver) {
                            selected = !selected;
                            timer.start();
                            runEvent();
                        }
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics grphcs) {
            Graphics2D g2 = (Graphics2D) grphcs;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            float alpha = getAlpha();
            if (alpha < 1) {
                g2.setColor(Color.GRAY);
                g2.fillRoundRect(0, 0, width, height, 15, 15);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, width, height, 15, 15);
            g2.setColor(getForeground());
            g2.setComposite(AlphaComposite.SrcOver);
            g2.fillOval((int) location, 2, height - 4, height - 4);
        }

        private float getAlpha() {
            float width = switchButton.getWidth() - switchButton.getHeight();
            float alpha = (location - 2) / width;
            if (alpha < 0) {
                alpha = 0;
            }
            if (alpha > 1) {
                alpha = 1;
            }
            return alpha;
        }
    }

    private void runEvent() {
        for (EventSwitchSelected event : events) {
            event.onSelected(selected);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        timer.start();
        runEvent();
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        textLabel.setToolTipText(text);
        switchButton.setToolTipText(text);
    }

    private class MyActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            if (isSelected()) {
                int endLocation = switchButton.getWidth() - switchButton.getHeight() + 2;
                if (location < endLocation) {
                    location += SPEED;
                    switchButton.repaint();
                } else {
                    timer.stop();
                    location = endLocation;
                    switchButton.repaint();
                }
            } else {
                int endLocation = 2;
                if (location > endLocation) {
                    location -= SPEED;
                    switchButton.repaint();
                } else {
                    timer.stop();
                    location = endLocation;
                    switchButton.repaint();
                }
            }
        }
    }
}