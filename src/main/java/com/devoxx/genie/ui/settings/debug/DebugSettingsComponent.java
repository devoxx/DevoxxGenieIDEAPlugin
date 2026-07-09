package com.devoxx.genie.ui.settings.debug;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Central settings UI for all debug logging options. Raw Request/Response logging lives here;
 * the MCP and agent debug log checkboxes are mirrored from their original settings pages
 * (MCP Settings and Agent Mode) — both places edit the same underlying setting, so changing
 * one is reflected in the other.
 */
public class DebugSettingsComponent {

    private final JPanel panel;
    private final JBCheckBox rawRequestResponseLoggingCheckBox;
    private final JBCheckBox mcpLoggingCheckBox;
    private final JBCheckBox agentDebugLogsCheckBox;

    public DebugSettingsComponent() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        rawRequestResponseLoggingCheckBox = new JBCheckBox("Enable Raw Request/Response Logging");
        rawRequestResponseLoggingCheckBox.setSelected(Boolean.TRUE.equals(state.getRawRequestResponseLoggingEnabled()));

        mcpLoggingCheckBox = new JBCheckBox("Enable MCP Logging");
        mcpLoggingCheckBox.setSelected(Boolean.TRUE.equals(state.getMcpDebugLogsEnabled()));

        agentDebugLogsCheckBox = new JBCheckBox("Enable agent debug logs");
        agentDebugLogsCheckBox.setSelected(Boolean.TRUE.equals(state.getAgentDebugLogsEnabled()));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(12));

        addOption(rawRequestResponseLoggingCheckBox,
                "When enabled, the exact request sent to the LLM provider (messages, tool calls, " +
                        "model parameters) and the response received (message, tool calls, token usage) are " +
                        "captured and shown in the <b>Activity Log</b> tool window, filterable to \"Show Raw " +
                        "Only\". Double-click an entry to open the full JSON, or use \"Copy All\" for bug reports. " +
                        "Likely API keys and tokens are masked automatically, but the raw prompt and response " +
                        "text is not — avoid enabling this if your conversations may contain other secrets you " +
                        "don't want written to the tool window / clipboard.");

        panel.add(Box.createVerticalStrut(16));

        addOption(mcpLoggingCheckBox,
                "Logs MCP (Model Context Protocol) traffic to the <b>Activity Log</b> tool window, " +
                        "filterable to \"Show MCP Only\". Only takes effect when MCP support is enabled. " +
                        "Also available in Settings → DevoxxGenie → MCP Settings (both edit the same setting).");

        panel.add(Box.createVerticalStrut(16));

        addOption(agentDebugLogsCheckBox,
                "Logs agent activity — tool calls with their arguments and results, intermediate LLM " +
                        "responses, approvals and sub-agent lifecycle — to the <b>Activity Log</b> tool window, " +
                        "filterable to \"Show Agents Only\". " +
                        "Also available in Settings → DevoxxGenie → Agent Mode (both edit the same setting).");

        panel.add(Box.createVerticalGlue());
    }

    private void addOption(JBCheckBox checkBox, String descriptionHtml) {
        JBLabel description = new JBLabel(
                "<html><body style='width:480px'>" + descriptionHtml + "</body></html>");
        description.setForeground(UIUtil.getContextHelpForeground());

        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(checkBox);
        panel.add(Box.createVerticalStrut(6));
        panel.add(description);
    }

    public JPanel getPanel() {
        return panel;
    }

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return rawRequestResponseLoggingCheckBox.isSelected() != Boolean.TRUE.equals(state.getRawRequestResponseLoggingEnabled())
                || mcpLoggingCheckBox.isSelected() != Boolean.TRUE.equals(state.getMcpDebugLogsEnabled())
                || agentDebugLogsCheckBox.isSelected() != Boolean.TRUE.equals(state.getAgentDebugLogsEnabled());
    }

    public void apply() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        state.setRawRequestResponseLoggingEnabled(rawRequestResponseLoggingCheckBox.isSelected());
        state.setMcpDebugLogsEnabled(mcpLoggingCheckBox.isSelected());
        state.setAgentDebugLogsEnabled(agentDebugLogsCheckBox.isSelected());
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        rawRequestResponseLoggingCheckBox.setSelected(Boolean.TRUE.equals(state.getRawRequestResponseLoggingEnabled()));
        mcpLoggingCheckBox.setSelected(Boolean.TRUE.equals(state.getMcpDebugLogsEnabled()));
        agentDebugLogsCheckBox.setSelected(Boolean.TRUE.equals(state.getAgentDebugLogsEnabled()));
    }

    public boolean isAnyLoggingSelected() {
        return rawRequestResponseLoggingCheckBox.isSelected()
                || mcpLoggingCheckBox.isSelected()
                || agentDebugLogsCheckBox.isSelected();
    }
}
