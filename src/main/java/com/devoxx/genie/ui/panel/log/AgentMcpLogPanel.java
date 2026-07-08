package com.devoxx.genie.ui.panel.log;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.debug.RawTrafficType;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.rag.RAGLogMessage;
import com.devoxx.genie.service.activity.ActivityLoggingMessage;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.service.rag.RAGLoggingMessage;
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
import java.util.function.Function;

/**
 * Unified log panel that merges Agent and MCP log streams with source-based filtering.
 * Double-click a log entry to open full content in a new editor tab.
 */
@Slf4j
public class AgentMcpLogPanel extends SimpleToolWindowPanel implements ActivityLoggingMessage, MCPLoggingMessage, RAGLoggingMessage, Disposable {

    private static final int DEFAULT_MAX_LOG_ENTRIES = 1000;
    private static final int BATCH_SIZE = 20;
    /**
     * Maximum characters per individual line shown in the panel preview. Very long single lines
     * are truncated with an ellipsis; the full content remains available via tooltip and the
     * double-click editor view.
     */
    private static final int INLINE_PREVIEW_MAX_LINE_LEN = 500;
    /**
     * Maximum number of lines rendered in a single panel row. Multi-line tool output beyond
     * this cap is collapsed into a "(N more lines)" hint so a giant {@code ps -ef} doesn't eat
     * the whole panel.
     */
    private static final int INLINE_PREVIEW_MAX_LINES = 10;

    /** Maximum characters shown in the hover tooltip. Beyond this, content is truncated with an ellipsis. */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    enum LogSource { MCP, AGENT, RAG, RAW }
    enum LogFilter { ALL, MCP_ONLY, AGENT_ONLY, RAG_ONLY, RAW_ONLY }

    record LogEntry(
        String timestamp,
        LogSource source,
        AgentType agentType,  // null unless source == AGENT
        RawTrafficType rawTrafficType,  // null unless source == RAW
        String message,
        String clipboardMessage,
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

        // Track viewport width so cells never paint beyond the tool window's clip rect.
        // Without this, a single long RAG hit preview (or run_command line) forces the JList
        // wider than its viewport, and every JCEF-driven repaint during LLM streaming
        // re-paints that oversized area — visible as IDE-wide flicker.
        logList = new JBList<>(logListModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        logList.setCellRenderer(new CombinedLogEntryRenderer());
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // No fixed cell height — multi-line entries grow to fit their content.
        logList.setVisibleRowCount(20);
        // No hover expansion popup: with large entries the popup repaints on every
        // mouse move and flickers. Full content stays reachable via double-click.
        logList.setExpandableItemsEnabled(false);

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
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setContent(scrollPane);

        setupToolbar();

        ApplicationManager.getApplication().invokeLater(() ->
                MessageBusUtil.connect(project, connection -> {
                    MessageBusUtil.subscribe(connection, AppTopics.ACTIVITY_LOG_MSG, this);
                    MessageBusUtil.subscribe(connection, AppTopics.MCP_TRAFFIC_MSG, this);
                    MessageBusUtil.subscribe(connection, AppTopics.RAG_LOG_MSG, this);
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
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                clearLogs();
            }
        });

        actionGroup.add(new ToggleAction("Pause", "Pause log collection",
                IconLoader.getIcon("/actions/pause.svg", AgentMcpLogPanel.class)) {
            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return isPaused;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                isPaused = state;
                e.getPresentation().setText(isPaused ? "Resume" : "Pause");
                e.getPresentation().setDescription(isPaused ? "Resume log collection" : "Pause log collection");
                e.getPresentation().setIcon(IconLoader.getIcon(
                        isPaused ? "/actions/resume.svg" : "/actions/pause.svg",
                        AgentMcpLogPanel.class));
            }
        });

        actionGroup.add(new AnAction("Copy All Logs", "Copy all log entries to clipboard",
                IconLoader.getIcon("/actions/copy.svg", AgentMcpLogPanel.class)) {
            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyLogsToClipboard();
            }
        });

