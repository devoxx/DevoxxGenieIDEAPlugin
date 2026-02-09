package com.devoxx.genie.ui.panel.agent;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.service.agent.AgentLoggingMessage;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for displaying Agent debug logs with tool calls, arguments, and results.
 * Double-click a log entry to open the full content in a new editor tab.
 */
@Slf4j
public class AgentLogPanel extends SimpleToolWindowPanel implements AgentLoggingMessage, Disposable {

    private static final String WELCOME_MESSAGE = "Agent Log panel is now active. Agent tool call logs will appear here.";
    private static final String AGENT_DISABLED_MESSAGE = "Agent debug logging is currently disabled. Enable it in Settings \u2192 DevoxxGenie \u2192 Agent Mode \u2192 Debug.";
    private static final String AGENT_EMPTY_LOGS_MESSAGE = "No agent logs captured yet. They will appear here when the LLM invokes tools.";

    private static final int DEFAULT_MAX_LOG_ENTRIES = 1000;
    private int maxLogEntries = DEFAULT_MAX_LOG_ENTRIES;
    private boolean isPaused = false;
    private final List<LogEntry> pendingLogs = new ArrayList<>();
    private static final int BATCH_SIZE = 20;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final transient Project project;
    private final DefaultListModel<LogEntry> logListModel;
    private final JBList<LogEntry> logList;
    private final List<LogEntry> fullLogs = new ArrayList<>();

