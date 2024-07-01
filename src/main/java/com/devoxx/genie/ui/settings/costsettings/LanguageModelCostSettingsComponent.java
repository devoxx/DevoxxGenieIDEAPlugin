package com.devoxx.genie.ui.settings.costsettings;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMModelRegistryService;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.renderer.ModelProviderCellEditor;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.LLMProviderUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import static com.devoxx.genie.ui.settings.costsettings.LanguageModelCostSettingsComponent.ColumnName.*;
public class LanguageModelCostSettingsComponent extends AbstractSettingsComponent {

    private final DefaultTableModel tableModel;
    private final JSpinner windowContextSpinner;
    private boolean isModified = false;
    private final java.util.List<LLMSettingsChangeListener> listeners = new ArrayList<>();
    private final JTable costTable;

    @Getter
    public enum ColumnName {
        PROVIDER("Provider"),
        MODEL("Model"),
        INPUT_COST("Input Cost"),
        OUTPUT_COST("Output Cost"),
        CONTEXT_WINDOW("Context Window");

        private final String displayName;

        ColumnName(String displayName) {
            this.displayName = displayName;
        }
    }

    public LanguageModelCostSettingsComponent() {
        String[] columnNames = Arrays.stream(ColumnName.values())
            .map(ColumnName::getDisplayName)
            .toArray(String[]::new);

        tableModel = new DefaultTableModel(columnNames, 0);

//        tableModel.addTableModelListener(e -> {
//            if (e.getType() == TableModelEvent.UPDATE) {
//                isModified = true;
//            }
//        });

        costTable = new JBTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                ColumnName columnName = ColumnName.values()[column];
//                return columnName != PROVIDER;
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                ColumnName columnName = ColumnName.values()[column];
                return switch (columnName) {
                    case PROVIDER -> ModelProvider.class;
                    case MODEL -> String.class;
                    case INPUT_COST, OUTPUT_COST -> Double.class;
                    case CONTEXT_WINDOW -> String.class;
                };
            }
        };

        costTable.getColumnModel().getColumn(0).setCellEditor(new ModelProviderCellEditor());

        setColumnWidths();
        setCustomRenderers();

        ComboBox<ModelProvider> providerComboBox = new ComboBox<>(LLMProviderUtil.getApiKeyEnabledProviders().toArray(new ModelProvider[0]));
        costTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(providerComboBox));

        JScrollPane scrollPane = new JBScrollPane(costTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton("Add Model");
        addButton.setEnabled(false);
        addButton.setToolTipText("Add a new model not fully implemented yet, we accept PR's :)");
        addButton.addActionListener(e -> addNewRow());
        panel.add(addButton, BorderLayout.SOUTH);

        // Add window context spinner
        windowContextSpinner = new JSpinner(new SpinnerNumberModel(8000, 1000, 1000000, 1000));
        windowContextSpinner.addChangeListener(e -> isModified = true);
        JPanel contextPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contextPanel.add(new JLabel("Default Window Context:"));
        contextPanel.add(windowContextSpinner);
        panel.add(contextPanel, BorderLayout.NORTH);

        loadCurrentCosts();
    }

    private void setCustomRenderers() {
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        costTable.getColumnModel().getColumn(CONTEXT_WINDOW.ordinal()).setCellRenderer(rightRenderer);
    }

    private void setColumnWidths() {
        for (ColumnName columnName : ColumnName.values()) {
            TableColumn column = costTable.getColumnModel().getColumn(columnName.ordinal());
            switch (columnName) {
                case PROVIDER -> column.setPreferredWidth(60);
                case MODEL -> column.setPreferredWidth(220);
                case INPUT_COST, OUTPUT_COST -> column.setPreferredWidth(60);
                case CONTEXT_WINDOW -> column.setPreferredWidth(100);
            }
        }
    }

    public void addSettingsChangeListener(LLMSettingsChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (LLMSettingsChangeListener listener : listeners) {
            listener.settingsChanged();
        }
    }

    private void addNewRow() {
        Vector<Object> newRow = new Vector<>();
        newRow.add(LLMProviderUtil.getApiKeyEnabledProviders().get(0)); // Default to first provider
        newRow.add("");
        newRow.add(0.0);
        newRow.add(0.0);
        newRow.add(8000);
        tableModel.addRow(newRow);
        SwingUtilities.invokeLater(this::scrollToBottom);
    }

    private void scrollToBottom() {
        int lastRowIndex = costTable.getRowCount() - 1;
        if (lastRowIndex >= 0) {
            Rectangle cellRect = costTable.getCellRect(lastRowIndex, 0, true);
            costTable.scrollRectToVisible(cellRect);
        }
    }

    private void loadCurrentCosts() {
        LLMModelRegistryService.getInstance()
            .getModels()
            .forEach(model -> {
                tableModel.addRow(new Object[]{
                    model.getProvider().getName(),
                    model.getModelName(),
                    model.getInputCost(),
                    model.getOutputCost(),
                    NumberFormat.getInstance().format(model.getContextWindow())
                });
        });
    }

    public boolean isModified() {
        return isModified;
    }

    public void reset() {
        tableModel.setRowCount(0);
        loadCurrentCosts();
        isModified = false;
    }

    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        settings.setDefaultWindowContext((Integer) windowContextSpinner.getValue());

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object providerObj = tableModel.getValueAt(i, ColumnName.PROVIDER.ordinal());
            String modelName = (String) tableModel.getValueAt(i, ColumnName.MODEL.ordinal());
            Object inputCostObj = tableModel.getValueAt(i, ColumnName.INPUT_COST.ordinal());
            Object outputCostObj = tableModel.getValueAt(i, ColumnName.OUTPUT_COST.ordinal());
            Object windowContextObj = tableModel.getValueAt(i, ColumnName.CONTEXT_WINDOW.ordinal());

            try {
                ModelProvider provider = (ModelProvider) providerObj;
                double inputCost = Double.parseDouble(inputCostObj.toString());
                double outputCost = Double.parseDouble(outputCostObj.toString());
                int windowContext = Integer.parseInt(windowContextObj.toString());

                settings.setModelCost(provider, modelName, inputCost, outputCost);
                settings.setModelWindowContext(provider, modelName, windowContext);
            } catch (NumberFormatException | ClassCastException e) {
                // Log the error or handle it as appropriate for your application
                System.err.println("Error applying cost for model " + modelName + ": " + e.getMessage());
            }
        }
        isModified = false;
        notifyListeners();
    }
}
