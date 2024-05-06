package com.devoxx.genie.ui;

import com.intellij.ui.components.JBPanel;

public class FillerPanel extends JBPanel<FillerPanel> {

    public FillerPanel(String name) {
        super();
        withMinimumHeight(5);
        setName(name);
    }
}
