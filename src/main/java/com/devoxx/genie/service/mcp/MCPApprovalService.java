package com.devoxx.genie.service.mcp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for handling MCP function call approvals
 */
@Slf4j
public class MCPApprovalService {

    private static final int APPROVAL_TIMEOUT_SECONDS = 30;

    /**
     * Request user approval for an MCP tool execution
     *
     * @param project The current project
     * @param toolName The name of the tool being called
     * @param arguments The arguments being passed to the tool
     * @return true if approved, false if denied or timed out
     */
    public static boolean requestApproval(@Nullable Project project, @NotNull String toolName, @NotNull String arguments) {
        // Skip approval if running in headless mode or if approval is not required
        if (ApplicationManager.getApplication().isHeadlessEnvironment() ||
                !DevoxxGenieStateService.getInstance().getMcpApprovalRequired()) {
            return true;
        }

        CompletableFuture<Boolean> approvalFuture = new CompletableFuture<>();

        ApplicationManager.getApplication().invokeLater(() -> {
            MCPApprovalDialog dialog = new MCPApprovalDialog(project, toolName, arguments);
            boolean approved = dialog.showAndGet();
            approvalFuture.complete(approved);
        });

        try {
            // Wait for user response with timeout
            return approvalFuture.get(APPROVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("MCP approval request timed out or was interrupted", e);
            NotificationUtil.sendNotification(project, "MCP tool execution was cancelled due to timeout");
            return false;
        }
    }

    /**
     * Dialog for requesting MCP tool execution approval
     */
    private static class MCPApprovalDialog extends DialogWrapper {
        private final String toolName;
        private final String arguments;

        protected MCPApprovalDialog(@Nullable Project project, @NotNull String toolName, @NotNull String arguments) {
            super(project, false);
            this.toolName = toolName;
            this.arguments = arguments;
            setTitle("Approve MCP Tool Execution");
            setOKButtonText("Approve");
            setCancelButtonText("Deny");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(JBUI.Borders.empty(10));
            panel.setPreferredSize(new Dimension(500, 300));

            // Create warning icon and message
            JPanel headerPanel = new JPanel(new BorderLayout());
            JBLabel iconLabel = new JBLabel(Messages.getWarningIcon());
            headerPanel.add(iconLabel, BorderLayout.WEST);

            JBLabel messageLabel = new JBLabel("<html><b>The AI assistant wants to execute the following MCP tool:</b></html>");
            headerPanel.add(messageLabel, BorderLayout.CENTER);
            panel.add(headerPanel, BorderLayout.NORTH);

            // Create tool info panel
            JPanel infoPanel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = JBUI.insets(5);

            // Tool name
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0.2;
            infoPanel.add(new JBLabel("<html><b>Tool:</b></html>"), c);

            c.gridx = 1;
            c.weightx = 0.8;
            infoPanel.add(new JBLabel(toolName), c);

            // Arguments
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 0.2;
            c.anchor = GridBagConstraints.NORTHWEST;
            infoPanel.add(new JBLabel("<html><b>Arguments:</b></html>"), c);

            c.gridx = 1;
            c.weightx = 0.8;
            JTextArea argumentsArea = new JTextArea(arguments);
            argumentsArea.setEditable(false);
            argumentsArea.setLineWrap(true);
            argumentsArea.setWrapStyleWord(true);
            JBScrollPane scrollPane = new JBScrollPane(argumentsArea);
            scrollPane.setPreferredSize(new Dimension(350, 150));
            infoPanel.add(scrollPane, c);

            panel.add(infoPanel, BorderLayout.CENTER);

            // Add warning message
            JBLabel warningLabel = new JBLabel("<html><i>Warning: Only approve if you trust this tool execution.</i></html>");
            warningLabel.setForeground(JBColor.RED);
            panel.add(warningLabel, BorderLayout.SOUTH);

            return panel;
        }

        @Override
        protected Action @NotNull [] createActions() {
            return new Action[]{getOKAction(), getCancelAction()};
        }

    }
}