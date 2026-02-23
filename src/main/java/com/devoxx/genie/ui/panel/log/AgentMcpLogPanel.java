package com.devoxx.genie.ui.panel.log;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.service.activity.ActivityLoggingMessage;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.MessageBusUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
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
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified log panel that merges Agent and MCP log streams with source-based filtering.
 * Double-click a log entry to open full content in a new editor tab.
 */
@Slf4j
public class AgentMcpLogPanel extends SimpleToolWindowPanel implements ActivityLoggingMessage, MCPLoggingMessage, Disposable {

    private static final int DEFAULT_MAX_LOG_ENTRIES = 1000;
    private static final int BATCH_SIZE = 20;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    enum LogSource { MCP, AGENT }
    enum LogFilter { ALL, MCP_ONLY, AGENT_ONLY }

    record LogEntry(
        String timestamp,
        LogSource source,
        AgentType agentType,  // null for MCP entries
        String message,
        String fullContent
    ) {
        @Override
        public @NonNull String toString() {
            return timestamp + " " + message;
        }
    }

    private int maxLogEntries = DEFAULT_MAX_LOG_ENTRIES;
    private boolean isPaused = false;
    private LogFilter currentFilter = LogFilter.ALL;

    private final transient Project project;
    private final DefaultListModel<LogEntry> logListModel = new DefaultListModel<>();
    private final JBList<LogEntry> logList;
    private final List<LogEntry> fullLogs = new ArrayList<>();
    private final List<LogEntry> pendingLogs = new ArrayList<>();