        actionGroup.add(new AnAction("Settings", "Configure log retention",
                IconLoader.getIcon("/general/settings.svg", AgentMcpLogPanel.class)) {
            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

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
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setText(switch (currentFilter) {
                case ALL        -> "Show All";
                case MCP_ONLY   -> "Show MCP Only";
                case AGENT_ONLY -> "Show Agents Only";
                case RAG_ONLY   -> "Show RAG Only";
                case RAW_ONLY   -> "Show Raw Only";
            });
        }

        @Override
        protected @NotNull DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
            DefaultActionGroup group = new DefaultActionGroup();
            group.add(filterAction("Show All",          LogFilter.ALL));
            group.add(filterAction("Show MCP Only",     LogFilter.MCP_ONLY));
            group.add(filterAction("Show Agents Only",  LogFilter.AGENT_ONLY));
            group.add(filterAction("Show RAG Only",     LogFilter.RAG_ONLY));
            group.add(filterAction("Show Raw Only",     LogFilter.RAW_ONLY));
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
            case RAG_ONLY -> e.source() == LogSource.RAG;
            case RAW_ONLY -> e.source() == LogSource.RAW;
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
            String displayText = formatAgentActivityMessage(message, AgentMcpLogPanel::formatForRow);
            String clipboardText = formatAgentActivityMessage(message, AgentMcpLogPanel::formatForClipboard);
            String fullContent = buildAgentActivityFullContent(message);
            LogEntry entry = new LogEntry(
                    LocalDateTime.now().format(TIME_FORMATTER),
                    LogSource.AGENT,
                    message.getAgentType(),
                    null,
                    displayText,
                    clipboardText,
                    fullContent
            );
            addToPending(entry);
        } else if (message.getSource() == ActivitySource.RAW) {
            String summary = message.getSummary() != null ? message.getSummary() : "";
            String content = message.getContent() != null ? message.getContent() : "";
            LogEntry entry = new LogEntry(
                    LocalDateTime.now().format(TIME_FORMATTER),
                    LogSource.RAW,
                    null,
                    message.getRawTrafficType(),
                    summary,
                    summary,
                    content
            );
            addToPending(entry);
        } else {
            String content = message.getContent();
            LogEntry entry = new LogEntry(
                    LocalDateTime.now().format(TIME_FORMATTER),
                    LogSource.MCP,
                    null,
                    null,
                    content,
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
                null,
                content,
                content,
                content
        );
        addToPending(entry);
    }

