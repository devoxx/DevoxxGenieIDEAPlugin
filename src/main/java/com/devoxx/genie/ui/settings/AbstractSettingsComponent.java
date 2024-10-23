package com.devoxx.genie.ui.settings;

import javax.swing.*;
import java.awt.*;

public class AbstractSettingsComponent implements SettingsComponent {

    protected final JPanel panel = new JPanel(new BorderLayout());

    @Override
    public JPanel createPanel() {
        return panel;
    }

    @Override
    public void addListeners() {
    }
}
