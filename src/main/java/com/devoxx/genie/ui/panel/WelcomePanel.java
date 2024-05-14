package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.util.WelcomeUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import java.awt.*;
import java.util.ResourceBundle;

public class WelcomePanel extends JBPanel<WelcomePanel> {
    private JBLabel jbLabel;

    /**
     * Create a welcome panel
     * @param resourceBundle the resource bundle
     */
    public WelcomePanel(ResourceBundle resourceBundle) {
        super(new BorderLayout());
        jbLabel = new JBLabel(WelcomeUtil.getWelcomeText(resourceBundle));
        add(jbLabel, BorderLayout.NORTH);
    }

    /**
     * Show the message
     */
    public void showMsg() {
        setVisible(true);
        jbLabel.setVisible(true);
    }
}