    public AgentLogPanel(@NotNull Project project) {
        super(true);
        this.project = project;

        logListModel = new DefaultListModel<>();

        if (!Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())) {
            logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), AgentType.TOOL_REQUEST, AGENT_DISABLED_MESSAGE, AGENT_DISABLED_MESSAGE));
        } else {
            logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), AgentType.TOOL_REQUEST, WELCOME_MESSAGE, WELCOME_MESSAGE));
            logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), AgentType.TOOL_REQUEST, AGENT_EMPTY_LOGS_MESSAGE, AGENT_EMPTY_LOGS_MESSAGE));
        }

        logList = new JBList<>(logListModel);
        logList.setCellRenderer(new AgentLogEntryRenderer());
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logList.setFixedCellHeight(24);
        logList.setVisibleRowCount(20);

        logList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = logList.locationToIndex(e.getPoint());
                    if (index >= 0 && index < logListModel.size()) {
                        openLogInEditor(logListModel.getElementAt(index));
                    }
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(logList);
        scrollPane.setBorder(JBUI.Borders.empty());
        setContent(scrollPane);

        setupToolbar();

        ApplicationManager.getApplication().invokeLater(() ->
                MessageBusUtil.connect(project, connection -> {
                    MessageBusUtil.subscribe(connection, AppTopics.AGENT_LOG_MSG, this);
                    Disposer.register(this, connection);
                }));
    }

    private void setupToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // Agent debug on/off toggle
        actionGroup.add(new ToggleAction("Agent Debug On/Off", "Enable/disable agent debug logs", null) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled());
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                DevoxxGenieStateService.getInstance().setAgentDebugLogsEnabled(state);

                ApplicationManager.getApplication().invokeLater(() -> {
                    clearLogs();
                    if (!state) {
                        LogEntry entry = new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), AgentType.TOOL_REQUEST, AGENT_DISABLED_MESSAGE, AGENT_DISABLED_MESSAGE);
                        logListModel.addElement(entry);
                        fullLogs.add(entry);
                    } else {
                        LogEntry entry = new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), AgentType.TOOL_REQUEST, AGENT_EMPTY_LOGS_MESSAGE, AGENT_EMPTY_LOGS_MESSAGE);
                        logListModel.addElement(entry);
                        fullLogs.add(entry);
                    }
                });
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                boolean enabled = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled());
                e.getPresentation().setIcon(enabled ?
                        IconLoader.getIcon("/actions/suspend.svg", AgentLogPanel.class) :
                        IconLoader.getIcon("/actions/execute.svg", AgentLogPanel.class));
                e.getPresentation().setText(enabled ? "Stop Agent Logging" : "Start Agent Logging");
            }
        });

        // Clear logs action
        actionGroup.add(new AnAction("Clear Logs", "Clear all log entries",
                IconLoader.getIcon("/actions/gc.svg", AgentLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                clearLogs();
            }
        });

        // Pause/Resume toggle
        actionGroup.add(new ToggleAction("Pause", "Pause log collection",
                IconLoader.getIcon("/actions/pause.svg", AgentLogPanel.class)) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return isPaused;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                isPaused = state;
                e.getPresentation().setText(isPaused ? "Resume" : "Pause");
                e.getPresentation().setDescription(isPaused ? "Resume log collection" : "Pause log collection");
            }
        });

        // Copy all logs to clipboard
        actionGroup.add(new AnAction("Copy All Logs", "Copy all log entries to clipboard",
                IconLoader.getIcon("/actions/copy.svg", AgentLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyLogsToClipboard();
            }
        });

        // Settings action for log retention
        actionGroup.add(new AnAction("Settings", "Configure log retention",
                IconLoader.getIcon("/general/settings.svg", AgentLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showSettingsDialog();
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("AgentLogPanelToolbar", actionGroup, true);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());
    }

    private void showSettingsDialog() {
        String result = Messages.showInputDialog(
                project,
                "Maximum number of log entries to keep in memory:",
                "Agent Log Settings",
                null,
                String.valueOf(maxLogEntries),
                new InputValidator() {
                    @Override
                    public boolean checkInput(String inputString) {
                        try {
                            int value = Integer.parseInt(inputString);
                            return value > 0 && value <= 10000;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }

                    @Override
                    public boolean canClose(String inputString) {
                        return checkInput(inputString);
                    }
                });

        if (result != null) {
            try {
                int newValue = Integer.parseInt(result);
                if (newValue != maxLogEntries) {
                    maxLogEntries = newValue;
                    pruneLogsToMaxSize();
                }
            } catch (NumberFormatException ignored) {
                // Ignore
            }
        }
    }

    private void pruneLogsToMaxSize() {
        ApplicationManager.getApplication().invokeLater(() -> {
            while (fullLogs.size() > maxLogEntries) {
                fullLogs.remove(0);
            }
            while (logListModel.size() > maxLogEntries) {
                logListModel.remove(0);
            }
        });
    }

    private synchronized void processPendingLogs() {
        if (pendingLogs.isEmpty()) {
            return;
        }

        List<LogEntry> logsToProcess = new ArrayList<>(pendingLogs);
        pendingLogs.clear();

        ApplicationManager.getApplication().invokeLater(() -> {
            // Clear placeholder messages if this is the first real log
            if (fullLogs.size() <= 2) {
                boolean hasOnlyPlaceholders = fullLogs.stream().allMatch(entry ->
                        entry.message().equals(AGENT_DISABLED_MESSAGE) ||
                        entry.message().equals(AGENT_EMPTY_LOGS_MESSAGE) ||
                        entry.message().equals(WELCOME_MESSAGE));
                if (hasOnlyPlaceholders) {
                    fullLogs.clear();
                    logListModel.clear();
                }
            }

            int excess = (fullLogs.size() + logsToProcess.size()) - maxLogEntries;
            if (excess > 0) {
                for (int i = 0; i < excess && !fullLogs.isEmpty(); i++) {
                    fullLogs.remove(0);
                    if (!logListModel.isEmpty()) {
                        logListModel.remove(0);
                    }
                }
            }

            fullLogs.addAll(logsToProcess);
            for (LogEntry entry : logsToProcess) {
                logListModel.addElement(entry);
            }

            if (!logListModel.isEmpty()) {
                int lastIndex = logListModel.size() - 1;
                logList.ensureIndexIsVisible(lastIndex);
            }
        });
    }

    @Override
    public void onAgentLoggingMessage(AgentMessage message) {
        if (message == null || isPaused) {
            return;
        }

        String logText = formatAgentMessage(message);
        String fullContent = buildFullContent(message);
        LogEntry entry = new LogEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                message.getType(),
                logText,
                fullContent
        );

        synchronized (pendingLogs) {
            pendingLogs.add(entry);

            if (pendingLogs.size() >= BATCH_SIZE) {
                processPendingLogs();
            } else if (pendingLogs.size() == 1) {
                Timer timer = new Timer(100, e -> processPendingLogs());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    private @NotNull String formatAgentMessage(@NotNull AgentMessage message) {
        StringBuilder sb = new StringBuilder();

        // Intermediate responses don't have a call counter — skip the [n/m] prefix
        if (message.getType() != AgentType.INTERMEDIATE_RESPONSE) {
            sb.append("[").append(message.getCallNumber()).append("/").append(message.getMaxCalls()).append("] ");
        }

        // Prefix with sub-agent ID when present (e.g. "[1/100] [sub-agent-1] ▶ search_files")
        if (message.getSubAgentId() != null) {
            sb.append("[").append(message.getSubAgentId()).append("] ");
        }

        switch (message.getType()) {
            case TOOL_REQUEST:
                sb.append("\u25B6 ").append(message.getToolName());
                if (message.getArguments() != null) {
                    String args = message.getArguments();
                    if (args.length() > 100) {
                        args = args.substring(0, 100) + "...";
                    }
                    sb.append(" \u2190 ").append(args);
                }
                break;
            case TOOL_RESPONSE:
                sb.append("\u2714 ").append(message.getToolName());
                if (message.getResult() != null) {
                    String result = message.getResult();
                    if (result.length() > 120) {
                        result = result.substring(0, 120) + "...";
                    }
                    sb.append(" \u2192 ").append(result.replace("\n", " "));
                }
                break;
            case TOOL_ERROR:
                sb.append("\u2716 ").append(message.getToolName());
                if (message.getResult() != null) {
                    sb.append(" \u2192 ").append(message.getResult());
                }
                break;
            case LOOP_LIMIT:
                sb.append("\u26A0 LOOP LIMIT REACHED (").append(message.getMaxCalls()).append(" calls)");
                break;
            case APPROVAL_REQUESTED:
                sb.append("\u2753 Approval requested for ").append(message.getToolName());
                break;
            case APPROVAL_GRANTED:
                sb.append("\u2714 Approval granted for ").append(message.getToolName());
                break;
            case APPROVAL_DENIED:
                sb.append("\u2716 Approval denied for ").append(message.getToolName());
                break;
            case INTERMEDIATE_RESPONSE:
                sb.append("\uD83D\uDCAC ");
                if (message.getResult() != null) {
                    String text = message.getResult().replace("\n", " ");
                    if (text.length() > 150) {
                        text = text.substring(0, 150) + "...";
                    }
                    sb.append(text);
                } else {
                    sb.append("LLM intermediate response");
                }
                break;
        }
        return sb.toString();
    }

    private @NotNull String buildFullContent(@NotNull AgentMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ").append(message.getType()).append("\n");
        if (message.getToolName() != null) {
            sb.append("Tool: ").append(message.getToolName()).append("\n");
        }
        if (message.getCallNumber() > 0) {
            sb.append("Call: ").append(message.getCallNumber()).append("/").append(message.getMaxCalls()).append("\n");
        }
        if (message.getSubAgentId() != null) {
            sb.append("Sub-agent: ").append(message.getSubAgentId()).append("\n");
        }
        sb.append("\n");
        if (message.getArguments() != null) {
            sb.append("--- Arguments ---\n").append(message.getArguments()).append("\n\n");
        }
        if (message.getResult() != null) {
            sb.append("--- Result ---\n").append(message.getResult()).append("\n");
        }
        return sb.toString();
    }

    private void openLogInEditor(LogEntry logEntry) {
        if (logEntry == null) {
            return;
        }

        try {
            String content = "Timestamp: " + logEntry.timestamp() + "\n" +
                    logEntry.fullContent();

            String fileName = "AgentLog_" + logEntry.timestamp().replace(":", "").replace(".", "_") + ".txt";

            ApplicationManager.getApplication().invokeLater(() -> {
                LightVirtualFile virtualFile = new LightVirtualFile(fileName, content);
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
            });
        } catch (Exception e) {
            log.error("Error opening agent log entry: {}", e.getMessage());
            NotificationUtil.sendNotification(project, "Error opening agent log: " + e.getMessage());
        }
    }

    private void copyLogsToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : fullLogs) {
            sb.append(entry.timestamp()).append("\n").append(entry.fullContent()).append("\n---\n");
        }
        if (sb.isEmpty()) {
            NotificationUtil.sendNotification(project, "No agent logs to copy.");
            return;
        }
        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        NotificationUtil.sendNotification(project, "Agent logs copied to clipboard (" + fullLogs.size() + " entries).");
    }

    private void clearLogs() {
        synchronized (pendingLogs) {
            pendingLogs.clear();
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            fullLogs.clear();
            logListModel.clear();
        });
    }

    @Override
    public void dispose() {
        synchronized (pendingLogs) {
            pendingLogs.clear();
        }
        fullLogs.clear();
        logListModel.clear();
    }

    private record LogEntry(String timestamp, AgentType type, String message, String fullContent) {
        @Override
        public String toString() {
            return timestamp + " " + message;
        }
    }

    private static class AgentLogEntryRenderer extends DefaultListCellRenderer {
        private static final Color REQUEST_COLOR = new JBColor(new Color(33, 150, 243), new Color(100, 180, 255));   // Blue
        private static final Color RESPONSE_COLOR = new JBColor(new Color(76, 175, 80), new Color(120, 200, 120));   // Green
        private static final Color ERROR_COLOR = new JBColor(new Color(244, 67, 54), new Color(255, 120, 120));      // Red
        private static final Color LIMIT_COLOR = new JBColor(new Color(255, 152, 0), new Color(255, 180, 80));       // Orange
        private static final Color APPROVAL_COLOR = new JBColor(new Color(156, 39, 176), new Color(200, 120, 220));  // Purple

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof LogEntry logEntry && !isSelected) {
                label.setText(logEntry.timestamp() + " " + logEntry.message());

                switch (logEntry.type()) {
                    case TOOL_REQUEST -> label.setForeground(REQUEST_COLOR);
                    case TOOL_RESPONSE -> label.setForeground(RESPONSE_COLOR);
                    case TOOL_ERROR -> label.setForeground(ERROR_COLOR);
                    case LOOP_LIMIT -> label.setForeground(LIMIT_COLOR);
                    case APPROVAL_REQUESTED, APPROVAL_GRANTED, APPROVAL_DENIED -> label.setForeground(APPROVAL_COLOR);
                    case INTERMEDIATE_RESPONSE -> label.setForeground(JBColor.foreground());
                }

                label.setToolTipText(logEntry.message());
            }

            return label;
        }
    }
}
