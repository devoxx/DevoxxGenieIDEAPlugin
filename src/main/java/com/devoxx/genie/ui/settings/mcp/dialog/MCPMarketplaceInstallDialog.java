package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.registry.*;
import com.devoxx.genie.service.mcp.MCPRegistryService;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MCPMarketplaceInstallDialog extends DialogWrapper {

    private final MCPRegistryServerInfo serverInfo;
    private final EnvVarConfigTableModel envVarModel = new EnvVarConfigTableModel();
    private final JBTable envVarTable = new JBTable(envVarModel);

    // Editable command field for STDIO package servers
    private final JBTextField commandField = new JBTextField();
    private final JBTextField argsField = new JBTextField();

    @Getter
    private MCPServer configuredServer;

    public MCPMarketplaceInstallDialog(@NotNull MCPRegistryServerInfo serverInfo) {
        super(true);
        this.serverInfo = serverInfo;
        setTitle("Install MCP Server: " + serverInfo.getName());
        setOKButtonText("Add Server");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        // Server name
        mainPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel nameLabel = new JLabel(serverInfo.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(nameLabel, gbc);

        // Version
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Version:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        mainPanel.add(new JLabel(serverInfo.getVersion() != null ? serverInfo.getVersion() : "N/A"), gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextArea descArea = new JTextArea(serverInfo.getDescription() != null ? serverInfo.getDescription() : "");
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setRows(3);
        descArea.setBackground(mainPanel.getBackground());
        mainPanel.add(new JBScrollPane(descArea), gbc);

        // Repository link
        if (serverInfo.getRepository() != null && serverInfo.getRepository().getUrl() != null
                && !serverInfo.getRepository().getUrl().isBlank()) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0;
            mainPanel.add(new JLabel("Repository:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            JButton repoLink = new JButton(serverInfo.getRepository().getUrl());
            repoLink.setBorderPainted(false);
            repoLink.setContentAreaFilled(false);
            repoLink.setForeground(new Color(0x589DF6));
            repoLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            repoLink.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI(serverInfo.getRepository().getUrl()));
                } catch (Exception ex) {
                    log.error("Failed to open repository URL", ex);
                }
            });
            mainPanel.add(repoLink, gbc);
        }

        // Transport type info
        String serverType = MCPRegistryService.getInstance().getServerType(serverInfo);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        mainPanel.add(new JLabel(serverType), gbc);

        // Connection info
        boolean isRemote = serverInfo.getRemotes() != null && !serverInfo.getRemotes().isEmpty();
        if (isRemote) {
            MCPRegistryRemote remote = serverInfo.getRemotes().get(0);
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0;
            mainPanel.add(new JLabel("URL:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            mainPanel.add(new JLabel(remote.getUrl()), gbc);
        } else if (serverInfo.getPackages() != null && !serverInfo.getPackages().isEmpty()) {
            MCPRegistryPackage pkg = serverInfo.getPackages().get(0);
            String command;
            String args;
            if ("npm".equalsIgnoreCase(pkg.getRegistryType())) {
                command = "npx";
                args = "-y " + pkg.getIdentifier();
            } else if ("oci".equalsIgnoreCase(pkg.getRegistryType())) {
                command = "docker";
                args = "run -i --rm " + pkg.getIdentifier();
            } else {
                command = pkg.getIdentifier();
                args = "";
            }

            commandField.setText(command);
            argsField.setText(args);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0;
            mainPanel.add(new JLabel("Command:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            mainPanel.add(commandField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0;
            mainPanel.add(new JLabel("Arguments:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            mainPanel.add(argsField, gbc);

            // Info note
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            gbc.weightx = 1;
            String toolName = "npm".equalsIgnoreCase(pkg.getRegistryType()) ? "npx" : "docker";
            JLabel infoNote = new JLabel("Note: Requires " + toolName + " on your system PATH");
            infoNote.setFont(infoNote.getFont().deriveFont(Font.ITALIC));
            mainPanel.add(infoNote, gbc);
            gbc.gridwidth = 1;
        }

        // Populate environment variables / headers table
        populateEnvVars(isRemote);

        if (envVarModel.getRowCount() > 0) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            gbc.weightx = 1;
            mainPanel.add(new JLabel("Configuration:"), gbc);

            envVarTable.setRowHeight(28);
            envVarTable.getColumnModel().getColumn(0).setPreferredWidth(180);
            envVarTable.getColumnModel().getColumn(1).setPreferredWidth(300);
            envVarTable.getColumnModel().getColumn(2).setPreferredWidth(60);

            // Use password field renderer/editor for secret fields
            envVarTable.getColumnModel().getColumn(1).setCellRenderer(new SecretCellRenderer());
            envVarTable.getColumnModel().getColumn(1).setCellEditor(new SecretCellEditor());

            JBScrollPane envScrollPane = new JBScrollPane(envVarTable);
            envScrollPane.setPreferredSize(new Dimension(540, 150));

            gbc.gridy++;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1;
            mainPanel.add(envScrollPane, gbc);
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
        }

        mainPanel.setPreferredSize(new Dimension(600, 450));
        return mainPanel;
    }

    private void populateEnvVars(boolean isRemote) {
        List<EnvVarEntry> entries = new ArrayList<>();

        if (isRemote && serverInfo.getRemotes() != null && !serverInfo.getRemotes().isEmpty()) {
            MCPRegistryRemote remote = serverInfo.getRemotes().get(0);
            if (remote.getHeaders() != null) {
                for (MCPRegistryHeader header : remote.getHeaders()) {
                    entries.add(new EnvVarEntry(
                            header.getName(),
                            header.getValue() != null ? header.getValue() : "",
                            header.getDescription(),
                            Boolean.TRUE.equals(header.getIsRequired()),
                            Boolean.TRUE.equals(header.getIsSecret())
                    ));
                }
            }
        } else if (serverInfo.getPackages() != null && !serverInfo.getPackages().isEmpty()) {
            MCPRegistryPackage pkg = serverInfo.getPackages().get(0);
            if (pkg.getEnvironmentVariables() != null) {
                for (MCPRegistryEnvVar envVar : pkg.getEnvironmentVariables()) {
                    entries.add(new EnvVarEntry(
                            envVar.getName(),
                            "",
                            envVar.getDescription(),
                            Boolean.TRUE.equals(envVar.getIsRequired()),
                            Boolean.TRUE.equals(envVar.getIsSecret())
                    ));
                }
            }
        }

        envVarModel.setEntries(entries);
    }

    @Override
    protected void doOKAction() {
        // Stop any active cell editing to capture typed values
        if (envVarTable.isEditing()) {
            envVarTable.getCellEditor().stopCellEditing();
        }

        Map<String, String> envValues = envVarModel.getValues();

        boolean isRemote = serverInfo.getRemotes() != null && !serverInfo.getRemotes().isEmpty();

        if (isRemote) {
            configuredServer = MCPRegistryService.getInstance().convertToMCPServer(serverInfo, envValues);
        } else {
            // For package servers, use the user-edited command/args
            String command = commandField.getText().trim();
            String argsText = argsField.getText().trim();
            List<String> argsList = argsText.isEmpty() ? List.of() : List.of(argsText.split("\\s+"));

            Map<String, String> env = new HashMap<>();
            for (Map.Entry<String, String> entry : envValues.entrySet()) {
                if (!entry.getValue().isBlank()) {
                    env.put(entry.getKey(), entry.getValue());
                }
            }

            configuredServer = MCPServer.builder()
                    .name(serverInfo.getName())
                    .transportType(MCPServer.TransportType.STDIO)
                    .command(command)
                    .args(argsList)
                    .env(env)
                    .build();
        }

        super.doOKAction();
    }

    /**
     * An entry in the configuration table.
     */
    private static class EnvVarEntry {
        final String name;
        String value;
        final String description;
        final boolean required;
        final boolean secret;

        EnvVarEntry(String name, String value, String description, boolean required, boolean secret) {
            this.name = name;
            this.value = value;
            this.description = description;
            this.required = required;
            this.secret = secret;
        }
    }

    /**
     * Table model for environment variable / header configuration.
     */
    private static class EnvVarConfigTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Value", "Required"};
        private final List<EnvVarEntry> entries = new ArrayList<>();

        public void setEntries(List<EnvVarEntry> newEntries) {
            entries.clear();
            entries.addAll(newEntries);
            fireTableDataChanged();
        }

        public Map<String, String> getValues() {
            Map<String, String> map = new HashMap<>();
            for (EnvVarEntry entry : entries) {
                map.put(entry.name, entry.value);
            }
            return map;
        }

        public EnvVarEntry getEntryAt(int row) {
            return row >= 0 && row < entries.size() ? entries.get(row) : null;
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
            EnvVarEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.name + (entry.description != null && !entry.description.isBlank()
                        ? " - " + entry.description : "");
                case 1 -> entry.value;
                case 2 -> entry.required ? "Yes" : "No";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1; // Only the value column is editable
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1 && aValue instanceof String) {
                entries.get(rowIndex).value = (String) aValue;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }

    /**
     * Renders secret fields as masked text.
     */
    private class SecretCellRenderer implements TableCellRenderer {
        private final JPasswordField passwordField = new JPasswordField();
        private final JLabel normalLabel = new JLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            EnvVarEntry entry = envVarModel.getEntryAt(row);
            if (entry != null && entry.secret && value instanceof String s && !s.isEmpty()) {
                passwordField.setText(s);
                if (isSelected) {
                    passwordField.setBackground(table.getSelectionBackground());
                    passwordField.setForeground(table.getSelectionForeground());
                } else {
                    passwordField.setBackground(table.getBackground());
                    passwordField.setForeground(table.getForeground());
                }
                return passwordField;
            }
            normalLabel.setText(value != null ? value.toString() : "");
            if (isSelected) {
                normalLabel.setBackground(table.getSelectionBackground());
                normalLabel.setForeground(table.getSelectionForeground());
            } else {
                normalLabel.setBackground(table.getBackground());
                normalLabel.setForeground(table.getForeground());
            }
            normalLabel.setOpaque(true);
            return normalLabel;
        }
    }

    /**
     * Editor that uses a password field for secret entries.
     */
    private class SecretCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPasswordField passwordField = new JPasswordField();
        private final JTextField textField = new JTextField();
        private boolean isSecret;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            EnvVarEntry entry = envVarModel.getEntryAt(row);
            isSecret = entry != null && entry.secret;
            String val = value != null ? value.toString() : "";
            if (isSecret) {
                passwordField.setText(val);
                return passwordField;
            }
            textField.setText(val);
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            if (isSecret) {
                return new String(passwordField.getPassword());
            }
            return textField.getText();
        }
    }
}
