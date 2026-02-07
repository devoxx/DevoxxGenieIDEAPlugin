package com.devoxx.genie.ui.settings.agent;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.model.Constant.AGENT_MAX_TOOL_CALLS;

public class AgentSettingsComponent extends AbstractSettingsComponent {

    private final JBCheckBox enableAgentModeCheckbox =
            new JBCheckBox("Enable Agent Mode", stateService.getAgentModeEnabled());
    private final JBIntSpinner maxToolCallsSpinner =
            new JBIntSpinner(stateService.getAgentMaxToolCalls() != null ? stateService.getAgentMaxToolCalls() : AGENT_MAX_TOOL_CALLS, 1, 100);
    private final JBCheckBox autoApproveReadOnlyCheckbox =
            new JBCheckBox("Auto-approve read-only tools (read_file, list_files, search_files)", stateService.getAgentAutoApproveReadOnly());
    private final JBCheckBox writeApprovalRequiredCheckbox =
            new JBCheckBox("Write tools always require approval (write_file, run_command)", Boolean.TRUE.equals(stateService.getAgentWriteApprovalRequired()));
    private final JBCheckBox enableDebugLogsCheckbox =
            new JBCheckBox("Enable Agent Debug Logs", Boolean.TRUE.equals(stateService.getAgentDebugLogsEnabled()));

    public AgentSettingsComponent() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy = 0;

        // --- Agent Mode ---
        addSection(contentPanel, gbc, "Agent Mode");

        addFullWidthRow(contentPanel, gbc, enableAgentModeCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the LLM gets built-in IDE tools (read_file, write_file, " +
                "list_files, search_files, run_command) to interact with your project autonomously.");

        // --- Loop Controls ---
        addSection(contentPanel, gbc, "Loop Controls");

        JPanel spinnerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        spinnerRow.add(new JBLabel("Max tool calls per prompt:"));
        spinnerRow.add(maxToolCallsSpinner);
        addFullWidthRow(contentPanel, gbc, spinnerRow);
        addHelpText(contentPanel, gbc,
                "Maximum number of tool calls the LLM can make per prompt. " +
                "Prevents infinite loops. The LLM will provide its best answer when the limit is reached.");

        // --- Approval ---
        addSection(contentPanel, gbc, "Approval");

        addFullWidthRow(contentPanel, gbc, autoApproveReadOnlyCheckbox);

        addFullWidthRow(contentPanel, gbc, writeApprovalRequiredCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, a confirmation dialog is shown before executing write tools. " +
                "You can also disable this from the approval dialog itself via the \"Don't ask again\" checkbox.");

        // --- Debug ---
        addSection(contentPanel, gbc, "Debug");

        addFullWidthRow(contentPanel, gbc, enableDebugLogsCheckbox);
        addHelpText(contentPanel, gbc,
                "Agent tool calls, arguments, and results are logged in the " +
                "'DevoxxGenie Agent Logs' tool window (View \u2192 Tool Windows).");

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
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
        JBLabel label = new JBLabel("<html><small>" + text + "</small></html>");
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(label, gbc);
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy++;
    }

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return enableAgentModeCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentModeEnabled())
                || maxToolCallsSpinner.getNumber() != (state.getAgentMaxToolCalls() != null ? state.getAgentMaxToolCalls() : AGENT_MAX_TOOL_CALLS)
                || autoApproveReadOnlyCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentAutoApproveReadOnly())
                || writeApprovalRequiredCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentWriteApprovalRequired())
                || enableDebugLogsCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentDebugLogsEnabled());
    }

    public void apply() {
        stateService.setAgentModeEnabled(enableAgentModeCheckbox.isSelected());
        stateService.setAgentMaxToolCalls(maxToolCallsSpinner.getNumber());
        stateService.setAgentAutoApproveReadOnly(autoApproveReadOnlyCheckbox.isSelected());
        stateService.setAgentWriteApprovalRequired(writeApprovalRequiredCheckbox.isSelected());
        stateService.setAgentDebugLogsEnabled(enableDebugLogsCheckbox.isSelected());
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        enableAgentModeCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentModeEnabled()));
        maxToolCallsSpinner.setNumber(state.getAgentMaxToolCalls() != null ? state.getAgentMaxToolCalls() : AGENT_MAX_TOOL_CALLS);
        autoApproveReadOnlyCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentAutoApproveReadOnly()));
        writeApprovalRequiredCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentWriteApprovalRequired()));
        enableDebugLogsCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentDebugLogsEnabled()));
    }

    @Override
    public void addListeners() {
        // No dynamic listeners needed
    }
}
