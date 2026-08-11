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
import com.devoxx.genie.service.agent.tool.AgentDiffPreviewFactory;
import com.devoxx.genie.service.agent.tool.AgentDiffPreviewFactory.DiffPreview;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ReadAccess;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestPanel;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.PathUtil;
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
        return requestApproval(project, toolName, arguments, null);
    }

    /**
     * Request user approval for an agent tool execution, optionally forced by a
     * command-blacklist match (issue #1209). When {@code blacklistedPattern} is non-null
     * the dialog is shown even if the user disabled write approvals.
     *
     * @param blacklistedPattern the blacklist pattern the command matched, or null
     */
    public static boolean requestApproval(@Nullable Project project,
                                          @NotNull String toolName,
                                          @NotNull String arguments,
                                          @Nullable String blacklistedPattern) {
        // Auto-approve in headless mode (tests, CI/CD)
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
            return true;
        }

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        if (!requiresDialog(stateService, blacklistedPattern)) {
            return true;
        }

        CompletableFuture<Boolean> approvalFuture = new CompletableFuture<>();

        publishApprovalEvent(project, AgentType.APPROVAL_REQUESTED, toolName, arguments);

        // Resolve the diff preview here, on the calling (agent) thread, so the EDT never does
        // file IO. Empty for non-file tools or unresolvable edits — the dialog then falls back
        // to the raw arguments view.
        DiffPreview preview = buildPreview(project, toolName, arguments);

        ApplicationManager.getApplication().invokeLater(() -> {
            AgentApprovalDialog dialog =
                    new AgentApprovalDialog(project, toolName, arguments, blacklistedPattern, preview);
            boolean approved = dialog.showAndGet();

            // If approved with "don't ask again" checked, disable future approvals.
            // Blacklisted commands keep forcing this dialog regardless of that setting.
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
     * Decides whether the approval dialog must be shown for a write-tool execution.
     * A command-blacklist match always forces the dialog (issue #1209) — the
     * "auto-approve writes" opt-out must not bypass the blacklist gate.
     */
    static boolean requiresDialog(@NotNull DevoxxGenieStateService stateService,
                                  @Nullable String blacklistedPattern) {
        if (blacklistedPattern != null) {
            return true;
        }
        return Boolean.TRUE.equals(stateService.getAgentWriteApprovalRequired());
    }

    /**
     * Builds the before/after preview for a file-mutating tool call (issue #705). Returns null
     * when there is nothing to show — a preview is a convenience, so any failure degrades to the
     * raw arguments view rather than blocking the approval.
     */
    private static @Nullable DiffPreview buildPreview(@Nullable Project project,
                                                      @NotNull String toolName,
                                                      @NotNull String arguments) {
        if (project == null || project.isDisposed()) {
            return null;
        }
        try {
            AgentDiffPreviewFactory factory = new AgentDiffPreviewFactory(project);
            return ReadAccess.<DiffPreview>compute(
                    () -> factory.create(toolName, arguments).orElse(null));
        } catch (Exception e) {
            log.debug("Could not build approval diff preview for {}", toolName, e);
            return null;
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
        private final Project project;
        private final String toolName;
        private final String arguments;
        private final String blacklistedPattern;
        private final DiffPreview preview;
        private final JBCheckBox dontAskAgainCheckbox;

        protected AgentApprovalDialog(@Nullable Project project,
                                      @NotNull String toolName,
                                      @NotNull String arguments,
                                      @Nullable String blacklistedPattern,
                                      @Nullable DiffPreview preview) {
            super(project, false);
            this.project = project;
            this.toolName = toolName;
            this.arguments = arguments;
            this.blacklistedPattern = blacklistedPattern;
            this.preview = preview;
            this.dontAskAgainCheckbox = new JBCheckBox("Don't ask again — auto-approve write actions");
            setTitle("Approve Agent Tool Execution");
            setOKButtonText("Approve");
            setCancelButtonText("Deny");
            init();
        }

        public boolean isDontAskAgainSelected() {
            // The checkbox is not shown for blacklisted commands: disabling write approval
            // would not stop the blacklist from forcing this dialog, so offering it here
            // would be misleading.
            return blacklistedPattern == null && dontAskAgainCheckbox.isSelected();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(JBUI.Borders.empty(10));
            // A diff needs considerably more room than a two-field argument dump.
            panel.setPreferredSize(preview != null ? new Dimension(900, 600) : new Dimension(500, 350));

            // Header with warning icon
            JPanel headerPanel = new JPanel(new BorderLayout());
            JBLabel iconLabel = new JBLabel(Messages.getWarningIcon());
            headerPanel.add(iconLabel, BorderLayout.WEST);

            JBLabel messageLabel = new JBLabel(preview != null
                    ? "<html><b>The AI agent wants to change " + preview.path() + "</b></html>"
                    : "<html><b>The AI agent wants to execute the following tool:</b></html>");
            messageLabel.setBorder(JBUI.Borders.emptyLeft(8));
            headerPanel.add(messageLabel, BorderLayout.CENTER);

            if (blacklistedPattern != null) {
                JBLabel blacklistLabel = new JBLabel(
                        "<html><b>This command matches the blacklist pattern \"" + blacklistedPattern +
                        "\"</b> (Settings → Agent → Built-in Tools → run_command) and therefore always requires approval.</html>");
                blacklistLabel.setForeground(JBColor.RED);
                blacklistLabel.setBorder(JBUI.Borders.emptyTop(6));
                headerPanel.add(blacklistLabel, BorderLayout.SOUTH);
            }
            panel.add(headerPanel, BorderLayout.NORTH);

            panel.add(preview != null ? createDiffPanel(preview) : createArgumentsPanel(),
                    BorderLayout.CENTER);

            // Bottom panel with checkbox and warning
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(JBUI.Borders.emptyTop(8));

            if (blacklistedPattern == null) {
                bottomPanel.add(dontAskAgainCheckbox, BorderLayout.NORTH);
            }

            JBLabel warningLabel = new JBLabel(
                    "<html><i>Warning: Only approve if you trust this tool execution. " +
                    "You can re-enable approval in Settings → Agent.</i></html>");
            warningLabel.setForeground(JBColor.RED);
            warningLabel.setBorder(JBUI.Borders.emptyTop(4));
            bottomPanel.add(warningLabel, BorderLayout.SOUTH);

            panel.add(bottomPanel, BorderLayout.SOUTH);

            return panel;
        }

        /**
         * Side-by-side diff of the file as it is now against what the tool would write.
         * The panel is tied to the dialog's disposable so its editors are released on close.
         */
        private @NotNull JComponent createDiffPanel(@NotNull DiffPreview diffPreview) {
            FileType fileType = FileTypeManager.getInstance()
                    .getFileTypeByFileName(PathUtil.getFileName(diffPreview.path()));

            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            SimpleDiffRequest request = new SimpleDiffRequest(
                    diffPreview.path(),
                    contentFactory.create(project, diffPreview.before(), fileType),
                    contentFactory.create(project, diffPreview.after(), fileType),
                    "Current",
                    "Proposed by agent");

            DiffRequestPanel diffPanel = DiffManager.getInstance()
                    .createRequestPanel(project, getDisposable(), null);
            diffPanel.setRequest(request);
            return diffPanel.getComponent();
        }

        /** Fallback view for tools without a file preview (run_command, MCP tools, ...). */
        private @NotNull JComponent createArgumentsPanel() {
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

            return infoPanel;
        }

        @Override
        protected Action @NotNull [] createActions() {
            return new Action[]{getOKAction(), getCancelAction()};
        }
    }
}
