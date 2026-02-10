package com.devoxx.genie.ui.settings.spec;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * Settings panel for Spec Driven Development features.
 */
public class SpecSettingsComponent extends AbstractSettingsComponent {

    private final JBCheckBox enableSpecBrowserCheckbox =
            new JBCheckBox("Enable Spec Browser", Boolean.TRUE.equals(stateService.getSpecBrowserEnabled()));

    private final JBTextField specDirectoryField = new JBTextField(
            stateService.getSpecDirectory() != null ? stateService.getSpecDirectory() : "backlog");

    private final JBCheckBox autoInjectSpecContextCheckbox =
            new JBCheckBox("Auto-inject active spec into prompts", Boolean.TRUE.equals(stateService.getAutoInjectSpecContext()));

    public SpecSettingsComponent() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy = 0;

        // --- Spec Browser ---
        addSection(contentPanel, gbc, "Spec Browser");

        addFullWidthRow(contentPanel, gbc, enableSpecBrowserCheckbox);
        addHelpText(contentPanel, gbc,
                "Enable the Spec Browser tool window to browse and manage task spec files. " +
                "Compatible with Backlog.md format (markdown files with YAML frontmatter).");

        JPanel dirRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        dirRow.add(new JBLabel("Spec directory:"));
        specDirectoryField.setColumns(20);
        dirRow.add(specDirectoryField);
        addFullWidthRow(contentPanel, gbc, dirRow);
        addHelpText(contentPanel, gbc,
                "Directory relative to the project root containing task spec files. " +
                "Default: \"backlog\" (compatible with Backlog.md).");

        // --- Context Injection ---
        addSection(contentPanel, gbc, "Context Injection");

        addFullWidthRow(contentPanel, gbc, autoInjectSpecContextCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, selecting a task spec and clicking 'Implement with Agent' will " +
                "automatically inject the full spec as structured context into the LLM prompt.");

        // --- Backlog.md MCP Integration ---
        addSection(contentPanel, gbc, "Backlog.md MCP Integration");

        JButton autoConfigButton = new JButton("Auto-configure Backlog.md MCP Server");
        autoConfigButton.addActionListener(e -> autoConfigureBacklogMcp());
        addFullWidthRow(contentPanel, gbc, autoConfigButton);
        addHelpText(contentPanel, gbc,
                "Adds a Backlog.md MCP server entry (stdio transport, command: backlog mcp start). " +
                "Requires Backlog.md to be installed: npm i -g backlog.md");

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
    }

    private void autoConfigureBacklogMcp() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        var mcpSettings = state.getMcpSettings();

        // Check if already configured
        if (mcpSettings.getMCPServer("backlog") != null) {
            JOptionPane.showMessageDialog(panel,
                    "Backlog.md MCP server is already configured.",
                    "Already Configured",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MCPServer server = MCPServer.builder()
                .name("backlog")
                .transportType(MCPServer.TransportType.STDIO)
                .command("backlog")
                .args(List.of("mcp", "start"))
                .enabled(true)
                .build();

        mcpSettings.addMCPServer(server);
        state.setMcpEnabled(true);

        JOptionPane.showMessageDialog(panel,
                "Backlog.md MCP server has been added.\n" +
                "MCP support has been enabled.\n\n" +
                "Make sure Backlog.md is installed: npm i -g backlog.md",
                "MCP Server Configured",
                JOptionPane.INFORMATION_MESSAGE);
    }

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

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return enableSpecBrowserCheckbox.isSelected() != Boolean.TRUE.equals(state.getSpecBrowserEnabled())
                || !Objects.equals(specDirectoryField.getText().trim(), state.getSpecDirectory())
                || autoInjectSpecContextCheckbox.isSelected() != Boolean.TRUE.equals(state.getAutoInjectSpecContext());
    }

    public void apply() {
        stateService.setSpecBrowserEnabled(enableSpecBrowserCheckbox.isSelected());
        stateService.setSpecDirectory(specDirectoryField.getText().trim());
        stateService.setAutoInjectSpecContext(autoInjectSpecContextCheckbox.isSelected());
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        enableSpecBrowserCheckbox.setSelected(Boolean.TRUE.equals(state.getSpecBrowserEnabled()));
        specDirectoryField.setText(state.getSpecDirectory() != null ? state.getSpecDirectory() : "backlog");
        autoInjectSpecContextCheckbox.setSelected(Boolean.TRUE.equals(state.getAutoInjectSpecContext()));
    }

    @Override
    public void addListeners() {
        // No dynamic listeners needed
    }
}
