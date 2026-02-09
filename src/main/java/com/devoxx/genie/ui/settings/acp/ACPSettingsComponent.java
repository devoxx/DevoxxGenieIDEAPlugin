package com.devoxx.genie.ui.settings.acp;

import com.devoxx.genie.model.acp.ACPSettings;
import com.devoxx.genie.service.acp.ACPSessionManager;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Settings component for ACP (Agent Client Protocol) configuration.
 * Allows users to configure an ACP agent (e.g., Gemini CLI).
 */
@Slf4j
public class ACPSettingsComponent extends AbstractSettingsComponent {

    @Getter
    private final JCheckBox enableAcpCheckbox;
    @Getter
    private final JBTextField agentNameField;
    @Getter
    private final TextFieldWithBrowseButton agentCommandField;
    @Getter
    private final JBTextField agentArgsField;

    private boolean isModified = false;

    public ACPSettingsComponent() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        ACPSettings settings = stateService.getAcpSettings();

        enableAcpCheckbox = new JCheckBox("Enable ACP Agent", Boolean.TRUE.equals(stateService.getAcpEnabled()));
        agentNameField = new JBTextField(settings.getAgentName());
        agentCommandField = new TextFieldWithBrowseButton();
        agentCommandField.setText(settings.getAgentCommand());
        agentCommandField.addBrowseFolderListener(
                "Select Agent Command",
                "Select the ACP agent executable (e.g., gemini)",
                null,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
        );
        agentArgsField = new JBTextField(String.join(" ", settings.getAgentArgs()));
    }

    @Override
    public @NotNull JPanel createPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridy = 0;

        // Enable checkbox
        addSection(mainPanel, gbc, "Agent Client Protocol (ACP)");
        addSettingRow(mainPanel, gbc, "Enable ACP", enableAcpCheckbox);

        // Info label
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        JBLabel infoLabel = new JBLabel(
                "<html>ACP allows DevoxxGenie to communicate with AI coding agents like Gemini CLI.<br>" +
                "When enabled, prompts are sent to the ACP agent instead of the selected LLM provider.</html>");
        infoLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        mainPanel.add(infoLabel, gbc);
        gbc.gridy++;

        // Agent configuration section
        addSection(mainPanel, gbc, "Agent Configuration");
        addSettingRow(mainPanel, gbc, "Agent Name", agentNameField);

        // Command field with Detect button
        JPanel commandPanel = new JPanel(new BorderLayout(5, 0));
        commandPanel.add(agentCommandField, BorderLayout.CENTER);
        JButton detectButton = new JButton("Detect");
        detectButton.setToolTipText("Auto-detect gemini CLI on PATH");
        detectButton.addActionListener(e -> detectGeminiCli());
        commandPanel.add(detectButton, BorderLayout.EAST);
        addSettingRow(mainPanel, gbc, "Agent Command", commandPanel);

        addSettingRow(mainPanel, gbc, "Agent Arguments", agentArgsField);

        // Test Connection button
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection());
        buttonPanel.add(testButton);
        mainPanel.add(buttonPanel, gbc);
        gbc.gridy++;

        // Spacer to push everything to top
        gbc.weighty = 1.0;
        mainPanel.add(new JPanel(), gbc);

        // Track modifications
        enableAcpCheckbox.addActionListener(e -> isModified = true);
        agentNameField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> isModified = true));
        agentCommandField.getTextField().getDocument().addDocumentListener(new SimpleDocumentListener(() -> isModified = true));
        agentArgsField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> isModified = true));

        panel.add(mainPanel, BorderLayout.CENTER);
        return panel;
    }

    public boolean isModified() {
        return isModified;
    }

    public void apply() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        stateService.setAcpEnabled(enableAcpCheckbox.isSelected());

        ACPSettings settings = stateService.getAcpSettings();
        settings.setAgentName(agentNameField.getText().trim());
        settings.setAgentCommand(agentCommandField.getText().trim());
        settings.setAgentArgs(parseArgs(agentArgsField.getText()));
        settings.setEnabled(enableAcpCheckbox.isSelected());

        // Shutdown existing session so it re-initializes with new settings
        ACPSessionManager.getInstance().shutdown();

        isModified = false;
    }

    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        ACPSettings settings = stateService.getAcpSettings();

        enableAcpCheckbox.setSelected(Boolean.TRUE.equals(stateService.getAcpEnabled()));
        agentNameField.setText(settings.getAgentName());
        agentCommandField.setText(settings.getAgentCommand());
        agentArgsField.setText(String.join(" ", settings.getAgentArgs()));

        isModified = false;
    }

    private void detectGeminiCli() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String path = findExecutableOnPath("gemini");
            ApplicationManager.getApplication().invokeLater(() -> {
                if (path != null) {
                    agentCommandField.setText(path);
                    agentNameField.setText("Gemini CLI");
                    agentArgsField.setText("--experimental-acp");
                    isModified = true;
                } else {
                    JOptionPane.showMessageDialog(panel,
                            "Could not find 'gemini' on PATH.\n" +
                            "Install it with: npm install -g @google/gemini-cli",
                            "Gemini CLI Not Found",
                            JOptionPane.WARNING_MESSAGE);
                }
            });
        });
    }

    private void testConnection() {
        String command = agentCommandField.getText().trim();
        if (command.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Please configure the agent command first.",
                    "No Agent Configured", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ACPSessionManager manager = ACPSessionManager.getInstance();
                // Temporarily apply settings for testing
                ACPSettings testSettings = new ACPSettings();
                testSettings.setAgentCommand(command);
                testSettings.setAgentArgs(parseArgs(agentArgsField.getText()));
                testSettings.setAgentName(agentNameField.getText().trim());

                DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
                ACPSettings original = stateService.getAcpSettings();
                stateService.setAcpSettings(testSettings);
                stateService.setAcpEnabled(true);

                try {
                    manager.shutdown();
                    manager.ensureInitialized();
                    JsonObject info = manager.getAgentInfo();
                    String agentName = info != null && info.has("name")
                            ? info.get("name").getAsString() : "Unknown";
                    String agentVersion = info != null && info.has("version")
                            ? info.get("version").getAsString() : "Unknown";

                    ApplicationManager.getApplication().invokeLater(() ->
                            JOptionPane.showMessageDialog(panel,
                                    "Connection successful!\n\nAgent: " + agentName + "\nVersion: " + agentVersion,
                                    "ACP Connection Test",
                                    JOptionPane.INFORMATION_MESSAGE));
                } finally {
                    manager.shutdown();
                    stateService.setAcpSettings(original);
                }
            } catch (Exception ex) {
                log.error("ACP connection test failed", ex);
                ApplicationManager.getApplication().invokeLater(() ->
                        JOptionPane.showMessageDialog(panel,
                                "Connection failed:\n" + ex.getMessage(),
                                "ACP Connection Test",
                                JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private static String findExecutableOnPath(String executable) {
        try {
            String cmd = com.intellij.openapi.util.SystemInfo.isWindows
                    ? "where " + executable
                    : "which " + executable;
            Process proc = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", cmd});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = proc.waitFor();
                if (exitCode == 0 && line != null && !line.isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to detect {} on PATH", executable, e);
        }
        return null;
    }

    private static List<String> parseArgs(String argsText) {
        if (argsText == null || argsText.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.asList(argsText.trim().split("\\s+"));
    }

    /**
     * Simple DocumentListener that calls a callback on any change.
     */
    private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable callback;

        SimpleDocumentListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
    }
}
