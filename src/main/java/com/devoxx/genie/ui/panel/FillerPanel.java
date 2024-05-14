package com.devoxx.genie.ui.panel;

import com.intellij.ui.components.JBPanel;

public class FillerPanel extends JBPanel<FillerPanel> {

    /**
     * Create a filler panel.  Maybe this could be replaced with border padding?
     * @param name the name of the panel
     */
    public FillerPanel(String name) {
        super();
        withMinimumHeight(5);
        setName(name);
    }
}
