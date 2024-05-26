package com.devoxx.genie.ui.panel;

import com.intellij.ui.components.JBLabel;

import java.awt.*;

public class HelpPanel extends BackgroundPanel {

    /**
     * Create a help panel, listing the fixed prompt commands available
     *
     * @param helpMsg the help message
     */
    public HelpPanel(String helpMsg) {
        super("helpPanel");
        setLayout(new BorderLayout());
        withPreferredHeight(80);
        add(new JBLabel(helpMsg), BorderLayout.CENTER);
    }
}
