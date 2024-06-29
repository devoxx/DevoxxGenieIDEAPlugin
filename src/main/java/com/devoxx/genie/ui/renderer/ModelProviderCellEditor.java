package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.util.LLMProviderUtil;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;

public class ModelProviderCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final ComboBox<ModelProvider> comboBox;

    public ModelProviderCellEditor() {
        List<ModelProvider> providers = LLMProviderUtil.getApiKeyEnabledProviders();
        comboBox = new ComboBox<>(providers.toArray(new ModelProvider[0]));
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        return comboBox;
    }

    @Override
    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }
}