    /**
     * Receives RAG retrieval events. Each event describes one semantic search: the query,
     * configured thresholds, and the list of matched chunks with their scores and a preview.
     */
    @Override
    public void onRAGLoggingMessage(RAGLogMessage message) {
        if (message == null || isPaused) {
            return;
        }
        String hash = message.getProjectLocationHash();
        if (hash != null && !hash.equals(project.getLocationHash())) {
            return;
        }
        LogEntry entry = new LogEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                LogSource.RAG,
                null,
                null,
                formatRagRow(message),
                formatRagForClipboard(message),
                formatRagFullContent(message)
        );
        addToPending(entry);
    }

    private static @NotNull String formatRagRow(@NotNull RAGLogMessage m) {
        // Single-line summary. The multi-line bullet list of hits used to be inlined here,
        // but the resulting tall variable-height JList cells caused continuous repaint of the
        // tool window (visible as IDE-wide flicker) while MCP/Agent rows — always single-line —
        // were fine. Full per-hit detail is still available via the hover tooltip and the
        // double-click "open in editor" view.
        StringBuilder sb = new StringBuilder();
        int hitCount = m.getHits() == null ? 0 : m.getHits().size();
        sb.append("RAG: \"").append(summarize(m.getQuery(), 80))
          .append("\" -> ").append(hitCount).append(" hit").append(hitCount == 1 ? "" : "s")
          .append(" (").append(m.getDurationMs()).append("ms");
        if (m.getMinScore() != null && m.getMaxResults() != null) {
            sb.append(", minScore=").append(String.format("%.2f", m.getMinScore()))
              .append(", topK=").append(m.getMaxResults());
        }
        sb.append(")");
        if (hitCount > 0) {
            sb.append("  [");
            int shown = Math.min(hitCount, 4);
            for (int i = 0; i < shown; i++) {
                if (i > 0) sb.append(", ");
                RAGLogMessage.Hit h = m.getHits().get(i);
                sb.append(filenameOf(h.getFilePath())).append(' ').append(formatScore(h.getScore()));
            }
            if (hitCount > shown) {
                sb.append(", +").append(hitCount - shown).append(" more");
            }
            sb.append(']');
        }
        return sb.toString();
    }

    private static @NotNull String formatRagForClipboard(@NotNull RAGLogMessage m) {
        StringBuilder sb = new StringBuilder();
        sb.append("RAG retrieval — query: ").append(m.getQuery() == null ? "" : m.getQuery().replace('\n', ' '))
          .append(" (").append(m.getDurationMs()).append("ms");
        if (m.getMinScore() != null) sb.append(", minScore=").append(m.getMinScore());
        if (m.getMaxResults() != null) sb.append(", topK=").append(m.getMaxResults());
        sb.append(")");
        if (m.getHits() != null) {
            for (RAGLogMessage.Hit h : m.getHits()) {
                sb.append('\n').append("    [").append(formatScore(h.getScore())).append("] ")
                  .append(h.getFilePath());
            }
        }
        return sb.toString();
    }

    private static @NotNull String formatRagFullContent(@NotNull RAGLogMessage m) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RAG Retrieval ===\n");
        sb.append("Query: ").append(m.getQuery()).append("\n");
        if (m.getEmbeddingModel() != null) sb.append("Embedding model: ").append(m.getEmbeddingModel()).append("\n");
        if (m.getMinScore() != null) sb.append("Min score: ").append(m.getMinScore()).append("\n");
        if (m.getMaxResults() != null) sb.append("Top-K (max results): ").append(m.getMaxResults()).append("\n");
        sb.append("Duration: ").append(m.getDurationMs()).append(" ms\n");
        int hitCount = m.getHits() == null ? 0 : m.getHits().size();
        sb.append("Hits: ").append(hitCount).append("\n\n");
        if (hitCount > 0) {
            for (int i = 0; i < m.getHits().size(); i++) {
                RAGLogMessage.Hit h = m.getHits().get(i);
                sb.append("--- Hit ").append(i + 1).append(" / ").append(hitCount).append(" ---\n");
                sb.append("File:   ").append(h.getFilePath()).append("\n");
                sb.append("Score:  ").append(formatScore(h.getScore())).append("\n");
                sb.append("Chunk length: ").append(h.getChunkLength()).append(" chars\n");
                sb.append("Preview:\n").append(h.getPreview() == null ? "" : h.getPreview()).append("\n\n");
            }
        }
        return sb.toString();
    }

    private static @NotNull String filenameOf(String path) {
        if (path == null) return "(unknown)";
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static @NotNull String summarize(String text, int max) {
        if (text == null) return "";
        String oneLine = text.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() > max ? oneLine.substring(0, max - 1) + "…" : oneLine;
    }

    private static @NotNull String formatScore(Double score) {
        return score == null ? "n/a" : String.format("%.3f", score);
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

    static @NotNull String formatAgentActivityMessage(@NotNull ActivityMessage message,
                                                      @NotNull Function<String, String> contentFormatter) {
        StringBuilder sb = new StringBuilder();
        if (message.getAgentType() != AgentType.INTERMEDIATE_RESPONSE
                && message.getAgentType() != AgentType.SYSTEM_PROMPT) {
            sb.append("[").append(message.getCallNumber()).append("/").append(message.getMaxCalls()).append("] ");
        }
        if (message.getSubAgentId() != null) {
            sb.append("[").append(message.getSubAgentId()).append("] ");
        }
        switch (message.getAgentType()) {
            case TOOL_REQUEST:    formatToolRequest(sb, message, contentFormatter);    break;
            case TOOL_RESPONSE:   formatToolResponse(sb, message, contentFormatter);   break;
            case TOOL_ERROR:
                sb.append("✖ ").append(message.getToolName());
                if (message.getResult() != null) sb.append(" → ").append(contentFormatter.apply(message.getResult()));
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
            case INTERMEDIATE_RESPONSE: formatIntermediateResponse(sb, message, contentFormatter); break;
            case SUB_AGENT_STARTED:
                sb.append("⬇ Sub-agent started: ").append(message.getSubAgentId());
                break;
            case SUB_AGENT_COMPLETED:
                sb.append("⬆ Sub-agent completed: ").append(message.getSubAgentId());
                break;
            case SUB_AGENT_ERROR:
                sb.append("✖ Sub-agent error: ").append(message.getSubAgentId());
                if (message.getResult() != null) sb.append(" → ").append(contentFormatter.apply(message.getResult()));
                break;
            case SYSTEM_PROMPT:
                sb.append("📋 System prompt");
                if (message.getResult() != null) {
                    sb.append('\n').append(contentFormatter.apply(message.getResult()));
                }
                break;
        }
        return sb.toString();
    }

    private static void formatToolRequest(@NotNull StringBuilder sb, @NotNull ActivityMessage message,
                                          @NotNull Function<String, String> contentFormatter) {
        sb.append("▶ ").append(message.getToolName());
        if (message.getArguments() != null) {
            sb.append(" ← ").append(contentFormatter.apply(message.getArguments()));
        }
    }

    private static void formatToolResponse(@NotNull StringBuilder sb, @NotNull ActivityMessage message,
                                           @NotNull Function<String, String> contentFormatter) {
        sb.append("✔ ").append(message.getToolName());
        if (message.getResult() != null) {
            sb.append(" → ").append(contentFormatter.apply(message.getResult()));
        }
    }

    private static void formatIntermediateResponse(@NotNull StringBuilder sb, @NotNull ActivityMessage message,
                                                   @NotNull Function<String, String> contentFormatter) {
        sb.append("\uD83D\uDCAC ");
        if (message.getResult() != null) {
            sb.append(contentFormatter.apply(message.getResult()));
        } else {
            sb.append("LLM intermediate response");
        }
    }

    /**
     * Formats a tool argument/result for the multi-line panel preview. Each input line maps to
     * an output line (real {@code \n} separators) so the renderer can render rows that grow
     * vertically. Each line is truncated to {@link #INLINE_PREVIEW_MAX_LINE_LEN} characters and
     * the overall block is capped at {@link #INLINE_PREVIEW_MAX_LINES} lines with a "(N more
     * lines)" hint; the full content remains available via tooltip and the double-click editor.
     */
    static @NotNull String formatForRow(@NotNull String text) {
        return formatForRow(text, INLINE_PREVIEW_MAX_LINE_LEN, INLINE_PREVIEW_MAX_LINES);
    }

    static @NotNull String formatForRow(@NotNull String text, int maxLineLen, int maxLines) {
        String[] lines = splitLines(text);
        int total = effectiveLineCount(lines);
        if (total == 0) return "";

        int shown = Math.min(total, maxLines);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            if (i > 0) sb.append('\n');
            String line = lines[i];
            if (line.length() > maxLineLen) {
                sb.append(line, 0, maxLineLen).append('…');
            } else {
                sb.append(line);
            }
        }
        if (total > shown) {
            sb.append('\n').append("… (").append(total - shown).append(" more lines)");
        }
        return sb.toString();
    }

    /**
     * Formats a tool argument/result for the copy-to-clipboard action. Single-line content
     * stays on the same line as its prefix (e.g. {@code → ok}). Multi-line content is moved
     * onto subsequent indented lines so newlines are preserved verbatim without losing the
     * visual link to the entry header.
     */
    static @NotNull String formatForClipboard(@NotNull String text) {
        String[] lines = splitLines(text);
        int total = effectiveLineCount(lines);
        if (total == 0) return "";
        if (total == 1) return lines[0];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            sb.append('\n').append("    ").append(lines[i]);
        }
        return sb.toString();
    }

    private static String[] splitLines(@NotNull String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private static int effectiveLineCount(String[] lines) {
        int n = lines.length;
        // Drop all trailing empty lines from final newline(s), so e.g. "line1\n\n" is reported
        // as a single-line result rather than "(2 lines)".
        while (n > 0 && lines[n - 1].isEmpty()) n--;
        return n;
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
            } else if (logEntry.source() == LogSource.RAW) {
                String fileName = "RawLog_" + logEntry.timestamp().replace(":", "").replace(".", "_") + ".json";
                String finalContent = formatJsonContent(logEntry.fullContent());
                ApplicationManager.getApplication().invokeLater(() -> {
                    LightVirtualFile virtualFile = new LightVirtualFile(fileName, finalContent);
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
              .append(entry.clipboardMessage()).append("\n");
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
                String sourceTag = switch (entry.source()) {
                    case MCP   -> "[MCP] ";
                    case AGENT -> "[AGT] ";
                    case RAG   -> "[RAG] ";
                    case RAW   -> "[RAW] ";
                };
                String badge = currentFilter == LogFilter.ALL ? sourceTag : "";
                String plain = entry.timestamp() + " " + badge + entry.message();
                label.setText(toHtmlRow(plain));
                label.setVerticalAlignment(SwingConstants.TOP);
                // No hover tooltip: large log entries produce a huge popup that flickers
                // as the mouse moves. Double-click opens the full content in an editor.
                label.setToolTipText(null);
                Color fg = resolveEntryColor(entry);
                if (fg != null) {
                    label.setForeground(fg);
                }
            }
            return label;
        }

        private @NotNull String toHtmlRow(@NotNull String text) {
            String escaped = text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>");
            return "<html><pre style=\"font-family:monospace;margin:0;padding:0\">" + escaped + "</pre></html>";
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
                    case SYSTEM_PROMPT -> JBColor.foreground();
                };
            }
            if (entry.source() == LogSource.MCP) {
                if (entry.message().startsWith("<")) return MCP_IN_COLOR;
                if (entry.message().startsWith(">")) return MCP_OUT_COLOR;
            }
            if (entry.source() == LogSource.RAW && entry.rawTrafficType() != null) {
                return switch (entry.rawTrafficType()) {
                    case REQUEST  -> MCP_OUT_COLOR;
                    case RESPONSE -> MCP_IN_COLOR;
                    case ERROR    -> ERROR_COLOR;
                };
            }
            return null;
        }
    }
}
