package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.util.WelcomeUtil;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.Desktop;
import java.net.URI;
import java.util.ResourceBundle;

public class WelcomePanel extends JBPanel<WelcomePanel> {
    private final JEditorPane jEditorPane;
    private final JBScrollPane scrollPane;

    public WelcomePanel(ResourceBundle resourceBundle) {
        super(new BorderLayout());

        jEditorPane = new JEditorPane("text/html", WelcomeUtil.getWelcomeText(resourceBundle));
        jEditorPane.setEditable(false);
        jEditorPane.setOpaque(false);
        jEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        jEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Create a JBScrollPane and add the JEditorPane to it
        scrollPane = new JBScrollPane(jEditorPane);
        scrollPane.setVerticalScrollBarPolicy(JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);  // Remove border for a cleaner look

        // Add the scrollPane to the panel
        add(scrollPane, BorderLayout.CENTER);

        // Set a preferred size for the panel
        setPreferredSize(new Dimension(600, 400));  // Adjust these values as needed
    }

    public void showMsg() {
        setVisible(true);
        jEditorPane.setVisible(true);
        scrollPane.setVisible(true);

        // Ensure the scroll pane is at the top when shown
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMinimum());
        });
    }
}