    public AgentMcpLogPanel(@NotNull Project project) {
        super(true);
        this.project = project;

        logList = new JBList<>(logListModel);
        logList.setCellRenderer(new CombinedLogEntryRenderer());
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
                    MessageBusUtil.subscribe(connection, AppTopics.ACTIVITY_LOG_MSG, this);
                    MessageBusUtil.subscribe(connection, AppTopics.MCP_TRAFFIC_MSG, this);
                    Disposer.register(this, connection);
                }));
    }

    private void setupToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // --- Filter dropdown ---
        actionGroup.add(new FilterComboBoxAction());
        actionGroup.addSeparator();

        // --- Utility actions ---
        actionGroup.add(new AnAction("Clear Logs", "Clear all log entries",
                IconLoader.getIcon("/actions/gc.svg", AgentMcpLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                clearLogs();
            }
        });

        actionGroup.add(new ToggleAction("Pause", "Pause log collection",
                IconLoader.getIcon("/actions/pause.svg", AgentMcpLogPanel.class)) {
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

        actionGroup.add(new AnAction("Copy All Logs", "Copy all log entries to clipboard",
                IconLoader.getIcon("/actions/copy.svg", AgentMcpLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyLogsToClipboard();
            }
        });

        actionGroup.add(new AnAction("Settings", "Configure log retention",
                IconLoader.getIcon("/general/settings.svg", AgentMcpLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showSettingsDialog();
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("AgentMcpLogPanelToolbar", actionGroup, true);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());
    }

    private class FilterComboBoxAction extends ComboBoxAction {

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setText(switch (currentFilter) {
                case ALL        -> "Show All";
                case MCP_ONLY   -> "Show MCP Only";
                case AGENT_ONLY -> "Show Agents Only";
            });
        }

        @Override
        protected @NotNull DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
            DefaultActionGroup group = new DefaultActionGroup();
            group.add(filterAction("Show All",          LogFilter.ALL));
            group.add(filterAction("Show MCP Only",     LogFilter.MCP_ONLY));
            group.add(filterAction("Show Agents Only",  LogFilter.AGENT_ONLY));
            return group;
        }

        private AnAction filterAction(String label, LogFilter filter) {
            return new AnAction(label) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    currentFilter = filter;
                    applyFilter();
                }
            };
        }

        private void applyFilter() {
            ApplicationManager.getApplication().invokeLater(() -> {
                logListModel.clear();
                fullLogs.stream()
                        .filter(AgentMcpLogPanel.this::matchesFilter)
                        .forEach(logListModel::addElement);
                scrollToBottom();
            });
        }
    }

    private boolean matchesFilter(LogEntry e) {
        return switch (currentFilter) {
            case ALL -> true;
            case MCP_ONLY -> e.source() == LogSource.MCP;
            case AGENT_ONLY -> e.source() == LogSource.AGENT;
        };
    }

    private void scrollToBottom() {
        if (!logListModel.isEmpty()) {
            logList.ensureIndexIsVisible(logListModel.size() - 1);
        }
    }

    @Override
    public void onActivityMessage(@NotNull ActivityMessage message) {
        if (isPaused) {
            return;
        }
        String hash = message.getProjectLocationHash();
        if (hash != null && !hash.equals(project.getLocationHash())) {
            return;
        }

        if (message.getSource() == ActivitySource.AGENT) {
            String displayText = formatAgentActivityMessage(message);
            String fullContent = buildAgentActivityFullContent(message);
            LogEntry entry = new LogEntry(
                    LocalDateTime.now().format(TIME_FORMATTER),
                    LogSource.AGENT,
                    message.getAgentType(),
                    displayText,
                    fullContent
            );
            addToPending(entry);
        } else {
            String content = message.getContent();
            LogEntry entry = new LogEntry(
                    LocalDateTime.now().format(TIME_FORMATTER),
                    LogSource.MCP,
                    null,
                    content,
                    content
            );
            addToPending(entry);
        }
    }

    /**
     * Receives MCP traffic messages (low-level protocol debug).
     * This is kept on the separate MCP_TRAFFIC_MSG topic.
     */
    @Override
    public void onMCPLoggingMessage(MCPMessage message) {
        if (message == null || isPaused) {
            return;
        }
        String hash = message.getProjectLocationHash();
        if (hash != null && !hash.equals(project.getLocationHash())) {
            return;
        }

        String content = message.getContent();
        LogEntry entry = new LogEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                LogSource.MCP,
                null,
                content,
                content
        );
        addToPending(entry);
    }

    private void addToPending(LogEntry entry) {
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

    private synchronized void processPendingLogs() {
        if (pendingLogs.isEmpty()) {
            return;
        }
        List<LogEntry> logsToProcess = new ArrayList<>(pendingLogs);
        pendingLogs.clear();

        ApplicationManager.getApplication().invokeLater(() -> {
            int excess = (fullLogs.size() + logsToProcess.size()) - maxLogEntries;
            if (excess > 0) {
                trimExcessLogs(excess);
            }
            fullLogs.addAll(logsToProcess);
            for (LogEntry entry : logsToProcess) {
                if (matchesFilter(entry)) {
                    logListModel.addElement(entry);
                }
            }
            scrollToBottom();
        });
    }

    private void trimExcessLogs(int excess) {
        int toRemove = Math.min(excess, fullLogs.size());
        List<LogEntry> removed = new ArrayList<>(fullLogs.subList(0, toRemove));
        fullLogs.subList(0, toRemove).clear();
        long filteredCount = removed.stream().filter(this::matchesFilter).count();
        for (int i = 0; i < filteredCount && !logListModel.isEmpty(); i++) {
            logListModel.remove(0);
        }
    }

    private @NotNull String formatAgentActivityMessage(@NotNull ActivityMessage message) {
        StringBuilder sb = new StringBuilder();
        if (message.getAgentType() != AgentType.INTERMEDIATE_RESPONSE) {
            sb.append("[").append(message.getCallNumber()).append("/").append(message.getMaxCalls()).append("] ");
        }
        if (message.getSubAgentId() != null) {
            sb.append("[").append(message.getSubAgentId()).append("] ");
        }
        switch (message.getAgentType()) {
            case TOOL_REQUEST:    formatToolRequest(sb, message);    break;
            case TOOL_RESPONSE:   formatToolResponse(sb, message);   break;
            case TOOL_ERROR:
                sb.append("✖ ").append(message.getToolName());
                if (message.getResult() != null) sb.append(" → ").append(message.getResult());
                break;
            case LOOP_LIMIT:
                sb.append("⚠ LOOP LIMIT REACHED (").append(message.getMaxCalls()).append(" calls)");
                break;
            case APPROVAL_REQUESTED:
                sb.append("❓ Approval requested for ").append(message.getToolName());
                break;
            case APPROVAL_GRANTED:
                sb.append("✔ Approval granted for ").append(message.getToolName());
                break;
            case APPROVAL_DENIED:
                sb.append("✖ Approval denied for ").append(message.getToolName());
                break;
            case INTERMEDIATE_RESPONSE: formatIntermediateResponse(sb, message); break;
            case SUB_AGENT_STARTED:
                sb.append("⬇ Sub-agent started: ").append(message.getSubAgentId());
                break;
            case SUB_AGENT_COMPLETED:
                sb.append("⬆ Sub-agent completed: ").append(message.getSubAgentId());
                break;
            case SUB_AGENT_ERROR:
                sb.append("✖ Sub-agent error: ").append(message.getSubAgentId());
                if (message.getResult() != null) sb.append(" → ").append(message.getResult());
                break;
        }
        return sb.toString();
    }

    private void formatToolRequest(@NotNull StringBuilder sb, @NotNull ActivityMessage message) {
        sb.append("▶ ").append(message.getToolName());
        if (message.getArguments() != null) {
            String args = message.getArguments();
            if (args.length() > 100) args = args.substring(0, 100) + "...";
            sb.append(" ← ").append(args);
        }
    }

    private void formatToolResponse(@NotNull StringBuilder sb, @NotNull ActivityMessage message) {
        sb.append("✔ ").append(message.getToolName());
        if (message.getResult() != null) {
            String result = message.getResult();
            if (result.length() > 120) result = result.substring(0, 120) + "...";
            sb.append(" → ").append(result.replace("\n", " "));
        }
    }

    private void formatIntermediateResponse(@NotNull StringBuilder sb, @NotNull ActivityMessage message) {
        sb.append("\uD83D\uDCAC ");
        if (message.getResult() != null) {
            String text = message.getResult().replace("\n", " ");
            if (text.length() > 150) text = text.substring(0, 150) + "...";
            sb.append(text);
        } else {
            sb.append("LLM intermediate response");
        }
    }

    private @NotNull String buildAgentActivityFullContent(@NotNull ActivityMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ").append(message.getAgentType()).append("\n");
        if (message.getToolName() != null) sb.append("Tool: ").append(message.getToolName()).append("\n");
        if (message.getCallNumber() > 0) {
            sb.append("Call: ").append(message.getCallNumber()).append("/").append(message.getMaxCalls()).append("\n");
        }
        if (message.getSubAgentId() != null) sb.append("Sub-agent: ").append(message.getSubAgentId()).append("\n");
        sb.append("\n");
        if (message.getArguments() != null) sb.append("--- Arguments ---\n").append(message.getArguments()).append("\n\n");
        if (message.getResult() != null) sb.append("--- Result ---\n").append(message.getResult()).append("\n");
        return sb.toString();
    }

    private void openLogInEditor(LogEntry logEntry) {
        if (logEntry == null) return;
        try {
            if (logEntry.source() == LogSource.AGENT) {
                String content = "Timestamp: " + logEntry.timestamp() + "\n" + logEntry.fullContent();
                String fileName = "AgentLog_" + logEntry.timestamp().replace(":", "").replace(".", "_") + ".txt";
                ApplicationManager.getApplication().invokeLater(() -> {
                    LightVirtualFile virtualFile = new LightVirtualFile(fileName, content);
                    FileEditorManager.getInstance(project).openFile(virtualFile, true);
                });
            } else {
                String content = logEntry.fullContent();
                if (content.startsWith("<") || content.startsWith(">")) {
                    content = content.substring(1).trim();
                }
                String fileName = "MCPLog_" + logEntry.timestamp().replace(":", "").replace(".", "_") + ".json";
                String finalContent = formatJsonContent(content);
                ApplicationManager.getApplication().invokeLater(() -> {
                    LightVirtualFile virtualFile = new LightVirtualFile(fileName, finalContent);
                    FileEditorManager.getInstance(project).openFile(virtualFile, true);
                });
            }
        } catch (Exception e) {
            log.error("Error opening log entry: {}", e.getMessage());
            NotificationUtil.sendNotification(project, "Error opening log: " + e.getMessage());
        }
    }

    private String formatJsonContent(String content) {
        try {
            Object json = JSON_MAPPER.readValue(content, Object.class);
            return JSON_MAPPER.writeValueAsString(json);
        } catch (Exception ignored) {
            // Not valid JSON, use as-is
            return content;
        }
    }

    private void copyLogsToClipboard() {
        List<LogEntry> logsToExport = new ArrayList<>(fullLogs);
        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : logsToExport) {
            sb.append(entry.timestamp()).append(" [").append(entry.source()).append("] ")
              .append(entry.message()).append("\n");
        }
        if (sb.isEmpty()) {
            NotificationUtil.sendNotification(project, "No logs to copy.");
            return;
        }
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        NotificationUtil.sendNotification(project, "Logs copied to clipboard (" + logsToExport.size() + " entries).");
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

    private void showSettingsDialog() {
        String result = Messages.showInputDialog(
                project,
                "Maximum number of log entries to keep in memory:",
                "Activity Log Settings",
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

    @Override
    public void dispose() {
        synchronized (pendingLogs) {
            pendingLogs.clear();
        }
        fullLogs.clear();
        logListModel.clear();
    }

    private class CombinedLogEntryRenderer extends DefaultListCellRenderer {
        private static final Color REQUEST_COLOR  = new JBColor(new Color(33, 150, 243),  new Color(100, 180, 255));
        private static final Color RESPONSE_COLOR = new JBColor(new Color(76, 175, 80),   new Color(120, 200, 120));
        private static final Color ERROR_COLOR    = new JBColor(new Color(244, 67, 54),   new Color(255, 120, 120));
        private static final Color LIMIT_COLOR    = new JBColor(new Color(255, 152, 0),   new Color(255, 180, 80));
        private static final Color APPROVAL_COLOR = new JBColor(new Color(156, 39, 176),  new Color(200, 120, 220));
        private static final Color MCP_IN_COLOR   = new JBColor(new Color(76, 175, 80),   new Color(120, 200, 120));
        private static final Color MCP_OUT_COLOR  = new JBColor(new Color(33, 150, 243),  new Color(100, 180, 255));

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof LogEntry entry && !isSelected) {
                String sourceTag = entry.source() == LogSource.MCP ? "[MCP] " : "[AGT] ";
                String badge = currentFilter == LogFilter.ALL ? sourceTag : "";
                label.setText(entry.timestamp() + " " + badge + entry.message());
                label.setToolTipText(entry.message());
                Color fg = resolveEntryColor(entry);
                if (fg != null) {
                    label.setForeground(fg);
                }
            }
            return label;
        }

        private Color resolveEntryColor(LogEntry entry) {
            if (entry.source() == LogSource.AGENT && entry.agentType() != null) {
                return switch (entry.agentType()) {
                    case TOOL_REQUEST                                        -> REQUEST_COLOR;
                    case TOOL_RESPONSE                                       -> RESPONSE_COLOR;
                    case TOOL_ERROR                                          -> ERROR_COLOR;
                    case LOOP_LIMIT                                          -> LIMIT_COLOR;
                    case APPROVAL_REQUESTED, APPROVAL_GRANTED,
                         APPROVAL_DENIED                                     -> APPROVAL_COLOR;
                    case INTERMEDIATE_RESPONSE                               -> JBColor.foreground();
                    case SUB_AGENT_STARTED -> null;
                    case SUB_AGENT_COMPLETED -> null;
                    case SUB_AGENT_ERROR -> null;
                };
            }
            if (entry.source() == LogSource.MCP) {
                if (entry.message().startsWith("<")) return MCP_IN_COLOR;
                if (entry.message().startsWith(">")) return MCP_OUT_COLOR;
            }
            return null;
        }
    }
}
