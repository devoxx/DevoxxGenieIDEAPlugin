package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.devoxx.genie.ui.util.DevoxxGenieFontsUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Custom renderer for the Model Provider dropdown to ensure consistent font scaling with ModelInfoRenderer
 */
public class ModelProviderRenderer extends JPanel implements ListCellRenderer<ModelProvider> {
    private final JLabel nameLabel = new JBLabel();

    public ModelProviderRenderer() {
        setLayout(new BorderLayout());
        add(nameLabel, BorderLayout.CENTER);
        setBorder(JBUI.Borders.empty(2));
        
        // Use the centralized dropdown font
        nameLabel.setFont(DevoxxGenieFontsUtil.getDropdownFont());
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ModelProvider> list,
                                                  ModelProvider provider,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        if (provider == null) {
            nameLabel.setText("");
        } else {
            nameLabel.setText(provider.getName());
        }

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        setEnabled(list.isEnabled());
        
        // Always ensure the font is set correctly (protection against JList overriding it)
        nameLabel.setFont(DevoxxGenieFontsUtil.getDropdownFont());

        return this;
    }
}
