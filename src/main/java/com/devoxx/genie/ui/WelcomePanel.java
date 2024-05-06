package com.devoxx.genie.ui;

import com.devoxx.genie.ui.util.WelcomeUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import java.awt.*;
import java.util.ResourceBundle;

public class WelcomePanel extends JBPanel<WelcomePanel> {
    JBLabel jbLabel;

    public WelcomePanel(ResourceBundle resourceBundle) {
        super(new BorderLayout());
        jbLabel = new JBLabel(WelcomeUtil.getWelcomeText(resourceBundle));
        add(jbLabel, BorderLayout.NORTH);
    }

    public void showMsg() {
        setVisible(true);
        jbLabel.setVisible(true);
    }
}
