package com.devoxx.genie.service.agent;

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

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for handling agent write tool approvals, independent of MCP approval settings.
 */
@Slf4j
public class AgentApprovalService {

    private static final int APPROVAL_TIMEOUT_SECONDS = 120;

    private AgentApprovalService() {}

    /**
     * Request user approval for an agent tool execution.
     *
     * @param project   The current project
     * @param toolName  The name of the tool being called
     * @param arguments The arguments being passed to the tool
     * @return true if approved, false if denied or timed out
     */
    public static boolean requestApproval(@Nullable Project project,
                                          @NotNull String toolName,
                                          @NotNull String arguments) {
        // Auto-approve in headless mode (tests, CI/CD)
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
            return true;
        }

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // Auto-approve if user has opted out of write approval
        if (!Boolean.TRUE.equals(stateService.getAgentWriteApprovalRequired())) {
            return true;
        }

        CompletableFuture<Boolean> approvalFuture = new CompletableFuture<>();

        publishApprovalEvent(project, AgentType.APPROVAL_REQUESTED, toolName, arguments);

        ApplicationManager.getApplication().invokeLater(() -> {
            AgentApprovalDialog dialog = new AgentApprovalDialog(project, toolName, arguments);
            boolean approved = dialog.showAndGet();

            // If approved with "don't ask again" checked, disable future approvals
            if (approved && dialog.isDontAskAgainSelected()) {
                stateService.setAgentWriteApprovalRequired(false);
                log.info("Agent write approval disabled by user via dialog checkbox");
            }

            approvalFuture.complete(approved);
        });

        try {
            boolean approved = approvalFuture.get(APPROVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            publishApprovalEvent(project,
                    approved ? AgentType.APPROVAL_GRANTED : AgentType.APPROVAL_DENIED,
                    toolName, arguments);
            return approved;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Agent approval request timed out or was interrupted", e);
            publishApprovalEvent(project, AgentType.APPROVAL_DENIED, toolName, arguments);
            NotificationUtil.sendNotification(project, "Agent tool execution was cancelled due to timeout");
            return false;
        }
    }

    /**
     * Publishes the approval lifecycle on the shared activity topic so the chat timeline
     * (and the Logs tool window, which already renders APPROVAL_* types) can show why the
     * agent loop is paused. Gated behind the same debug-logs setting as the loop tracker's
     * tool events — the chat row this event resolves only exists when that setting is on.
     */
    private static void publishApprovalEvent(@Nullable Project project,
                                             @NotNull AgentType type,
                                             @NotNull String toolName,
                                             @NotNull String arguments) {
        if (project == null || project.isDisposed()) {
            return;
        }
        if (!Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())) {
            return;
        }
        try {
            ActivityMessage message = ActivityMessage.builder()
                    .source(ActivitySource.AGENT)
                    .agentType(type)
                    .toolName(toolName)
                    .arguments(arguments)
                    .projectLocationHash(project.getLocationHash())
                    .build();

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                    .onActivityMessage(message);
        } catch (Exception e) {
            log.debug("Failed to publish agent approval event", e);
        }
    }

    /**
     * Dialog for requesting agent tool execution approval.
     */
    private static class AgentApprovalDialog extends DialogWrapper {
        private final String toolName;
        private final String arguments;
        private final JBCheckBox dontAskAgainCheckbox;

        protected AgentApprovalDialog(@Nullable Project project,
                                      @NotNull String toolName,
                                      @NotNull String arguments) {
            super(project, false);
            this.toolName = toolName;
            this.arguments = arguments;
            this.dontAskAgainCheckbox = new JBCheckBox("Don't ask again — auto-approve write actions");
            setTitle("Approve Agent Tool Execution");
            setOKButtonText("Approve");
            setCancelButtonText("Deny");
            init();
        }

        public boolean isDontAskAgainSelected() {
            return dontAskAgainCheckbox.isSelected();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(JBUI.Borders.empty(10));
            panel.setPreferredSize(new Dimension(500, 350));

            // Header with warning icon
            JPanel headerPanel = new JPanel(new BorderLayout());
            JBLabel iconLabel = new JBLabel(Messages.getWarningIcon());
            headerPanel.add(iconLabel, BorderLayout.WEST);

            JBLabel messageLabel = new JBLabel(
                    "<html><b>The AI agent wants to execute the following tool:</b></html>");
            messageLabel.setBorder(JBUI.Borders.emptyLeft(8));
            headerPanel.add(messageLabel, BorderLayout.CENTER);
            panel.add(headerPanel, BorderLayout.NORTH);

            // Tool info panel
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

            // Bottom panel with checkbox and warning
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(JBUI.Borders.emptyTop(8));

            bottomPanel.add(dontAskAgainCheckbox, BorderLayout.NORTH);

            JBLabel warningLabel = new JBLabel(
                    "<html><i>Warning: Only approve if you trust this tool execution. " +
                    "You can re-enable approval in Settings → Agent.</i></html>");
            warningLabel.setForeground(JBColor.RED);
            warningLabel.setBorder(JBUI.Borders.emptyTop(4));
            bottomPanel.add(warningLabel, BorderLayout.SOUTH);

            panel.add(bottomPanel, BorderLayout.SOUTH);

            return panel;
        }

        @Override
        protected Action @NotNull [] createActions() {
            return new Action[]{getOKAction(), getCancelAction()};
        }
    }
}
