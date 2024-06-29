package com.devoxx.genie.ui.settings.llmsettings;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.settings.SettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DefaultLLMSettings;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

public class LLMCostSettingsComponent implements SettingsComponent {

    private final JPanel panel;
    private final DefaultTableModel tableModel;
    private final JSpinner windowContextSpinner;
    private boolean isModified = false;
    private final java.util.List<LLMSettingsChangeListener> listeners = new ArrayList<>();

    public LLMCostSettingsComponent() {
        panel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"Provider", "Model", "Input Cost", "Output Cost", "Window Context"}, 0);
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                isModified = true;
            }
        });

        JBTable costTable = new JBTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3 || column == 4; // Allow editing of cost and window context columns
            }
        };

        JBScrollPane scrollPane = new JBScrollPane(costTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton("Add Model");
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
        tableModel.addRow(new Object[]{"", "", 0.0, 0.0, 0.0});
    }

    @Override
    public JPanel createSettingsPanel() {
        return panel;
    }

    @Override
    public void addListeners() {
        // No additional listeners needed for this component
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
                            provider.getName(),
                            model.getName(),
                            String.format("%.6f", inputCost),
                            String.format("%.6f", outputCost),
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
            String providerName = (String) tableModel.getValueAt(i, 0);
            String modelName = (String) tableModel.getValueAt(i, 1);
            String inputCostString = (String) tableModel.getValueAt(i, 2);
            String outputCostString = (String) tableModel.getValueAt(i, 3);
            Object windowContextObj = tableModel.getValueAt(i, 4);

            ModelProvider provider = ModelProvider.fromString(providerName);
            NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
            try {
                double inputCost = format.parse(inputCostString).doubleValue();
                double outputCost = format.parse(outputCostString).doubleValue();
                int windowContext = windowContextObj instanceof Integer ? (Integer) windowContextObj :
                    Integer.parseInt(windowContextObj.toString());

                settings.setModelCost(provider, modelName, inputCost, outputCost);
                settings.setModelWindowContext(provider, modelName, windowContext);
            } catch (ParseException | NumberFormatException e) {
                // Log the error or handle it as appropriate for your application
                System.err.println("Error applying cost for model " + modelName + ": " + e.getMessage());
            }
        }
        isModified = false;
        notifyListeners();
    }
}
