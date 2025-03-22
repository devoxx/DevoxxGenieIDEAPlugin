package com.devoxx.genie.ui.settings.mcp.dialog;


import com.devoxx.genie.model.mcp.MCPServer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog for editing environment variables
 */
public class MCPEnvironmentVariablesDialog extends DialogWrapper {
    private final EnvVarTableModel tableModel;
    private final JTable envVarTable;
    private final MCPServer mcpServer;

    public MCPEnvironmentVariablesDialog(@NotNull MCPServer mcpServer) {
        super(true);
        this.mcpServer = mcpServer;
        setTitle("Environment Variables for " + mcpServer.getName());

        tableModel = new EnvVarTableModel(mcpServer.getEnv());
        envVarTable = new JBTable(tableModel);

        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(envVarTable)
                .setAddAction(button -> addEnvVar())
                .setEditAction(button -> editEnvVar())
                .setRemoveAction(button -> removeEnvVar());

        panel.add(new JLabel("Environment variables for MCP server: " + mcpServer.getName()),
                BorderLayout.NORTH);
        panel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER);

        panel.setPreferredSize(new Dimension(500, 300));

        return panel;
    }

    private void addEnvVar() {
        EnvVarDialog dialog = new EnvVarDialog(null, null);
        if (dialog.showAndGet()) {
            tableModel.addEnvVar(dialog.getKey(), dialog.getValue());
        }
    }

    private void editEnvVar() {
        int selectedRow = envVarTable.getSelectedRow();
        if (selectedRow >= 0) {
            String key = (String) tableModel.getValueAt(selectedRow, 0);
            String value = (String) tableModel.getValueAt(selectedRow, 1);

            EnvVarDialog dialog = new EnvVarDialog(key, value);
            if (dialog.showAndGet()) {
                tableModel.updateEnvVar(selectedRow, dialog.getKey(), dialog.getValue());
            }
        }
    }

    private void removeEnvVar() {
        int selectedRow = envVarTable.getSelectedRow();
        if (selectedRow >= 0) {
            int result = Messages.showYesNoDialog(
                    "Are you sure you want to remove this environment variable?",
                    "Confirm Removal",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                tableModel.removeEnvVar(selectedRow);
            }
        }
    }

    public Map<String, String> getEnvironmentVariables() {
        return tableModel.getEnvVars();
    }

    /**
     * Dialog for adding/editing environment variables
     */
    private static class EnvVarDialog extends DialogWrapper {
        private final JTextField keyField = new JTextField();
        private final JTextField valueField = new JTextField();

        public EnvVarDialog(String key, String value) {
            super(true);
            setTitle(key == null ? "Add Environment Variable" : "Edit Environment Variable");
            init();

            if (key != null) {
                keyField.setText(key);
                valueField.setText(value);

                // Disable key field when editing
                keyField.setEditable(false);
            }
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Key field
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Key:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            keyField.setPreferredSize(new Dimension(300, keyField.getPreferredSize().height));
            panel.add(keyField, gbc);

            // Value field
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            panel.add(new JLabel("Value:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            valueField.setPreferredSize(new Dimension(300, valueField.getPreferredSize().height));
            panel.add(valueField, gbc);

            return panel;
        }

        @Override
        protected @NotNull java.util.List<ValidationInfo> doValidateAll() {
            java.util.List<ValidationInfo> validationInfos = new ArrayList<>();

            if (keyField.getText().trim().isEmpty()) {
                validationInfos.add(new ValidationInfo("Key cannot be empty", keyField));
            }

            return validationInfos;
        }

        public String getKey() {
            return keyField.getText().trim();
        }

        public String getValue() {
            return valueField.getText().trim();
        }
    }

    /**
     * Table model for environment variables
     */
    private static class EnvVarTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Key", "Value"};
        private final List<Map.Entry<String, String>> entries;
        private final Map<String, String> envVars;

        public EnvVarTableModel(Map<String, String> envVars) {
            this.envVars = new HashMap<>(envVars);
            this.entries = new ArrayList<>(envVars.entrySet());
        }

        public Map<String, String> getEnvVars() {
            return new HashMap<>(envVars);
        }

        public void addEnvVar(String key, String value) {
            envVars.put(key, value);
            refreshEntries();
            fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
        }

        public void updateEnvVar(int row, String key, String value) {
            if (row >= 0 && row < entries.size()) {
                String oldKey = entries.get(row).getKey();
                envVars.remove(oldKey);
                envVars.put(key, value);
                refreshEntries();
                fireTableRowsUpdated(row, row);
            }
        }

        public void removeEnvVar(int row) {
            if (row >= 0 && row < entries.size()) {
                String key = entries.get(row).getKey();
                envVars.remove(key);
                refreshEntries();
                fireTableRowsDeleted(row, row);
            }
        }

        private void refreshEntries() {
            entries.clear();
            entries.addAll(envVars.entrySet());
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Map.Entry<String, String> entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.getKey();
                case 1 -> entry.getValue();
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
}