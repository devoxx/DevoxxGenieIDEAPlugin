package com.devoxx.genie.ui;

import javax.swing.*;

public class SelectionPanel extends JPanel {
    private final JComboBox<String> llmProvidersComboBox = new JComboBox<>();
    private final JComboBox<String> modelNameComboBox = new JComboBox<>();

    public SelectionPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(llmProvidersComboBox);
        add(Box.createVerticalStrut(5));
        add(modelNameComboBox);
        // other initialization code
    }

    public JComboBox<String> getLlmProvidersComboBox() {
        return llmProvidersComboBox;
    }

    public JComboBox<String> getModelNameComboBox() {
        return modelNameComboBox;
    }
}
