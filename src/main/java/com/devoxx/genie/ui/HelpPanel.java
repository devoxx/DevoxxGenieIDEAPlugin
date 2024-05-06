package com.devoxx.genie.ui;

import com.intellij.ui.components.JBLabel;

import java.awt.*;

public class HelpPanel extends BackgroundPanel {

    public HelpPanel(String helpMsg) {
        super("helpPanel");
        withPreferredHeight(80);
        add(new JBLabel(helpMsg), BorderLayout.CENTER);
    }
}
