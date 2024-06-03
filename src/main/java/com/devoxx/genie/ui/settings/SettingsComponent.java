package com.devoxx.genie.ui.settings;

import javax.swing.*;

public interface SettingsComponent {

    /**
     * Create the settings panel
     */
    JPanel createSettingsPanel();

    /**
     * Adds listeners to the settings panel components
     */
    void addListeners();
}
