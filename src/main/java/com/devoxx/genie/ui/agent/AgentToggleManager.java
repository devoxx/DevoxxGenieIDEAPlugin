package com.devoxx.genie.ui.agent;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.AgentIcon;

/**
 * Manages the Agent Mode toggle button in the footer toolbar.
 * Displays a spy/agent icon that users can click to enable/disable agent mode.
 */
public class AgentToggleManager {

    private final Project project;

    @Getter
    private final JLabel agentToggleLabel;

    public AgentToggleManager(@NotNull Project project) {
        this.project = project;
        this.agentToggleLabel = createAgentToggleLabel();
        updateAgentToggle();
    }

    private @NotNull JLabel createAgentToggleLabel() {
        JLabel label = new JLabel();
        label.setIcon(AgentIcon);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.CENTER);
        label.setIconTextGap(2);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleAgentMode();
            }
        });

        return label;
    }

    /**
     * Toggle agent mode on/off.
     */
    private void toggleAgentMode() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        boolean currentState = Boolean.TRUE.equals(stateService.getAgentModeEnabled());
        stateService.setAgentModeEnabled(!currentState);
        updateAgentToggle();
    }

    /**
     * Update the toggle label to reflect current agent mode state.
     */
    public void updateAgentToggle() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        boolean enabled = Boolean.TRUE.equals(stateService.getAgentModeEnabled());

        if (enabled) {
            agentToggleLabel.setText("Agent");
            agentToggleLabel.setForeground(new JBColor(new Color(0xFF5400), new Color(0xFF7B33)));
            agentToggleLabel.setToolTipText(buildEnabledTooltip(stateService));
        } else {
            agentToggleLabel.setText("Agent");
            agentToggleLabel.setForeground(JBColor.GRAY);
            agentToggleLabel.setToolTipText(
                    "<html><b>Agent Mode: OFF</b><br><br>" +
                    "Click to enable Agent Mode.<br>" +
                    "Gives the LLM built-in IDE tools:<br>" +
                    "read_file, write_file, list_files,<br>" +
                    "search_files, run_command<br><br>" +
                    "Configure in Settings &rarr; DevoxxGenie &rarr; Agent Mode</html>");
        }
    }

    private @NotNull String buildEnabledTooltip(@NotNull DevoxxGenieStateService stateService) {
        int maxCalls = stateService.getAgentMaxToolCalls() != null ? stateService.getAgentMaxToolCalls() : 25;
        boolean autoApprove = Boolean.TRUE.equals(stateService.getAgentAutoApproveReadOnly());

        return "<html><b>Agent Mode: ON</b><br><br>" +
                "Built-in tools: read_file, write_file, list_files,<br>" +
                "search_files, run_command<br><br>" +
                "Max tool calls: " + maxCalls + "<br>" +
                "Auto-approve read-only: " + (autoApprove ? "Yes" : "No") + "<br><br>" +
                "Click to disable Agent Mode</html>";
    }
}
