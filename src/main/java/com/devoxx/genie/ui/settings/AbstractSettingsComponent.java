package com.devoxx.genie.ui.settings;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class AbstractSettingsComponent {
    protected JPanel panel;
    protected GridBagConstraints gbc;

    protected void init() {
        panel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
    }

    public JComponent getComponent() {
        return panel;
    }

    /**
     * Set the title of the settings panel
     *
     * @param title         the title
     * @param settingsPanel the settings panel
     * @param gbc           the grid bag constraints
     */
    protected void setTitle(String title,
                          @NotNull JPanel settingsPanel,
                          @NotNull GridBagConstraints gbc) {
        JLabel titleLabel = new JLabel(title);

        gbc.insets = JBUI.insets(10, 0);
        settingsPanel.add(titleLabel, gbc);

        // Reset the insets for the next component
        gbc.insets = JBUI.emptyInsets();

        // Add vertical spacing below the title
        gbc.weighty = 1.0; // Allow the empty space to expand vertically
        settingsPanel.add(new JLabel(), gbc);

        // Reset the constraints for the next component
        gbc.weighty = 0.0;
        resetGbc(gbc);
    }

    /**
     * Add a text area with label
     * @param panel the panel
     * @param gbc   the gridbag constraints
     * @param label the label
     * @param value the value
     */
    protected void addTextAreaWithLabel(@NotNull JPanel panel,
                                      GridBagConstraints gbc,
                                      String label,
                                      String value) {
        panel.add(new JLabel(label), gbc);
        gbc.gridx++;
        JTextArea textArea = new JTextArea(value, 3, 40);
        panel.add(textArea, gbc);
        resetGbc(gbc);
    }

    /**
     * Reset the grid bag constraints
     * @param gbc the grid bag constraints
     */
    protected void resetGbc(@NotNull GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
    }
}
