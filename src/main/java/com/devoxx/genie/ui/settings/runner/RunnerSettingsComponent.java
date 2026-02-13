package com.devoxx.genie.ui.settings.runner;

import com.devoxx.genie.model.spec.AcpToolConfig;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings panel for CLI and ACP Runners configuration.
 */
public class RunnerSettingsComponent extends AbstractSettingsComponent {

    private final Project project;

    private final CliToolTableModel cliToolTableModel = new CliToolTableModel();
    private final JBTable cliToolTable = new JBTable(cliToolTableModel);

    private final AcpToolTableModel acpToolTableModel = new AcpToolTableModel();
    private final JBTable acpToolTable = new JBTable(acpToolTableModel);

    public RunnerSettingsComponent(@NotNull Project project) {
        this.project = project;
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy = 0;

        // --- CLI Runners ---
        addSection(contentPanel, gbc, "CLI Runners");

        addHelpText(contentPanel, gbc,
                "Configure external CLI tools (e.g., GitHub Copilot CLI, Claude Code, Gemini CLI) " +
                "that can execute spec tasks instead of the built-in LLM provider. " +
                "CLI tools must have the Backlog MCP server installed so they can update task status. " +
                "Select the execution mode in the Spec Browser toolbar.");

        // Configure table columns
        cliToolTable.getColumnModel().getColumn(0).setMaxWidth(60);   // Enabled
        cliToolTable.getColumnModel().getColumn(0).setMinWidth(60);
        cliToolTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Type
        cliToolTable.getColumnModel().getColumn(2).setPreferredWidth(220); // Path
        cliToolTable.getColumnModel().getColumn(3).setPreferredWidth(170); // MCP Config Flag
        cliToolTable.setRowHeight(25);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(cliToolTable)
                .setAddAction(button -> addCliTool())
                .setEditAction(button -> editCliTool())
                .setRemoveAction(button -> removeCliTool());

        JPanel tablePanel = decorator.createPanel();
        tablePanel.setPreferredSize(new Dimension(-1, 150));

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(tablePanel, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy++;

        // Load existing CLI tools from state
        loadCliTools();

        // --- ACP Runners ---
        addSection(contentPanel, gbc, "ACP Runners");

        addHelpText(contentPanel, gbc,
                "Configure external ACP (Agent Communication Protocol) tools that communicate " +
                "via JSON-RPC 2.0 over stdin/stdout. ACP provides structured streaming, file operations, " +
                "terminal management, and capability negotiation. " +
                "Supported CLIs: Kimi, Gemini CLI, Kilocode.");

        // Configure ACP table columns
        acpToolTable.getColumnModel().getColumn(0).setMaxWidth(60);   // Enabled
        acpToolTable.getColumnModel().getColumn(0).setMinWidth(60);
        acpToolTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Type
        acpToolTable.getColumnModel().getColumn(2).setPreferredWidth(270); // Path
        acpToolTable.setRowHeight(25);

        ToolbarDecorator acpDecorator = ToolbarDecorator.createDecorator(acpToolTable)
                .setAddAction(button -> addAcpTool())
                .setEditAction(button -> editAcpTool())
                .setRemoveAction(button -> removeAcpTool());

        JPanel acpTablePanel = acpDecorator.createPanel();
        acpTablePanel.setPreferredSize(new Dimension(-1, 150));

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(acpTablePanel, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy++;

        // Load existing ACP tools from state
        loadAcpTools();

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
    }

    // ===== CLI Tool Table Management =====

    private void loadCliTools() {
        List<CliToolConfig> tools = stateService.getCliTools();
        if (tools != null) {
            for (CliToolConfig tool : tools) {
                cliToolTableModel.addTool(tool);
            }
        }
        // Pre-populate with Copilot if no tools configured
        if (cliToolTableModel.getRowCount() == 0) {
            com.devoxx.genie.service.cli.command.CliCommand cmd = CliToolConfig.CliType.COPILOT.createCommand();
            List<String> defaultArgs = new ArrayList<>();
            for (String arg : cmd.defaultExtraArgs().split("\\s+")) {
                if (!arg.isEmpty()) defaultArgs.add(arg);
            }
            cliToolTableModel.addTool(CliToolConfig.builder()
                    .type(CliToolConfig.CliType.COPILOT)
                    .name(CliToolConfig.CliType.COPILOT.getDisplayName())
                    .executablePath(cmd.defaultExecutablePath())
                    .extraArgs(defaultArgs)
                    .mcpConfigFlag(cmd.defaultMcpConfigFlag())
                    .enabled(true)
                    .build());
        }
    }

    private void addCliTool() {
        CliToolDialog dialog = new CliToolDialog(null);
        if (dialog.showAndGet()) {
            cliToolTableModel.addTool(dialog.getResult());
        }
    }

    private void editCliTool() {
        int row = cliToolTable.getSelectedRow();
        if (row < 0) return;
        CliToolConfig existing = cliToolTableModel.getToolAt(row);
        CliToolDialog dialog = new CliToolDialog(existing);
        if (dialog.showAndGet()) {
            cliToolTableModel.updateTool(row, dialog.getResult());
        }
    }

    private void removeCliTool() {
        int row = cliToolTable.getSelectedRow();
        if (row >= 0) {
            cliToolTableModel.removeTool(row);
        }
    }

    // ===== ACP Tool Table Management =====

    private void loadAcpTools() {
        List<AcpToolConfig> tools = stateService.getAcpTools();
        if (tools != null) {
            for (AcpToolConfig tool : tools) {
                acpToolTableModel.addTool(tool);
            }
        }
    }

    private void addAcpTool() {
        AcpToolDialog dialog = new AcpToolDialog(null);
        if (dialog.showAndGet()) {
            acpToolTableModel.addTool(dialog.getResult());
        }
    }

    private void editAcpTool() {
        int row = acpToolTable.getSelectedRow();
        if (row < 0) return;
        AcpToolConfig existing = acpToolTableModel.getToolAt(row);
        AcpToolDialog dialog = new AcpToolDialog(existing);
        if (dialog.showAndGet()) {
            acpToolTableModel.updateTool(row, dialog.getResult());
        }
    }

    private void removeAcpTool() {
        int row = acpToolTable.getSelectedRow();
        if (row >= 0) {
            acpToolTableModel.removeTool(row);
        }
    }

    // ===== Helper Methods =====

    private void addFullWidthRow(JPanel panel, GridBagConstraints gbc, JComponent component) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        panel.add(component, gbc);
        gbc.gridy++;
    }

    private void addHelpText(JPanel panel, GridBagConstraints gbc, String text) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 25, 8, 5);
        JTextArea helpArea = new JTextArea(text);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setEditable(false);
        helpArea.setFocusable(false);
        helpArea.setOpaque(false);
        helpArea.setBorder(null);
        helpArea.setFont(UIManager.getFont("Label.font").deriveFont((float) UIManager.getFont("Label.font").getSize() - 1));
        helpArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(helpArea, gbc);
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy++;
    }

    // ===== Shared UI Helpers =====

    private static JTextArea createHelpArea() {
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(UIManager.getFont("Label.font").deriveFont((float) UIManager.getFont("Label.font").getSize() - 1));
        area.setForeground(UIManager.getColor("Label.disabledForeground"));
        return area;
    }

    // ===== State Management =====

    public boolean isModified() {
        return isCliToolsModified() || isAcpToolsModified();
    }

    private boolean isCliToolsModified() {
        List<CliToolConfig> saved = stateService.getCliTools();
        List<CliToolConfig> current = cliToolTableModel.getAllTools();
        if (saved == null) return !current.isEmpty();
        return !saved.equals(current);
    }

    private boolean isAcpToolsModified() {
        List<AcpToolConfig> saved = stateService.getAcpTools();
        List<AcpToolConfig> current = acpToolTableModel.getAllTools();
        if (saved == null) return !current.isEmpty();
        return !saved.equals(current);
    }

    public void apply() {
        stateService.setCliTools(new ArrayList<>(cliToolTableModel.getAllTools()));
        stateService.setAcpTools(new ArrayList<>(acpToolTableModel.getAllTools()));
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        cliToolTableModel.clear();
        List<CliToolConfig> tools = state.getCliTools();
        if (tools != null) {
            for (CliToolConfig tool : tools) {
                cliToolTableModel.addTool(tool);
            }
        }

        acpToolTableModel.clear();
        List<AcpToolConfig> acpTools = state.getAcpTools();
        if (acpTools != null) {
            for (AcpToolConfig tool : acpTools) {
                acpToolTableModel.addTool(tool);
            }
        }
    }

    @Override
    public void addListeners() {
        // No dynamic listeners needed
    }

    // ===== CLI Tool Table Model =====

    private static class CliToolTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Enabled", "Type", "Executable Path", "MCP Config Flag"};
        private final List<CliToolConfig> tools = new ArrayList<>();

        @Override
        public int getRowCount() {
            return tools.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0; // Only "Enabled" is directly editable
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CliToolConfig tool = tools.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> tool.isEnabled();
                case 1 -> tool.getType() != null ? tool.getType().getDisplayName() : "";
                case 2 -> tool.getExecutablePath();
                case 3 -> tool.getMcpConfigFlag() != null ? tool.getMcpConfigFlag() : "";
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean) {
                tools.get(rowIndex).setEnabled((Boolean) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        public void addTool(CliToolConfig tool) {
            tools.add(tool);
            fireTableRowsInserted(tools.size() - 1, tools.size() - 1);
        }

        public void updateTool(int row, CliToolConfig tool) {
            tools.set(row, tool);
            fireTableRowsUpdated(row, row);
        }

        public void removeTool(int row) {
            tools.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public CliToolConfig getToolAt(int row) {
            return tools.get(row);
        }

        public List<CliToolConfig> getAllTools() {
            return new ArrayList<>(tools);
        }

        public void clear() {
            int size = tools.size();
            if (size > 0) {
                tools.clear();
                fireTableRowsDeleted(0, size - 1);
            }
        }
    }

    // ===== CLI Tool Dialog =====

    private static class CliToolDialog extends com.intellij.openapi.ui.DialogWrapper {
        private final JComboBox<CliToolConfig.CliType> typeCombo = new JComboBox<>(CliToolConfig.CliType.values());
        private final JBTextField pathField = new JBTextField();
        private final JBTextField argsField = new JBTextField();
        private final JBTextField envVarsField = new JBTextField();
        private final JBTextField mcpConfigFlagField = new JBTextField();
        private final JBCheckBox enabledCheckbox = new JBCheckBox("Enabled", true);
        private final JBLabel testResultLabel = new JBLabel();
        private final JTextArea pathHelpArea = createHelpArea();
        private boolean suppressTypeListener = false;

        CliToolDialog(CliToolConfig existing) {
            super(true);
            setTitle(existing == null ? "Add CLI Tool" : "Edit CLI Tool");
            if (existing != null) {
                suppressTypeListener = true;
                typeCombo.setSelectedItem(existing.getType() != null ? existing.getType() : CliToolConfig.CliType.CUSTOM);
                suppressTypeListener = false;
                pathField.setText(existing.getExecutablePath());
                argsField.setText(existing.getExtraArgs() != null ? String.join(" ", existing.getExtraArgs()) : "");
                envVarsField.setText(formatEnvVars(existing.getEnvVars()));
                mcpConfigFlagField.setText(existing.getMcpConfigFlag() != null ? existing.getMcpConfigFlag() : "");
                enabledCheckbox.setSelected(existing.isEnabled());
                updatePathHelp(existing.getType());
            }
            init();
            // Pre-fill defaults for the initially selected type when adding a new entry
            if (existing == null) {
                onTypeChanged();
            }
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;

            // Type selector (first row)
            gbc.gridy = 0;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JBLabel("Type:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            typeCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, sel, focus);
                    if (value instanceof CliToolConfig.CliType t) setText(t.getDisplayName());
                    return this;
                }
            });
            typeCombo.addActionListener(e -> onTypeChanged());
            panel.add(typeCombo, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JBLabel("Executable path:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(pathField, gbc);

            // Dynamic help text below executable path
            gbc.gridy++;
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(pathHelpArea, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JBLabel("Extra args:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(argsField, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JBLabel("Env vars:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            envVarsField.getEmptyText().setText("KEY=VALUE, KEY2=VALUE2");
            panel.add(envVarsField, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JBLabel("MCP config flag:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            mcpConfigFlagField.setEditable(false);
            mcpConfigFlagField.setEnabled(false);
            panel.add(mcpConfigFlagField, gbc);

            // Help text for MCP config
            gbc.gridy++;
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JBLabel mcpHelpLabel = new JBLabel("Backlog MCP config is auto-generated and passed to the CLI tool");
            mcpHelpLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            mcpHelpLabel.setFont(mcpHelpLabel.getFont().deriveFont((float) mcpHelpLabel.getFont().getSize() - 1));
            panel.add(mcpHelpLabel, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            panel.add(enabledCheckbox, gbc);

            // Test button row
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            JPanel testRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            JButton testButton = new JButton("Test Connection");
            testButton.addActionListener(e -> runTest());
            testRow.add(testButton);
            testResultLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            testRow.add(testResultLabel);
            panel.add(testRow, gbc);

            return panel;
        }

        private void onTypeChanged() {
            if (suppressTypeListener) return;
            CliToolConfig.CliType type = (CliToolConfig.CliType) typeCombo.getSelectedItem();
            if (type == null) return;
            updatePathHelp(type);
            if (type == CliToolConfig.CliType.CUSTOM) return;

            // Delegate to the Command for this type — no switch needed
            com.devoxx.genie.service.cli.command.CliCommand command = type.createCommand();
            pathField.setText(command.defaultExecutablePath());
            argsField.setText(command.defaultExtraArgs());
            mcpConfigFlagField.setText(command.defaultMcpConfigFlag());
        }

        private void updatePathHelp(CliToolConfig.CliType type) {
            if (type == null || type == CliToolConfig.CliType.CUSTOM) {
                pathHelpArea.setText("Find the full path by running: whereis <executable>");
            } else {
                com.devoxx.genie.service.cli.command.CliCommand cmd = type.createCommand();
                pathHelpArea.setText("Find the full path by running: whereis " + cmd.defaultExecutablePath());
            }
        }

        private void runTest() {
            String path = pathField.getText().trim();
            if (path.isEmpty()) {
                testResultLabel.setForeground(UIManager.getColor("Component.errorFocusColor"));
                testResultLabel.setText("Executable path is empty");
                return;
            }

            testResultLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            testResultLabel.setText("Testing...");

            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // Use the CliCommand abstraction to build command and deliver prompt
                    // This ensures tool-specific behavior (e.g., --prompt flag for Kimi)
                    CliToolConfig testConfig = getResult();
                    String testPrompt = "Respond with only: OK";
                    CliToolConfig.CliType cliType = testConfig.getType() != null
                            ? testConfig.getType() : CliToolConfig.CliType.CUSTOM;
                    com.devoxx.genie.service.cli.command.CliCommand cliCommand = cliType.createCommand();
                    java.util.List<String> command = cliCommand.buildProcessCommand(testConfig, testPrompt, null);

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(false);

                    // Inherit the user's shell environment (PATH, tokens, etc.)
                    pb.environment().putAll(com.intellij.util.EnvironmentUtil.getEnvironmentMap());

                    // Overlay with tool-specific env var overrides
                    if (testConfig.getEnvVars() != null && !testConfig.getEnvVars().isEmpty()) {
                        pb.environment().putAll(testConfig.getEnvVars());
                    }

                    Process process = pb.start();

                    // Delegate prompt delivery to the command
                    cliCommand.writePrompt(process, testPrompt);

                    // Read stdout and stderr in parallel
                    StringBuilder stdout = new StringBuilder();
                    StringBuilder stderr = new StringBuilder();

                    Thread stdoutReader = new Thread(() -> {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(process.getInputStream()))) {
                            String line;
                            int count = 0;
                            while ((line = reader.readLine()) != null && count < 10) {
                                if (stdout.length() > 0) stdout.append(" ");
                                stdout.append(line);
                                count++;
                            }
                        } catch (java.io.IOException ignored) {}
                    });
                    Thread stderrReader = new Thread(() -> {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(process.getErrorStream()))) {
                            String line;
                            int count = 0;
                            while ((line = reader.readLine()) != null && count < 10) {
                                if (stderr.length() > 0) stderr.append(" ");
                                stderr.append(line);
                                count++;
                            }
                        } catch (java.io.IOException ignored) {}
                    });

                    stdoutReader.setDaemon(true);
                    stderrReader.setDaemon(true);
                    stdoutReader.start();
                    stderrReader.start();

                    boolean exited = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                    if (!exited) {
                        process.destroyForcibly();
                        showTestResult(false, "Timed out after 30s");
                        return;
                    }

                    stdoutReader.join(3000);
                    stderrReader.join(3000);

                    int exitCode = process.exitValue();
                    if (exitCode == 0) {
                        showTestResult(true, "Connected successfully");
                    } else {
                        // Prefer stderr for error messages, fall back to stdout
                        String err = stderr.toString().trim();
                        if (err.isEmpty()) err = stdout.toString().trim();
                        if (err.isEmpty()) err = "Exit code " + exitCode;
                        showTestResult(false, err);
                    }

                } catch (java.io.IOException ex) {
                    showTestResult(false, "Not found: " + ex.getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showTestResult(false, "Interrupted");
                }
            });
        }

        private void showTestResult(boolean success, String message) {
            SwingUtilities.invokeLater(() -> {
                String truncated = message.length() > 80 ? message.substring(0, 80) + "..." : message;
                if (success) {
                    testResultLabel.setForeground(new java.awt.Color(0, 128, 0));
                    testResultLabel.setText(truncated);
                } else {
                    testResultLabel.setForeground(UIManager.getColor("Component.errorFocusColor"));
                    testResultLabel.setText(truncated);
                }
            });
        }

        private static String formatEnvVars(java.util.Map<String, String> envVars) {
            if (envVars == null || envVars.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (var entry : envVars.entrySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            return sb.toString();
        }

        /**
         * Strip surrounding shell-style quotes from an argument.
         * ProcessBuilder doesn't use a shell, so literal quotes must be removed.
         */
        private static String stripShellQuotes(String arg) {
            if (arg.length() >= 2) {
                if ((arg.startsWith("'") && arg.endsWith("'")) ||
                        (arg.startsWith("\"") && arg.endsWith("\""))) {
                    return arg.substring(1, arg.length() - 1);
                }
            }
            return arg;
        }

        private static java.util.Map<String, String> parseEnvVars(String text) {
            java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
            if (text == null || text.isEmpty()) return map;
            for (String pair : text.split(",")) {
                String trimmed = pair.trim();
                int eq = trimmed.indexOf('=');
                if (eq > 0 && eq < trimmed.length() - 1) {
                    map.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                }
            }
            return map;
        }

        public CliToolConfig getResult() {
            List<String> args = new ArrayList<>();
            String argsText = argsField.getText().trim();
            if (!argsText.isEmpty()) {
                for (String arg : argsText.split("\\s+")) {
                    args.add(stripShellQuotes(arg));
                }
            }
            CliToolConfig.CliType selectedType = (CliToolConfig.CliType) typeCombo.getSelectedItem();
            if (selectedType == null) selectedType = CliToolConfig.CliType.CUSTOM;
            return CliToolConfig.builder()
                    .type(selectedType)
                    .name(selectedType.getDisplayName())
                    .executablePath(pathField.getText().trim())
                    .extraArgs(args)
                    .envVars(parseEnvVars(envVarsField.getText().trim()))
                    .mcpConfigFlag(mcpConfigFlagField.getText().trim())
                    .enabled(enabledCheckbox.isSelected())
                    .build();
        }
    }

    // ===== ACP Tool Table Model =====

    private static class AcpToolTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Enabled", "Type", "Executable Path"};
        private final List<AcpToolConfig> tools = new ArrayList<>();

        @Override
        public int getRowCount() {
            return tools.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AcpToolConfig tool = tools.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> tool.isEnabled();
                case 1 -> tool.getType() != null ? tool.getType().getDisplayName() : "";
                case 2 -> tool.getExecutablePath();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean) {
                tools.get(rowIndex).setEnabled((Boolean) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        public void addTool(AcpToolConfig tool) {
            tools.add(tool);
            fireTableRowsInserted(tools.size() - 1, tools.size() - 1);
        }

        public void updateTool(int row, AcpToolConfig tool) {
            tools.set(row, tool);
            fireTableRowsUpdated(row, row);
        }

        public void removeTool(int row) {
            tools.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public AcpToolConfig getToolAt(int row) {
            return tools.get(row);
        }

        public List<AcpToolConfig> getAllTools() {
            return new ArrayList<>(tools);
        }

        public void clear() {
            int size = tools.size();
            if (size > 0) {
                tools.clear();
                fireTableRowsDeleted(0, size - 1);
            }
        }
    }

    // ===== ACP Tool Dialog =====

    private static class AcpToolDialog extends com.intellij.openapi.ui.DialogWrapper {
        private final JComboBox<AcpToolConfig.AcpType> typeCombo = new JComboBox<>(AcpToolConfig.AcpType.values());
        private final JBTextField pathField = new JBTextField();
        private final JBCheckBox enabledCheckbox = new JBCheckBox("Enabled", true);
        private final JBLabel testResultLabel = new JBLabel();
        private final JTextArea pathHelpArea = createHelpArea();
        private boolean suppressTypeListener = false;

        AcpToolDialog(AcpToolConfig existing) {
            super(true);
            setTitle(existing == null ? "Add ACP Tool" : "Edit ACP Tool");
            if (existing != null) {
                suppressTypeListener = true;
                typeCombo.setSelectedItem(existing.getType() != null ? existing.getType() : AcpToolConfig.AcpType.CUSTOM);
                suppressTypeListener = false;
                pathField.setText(existing.getExecutablePath());
                enabledCheckbox.setSelected(existing.isEnabled());
                updatePathHelp(existing.getType());
            }
            init();
            if (existing == null) {
                onTypeChanged();
            }
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridy = 0;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JBLabel("Type:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            typeCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, sel, focus);
                    if (value instanceof AcpToolConfig.AcpType t) setText(t.getDisplayName());
                    return this;
                }
            });
            typeCombo.addActionListener(e -> onTypeChanged());
            panel.add(typeCombo, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JBLabel("Executable path:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(pathField, gbc);

            // Dynamic help text below executable path
            gbc.gridy++;
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(pathHelpArea, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            panel.add(enabledCheckbox, gbc);

            // Test button row
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            JPanel testRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            JButton testButton = new JButton("Test Connection");
            testButton.addActionListener(e -> runTest());
            testRow.add(testButton);
            testResultLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            testRow.add(testResultLabel);
            panel.add(testRow, gbc);

            return panel;
        }

        private void onTypeChanged() {
            if (suppressTypeListener) return;
            AcpToolConfig.AcpType type = (AcpToolConfig.AcpType) typeCombo.getSelectedItem();
            updatePathHelp(type);
            if (type == null || type == AcpToolConfig.AcpType.CUSTOM) return;
            pathField.setText(type.getDefaultExecutablePath());
        }

        private void updatePathHelp(AcpToolConfig.AcpType type) {
            if (type == null || type == AcpToolConfig.AcpType.CUSTOM) {
                pathHelpArea.setText("Find the full path by running: whereis <executable>");
            } else if (type == AcpToolConfig.AcpType.CLAUDE) {
                pathHelpArea.setText("Requires the ACP bridge: https://github.com/zed-industries/claude-code-acp\n" +
                        "Find the full path by running: whereis claude-code-acp");
            } else {
                pathHelpArea.setText("Find the full path by running: whereis " + type.getDefaultExecutablePath());
            }
        }

        private void runTest() {
            String path = pathField.getText().trim();
            if (path.isEmpty()) {
                testResultLabel.setForeground(UIManager.getColor("Component.errorFocusColor"));
                testResultLabel.setText("Executable path is empty");
                return;
            }

            testResultLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            testResultLabel.setText("Testing ACP handshake...");

            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    com.devoxx.genie.service.acp.protocol.AcpClient client =
                            new com.devoxx.genie.service.acp.protocol.AcpClient(text -> {});
                    AcpToolConfig.AcpType selType = (AcpToolConfig.AcpType) typeCombo.getSelectedItem();
                    String flag = (selType != null) ? selType.getDefaultAcpFlag() : "acp";
                    client.start(null, path, flag);
                    client.initialize();
                    client.close();
                    showTestResult(true, "ACP handshake successful");
                } catch (Exception ex) {
                    showTestResult(false, ex.getMessage());
                }
            });
        }

        private void showTestResult(boolean success, String message) {
            SwingUtilities.invokeLater(() -> {
                String truncated = message.length() > 80 ? message.substring(0, 80) + "..." : message;
                if (success) {
                    testResultLabel.setForeground(new java.awt.Color(0, 128, 0));
                    testResultLabel.setText(truncated);
                } else {
                    testResultLabel.setForeground(UIManager.getColor("Component.errorFocusColor"));
                    testResultLabel.setText(truncated);
                }
            });
        }

        public AcpToolConfig getResult() {
            AcpToolConfig.AcpType selectedType = (AcpToolConfig.AcpType) typeCombo.getSelectedItem();
            if (selectedType == null) selectedType = AcpToolConfig.AcpType.CUSTOM;
            return AcpToolConfig.builder()
                    .type(selectedType)
                    .name(selectedType.getDisplayName())
                    .executablePath(pathField.getText().trim())
                    .acpFlag(selectedType.getDefaultAcpFlag())
                    .enabled(enabledCheckbox.isSelected())
                    .build();
        }
    }
}
