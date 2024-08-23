package com.devoxx.genie.ui.panel;

import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

public class HelpPanel extends BackgroundPanel {
    private final JEditorPane helpPane;

    public HelpPanel(String helpMsg) {
        super("helpPanel");
        setLayout(new BorderLayout());

        helpPane = new JEditorPane("text/html", "");
        helpPane.setEditable(false);
        helpPane.setOpaque(false);
        helpPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JBScrollPane scrollPane = new JBScrollPane(helpPane);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        add(scrollPane, BorderLayout.CENTER);
        updateHelpText(helpMsg);
    }

    public void updateHelpText(String newHelpMsg) {
        helpPane.setText(newHelpMsg);
        updatePanelSize();
    }

    private void updatePanelSize() {
        SwingUtilities.invokeLater(() -> {
            int preferredHeight = calculatePreferredHeight();
            setPreferredSize(new Dimension(getWidth(), preferredHeight));
            revalidate();
            repaint();
        });
    }

    private int calculatePreferredHeight() {
        int contentHeight = helpPane.getPreferredSize().height;
        int maxHeight = 300; // Set a maximum height if needed
        return Math.min(contentHeight + 20, maxHeight); // Add some padding
    }
}
