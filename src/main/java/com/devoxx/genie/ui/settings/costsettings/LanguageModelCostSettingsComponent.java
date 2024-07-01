package com.devoxx.genie.ui.settings.costsettings;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.renderer.ModelProviderCellEditor;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DefaultLLMSettings;
import com.devoxx.genie.util.LLMProviderUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;

public class LanguageModelCostSettingsComponent extends AbstractSettingsComponent {

    private final DefaultTableModel tableModel;
    private final JSpinner windowContextSpinner;
    private boolean isModified = false;
    private final java.util.List<LLMSettingsChangeListener> listeners = new ArrayList<>();
    private final JTable costTable;

    public LanguageModelCostSettingsComponent() {

        tableModel = new DefaultTableModel(new Object[]{"Provider", "Model", "Input Cost", "Output Cost", "Context Window"}, 0);
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                isModified = true;
            }
        });

        costTable = new JBTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 2 || column == 3 || column == 4;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 0 -> ModelProvider.class;
                    case 1 -> String.class;
                    case 2, 3 -> Double.class;
                    case 4 -> Integer.class;
                    default -> Object.class;
                };
            }
        };

        costTable.getColumnModel().getColumn(0).setCellEditor(new ModelProviderCellEditor());

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
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        windowContextSpinner.setValue(settings.getDefaultWindowContext());

        for (ModelProvider provider : ModelProvider.values()) {
            if (DefaultLLMSettings.isApiBasedProvider(provider)) {
                ChatModelFactoryProvider.getFactoryByProvider(provider).ifPresent(factory -> {
                    for (LanguageModel model : factory.getModelNames()) {
                        double inputCost = settings.getModelInputCost(provider, model.getName());
                        double outputCost = settings.getModelOutputCost(provider, model.getName());
                        int windowContext = model.getMaxTokens();
                        tableModel.addRow(new Object[]{
                            provider,
                            model.getName(),
                            inputCost,
                            outputCost,
                            windowContext
                        });
                    }
                });
            }
        }
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
            ModelProvider provider = (ModelProvider) tableModel.getValueAt(i, 0);
            String modelName = (String) tableModel.getValueAt(i, 1);
            double inputCost = (double) tableModel.getValueAt(i, 2);
            double outputCost = (double) tableModel.getValueAt(i, 3);
            Object windowContextObj = tableModel.getValueAt(i, 4);

            try {
                int windowContext = windowContextObj instanceof Integer ? (Integer) windowContextObj :
                    Integer.parseInt(windowContextObj.toString());

                settings.setModelCost(provider, modelName, inputCost, outputCost);
                settings.setModelWindowContext(provider, modelName, windowContext);
            } catch (NumberFormatException e) {
                // Log the error or handle it as appropriate for your application
                System.err.println("Error applying cost for model " + modelName + ": " + e.getMessage());
            }
        }
        isModified = false;
        notifyListeners();
    }
}
