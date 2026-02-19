package com.devoxx.genie.ui.settings.costsettings;

import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.SwingConstants.RIGHT;

public class LanguageModelCostSettingsComponent extends AbstractSettingsComponent {
    private final JTable costTable;
    private final SortableTableModel tableModel;

    @Getter
    private final JCheckBox showCalcTokensButtonCheckBox =
            new JCheckBox("Show Calc Tokens button in footer", stateService.getShowCalcTokensButton());

    @Getter
    private final JCheckBox showAddFileButtonCheckBox =
            new JCheckBox("Show Add File button in footer", stateService.getShowAddFileButton());

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

        tableModel = new SortableTableModel(columnNames);
        costTable = new JBTable(tableModel);
        costTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Enable sorting
        costTable.setAutoCreateRowSorter(true);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
        costTable.setRowSorter(sorter);

        // Set custom comparators for different column types
        sorter.setComparator(ColumnName.PROVIDER.ordinal(), String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(ColumnName.MODEL.ordinal(), String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(ColumnName.INPUT_COST.ordinal(), Comparator.comparingDouble(Double.class::cast));
        sorter.setComparator(ColumnName.OUTPUT_COST.ordinal(), Comparator.comparingDouble(Double.class::cast));
        sorter.setComparator(ColumnName.CONTEXT_WINDOW.ordinal(), Comparator.comparingInt(Integer.class::cast));

        // Sort by provider by default
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(ColumnName.PROVIDER.ordinal(), SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        setupColumns();
        setCustomRenderers();
        loadCurrentCosts();

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(showCalcTokensButtonCheckBox);
        optionsPanel.add(showAddFileButtonCheckBox);
        panel.add(optionsPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JBScrollPane(costTable);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/configuration/token-cost";
    }

    private void setupColumns() {
        costTable.getColumnModel().getColumn(ColumnName.PROVIDER.ordinal()).setPreferredWidth(60);
        costTable.getColumnModel().getColumn(ColumnName.MODEL.ordinal()).setPreferredWidth(220);
        costTable.getColumnModel().getColumn(ColumnName.INPUT_COST.ordinal()).setPreferredWidth(60);
        costTable.getColumnModel().getColumn(ColumnName.OUTPUT_COST.ordinal()).setPreferredWidth(60);
        costTable.getColumnModel().getColumn(ColumnName.CONTEXT_WINDOW.ordinal()).setPreferredWidth(100);
    }

    private void setCustomRenderers() {
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(RIGHT);

        // Set right alignment for numeric columns
        costTable.getColumnModel().getColumn(ColumnName.CONTEXT_WINDOW.ordinal()).setCellRenderer(rightRenderer);
        costTable.getColumnModel().getColumn(ColumnName.INPUT_COST.ordinal()).setCellRenderer(rightRenderer);
        costTable.getColumnModel().getColumn(ColumnName.OUTPUT_COST.ordinal()).setCellRenderer(rightRenderer);
    }

    private void loadCurrentCosts() {
        tableModel.setRowCount(0);
        LLMModelRegistryService.getInstance()
            .getModels()
            .forEach(model -> tableModel.addRow(new Object[]{
                model.getProvider().getName(),
                model.getModelName(),
                model.getInputCost(),
                model.getOutputCost(),
                NumberFormat.getInstance().format(model.getInputMaxTokens())
            }));
    }

    private static class SortableTableModel extends DefaultTableModel {
        public SortableTableModel(String[] columnNames) {
            super(columnNames, 0);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == ColumnName.PROVIDER.ordinal() || column == ColumnName.MODEL.ordinal()) {
                return String.class;
            } else if (column == ColumnName.INPUT_COST.ordinal() || column == ColumnName.OUTPUT_COST.ordinal()) {
                return Double.class;
            } else if (column == ColumnName.CONTEXT_WINDOW.ordinal()) {
                return Integer.class;
            }
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == ColumnName.INPUT_COST.ordinal() ||
                column == ColumnName.OUTPUT_COST.ordinal() ||
                column == ColumnName.CONTEXT_WINDOW.ordinal();
        }
    }
}
