package com.devoxx.genie.ui.panel.mcp;

import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
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
 * Panel for displaying MCP logs with double-click functionality
 * to open complete logs in a new editor tab.
 */
@Slf4j
public class MCPLogPanel extends SimpleToolWindowPanel implements MCPLoggingMessage, Disposable {
    
    private static final String WELCOME_MESSAGE = "MCP Log panel is now active. All MCP communication logs will appear here.";
    
    private static final String MCP_DISABLED_MESSAGE = "MCP logging is currently disabled. Enable it using the 'MCP On/Off' button above.";
    private static final String MCP_EMPTY_LOGS_MESSAGE = "No MCP logs captured yet. They will appear here when available.";

    private static final int DEFAULT_MAX_LOG_ENTRIES = 1000;
    private int maxLogEntries = DEFAULT_MAX_LOG_ENTRIES;
    private boolean isPaused = false;
    private final List<LogEntry> pendingLogs = new ArrayList<>(); // Buffer for batching logs
    private static final int BATCH_SIZE = 20; // Process logs in batches for better performance
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Project project;
    private final DefaultListModel<LogEntry> logListModel;
    private final JBList<LogEntry> logList;
    private final List<LogEntry> fullLogs = new ArrayList<>();
    
    /**
     * Constructor for MCPLogPanel
     *
     * @param project The current project
     */
    public MCPLogPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        
        // Create list model and list component with virtual mode for efficiency
        logListModel = new DefaultListModel<>();
        
        // Add placeholder messages based on MCP state
        if (!MCPService.isDebugLogsEnabled()) {
            logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), MCP_DISABLED_MESSAGE));
        } else {
            logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), WELCOME_MESSAGE));
            logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), MCP_EMPTY_LOGS_MESSAGE));
        }
        
        logList = new JBList<>(logListModel);
        logList.setCellRenderer(new LogEntryRenderer());
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Enable virtual mode for better performance with large log lists
        logList.setFixedCellHeight(24); // Fixed height for better performance
        logList.setVisibleRowCount(20); // Only render visible items
        
        // Add double-click listener
        logList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = logList.locationToIndex(e.getPoint());
                    if (index >= 0 && index < logListModel.size()) {
                        openLogInEditor(logListModel.getElementAt(index));
                        log.debug("Opening log entry at index: " + index);
                    }
                }
            }
        });
        
        // Create scroll pane
        JBScrollPane scrollPane = new JBScrollPane(logList);
        scrollPane.setBorder(JBUI.Borders.empty());
        
        // Add the scroll pane to this panel
        setContent(scrollPane);
        
        // Add toolbar with actions
        setupToolbar();
        
        // Delay subscription to MCP logging messages for better initial performance
        // This ensures the UI is fully constructed before handling logs
        ApplicationManager.getApplication().invokeLater(() -> {
            MessageBusUtil.connect(project, connection -> {
                MessageBusUtil.subscribe(connection, AppTopics.MCP_LOGGING_MSG, this);
                Disposer.register(this, connection);
            });
        });
    }

    private void setupToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        // MCP on/off toggle - use null icon initially as it will be set in update()
        actionGroup.add(new ToggleAction("MCP On/Off", "Enable/disable MCP and MCP logs", null) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return DevoxxGenieStateService.getInstance().getMcpEnabled() &&
                       DevoxxGenieStateService.getInstance().getMcpDebugLogsEnabled();
            }
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
                stateService.setMcpEnabled(state);
                stateService.setMcpDebugLogsEnabled(state);
                
                // Update log display with appropriate message
                ApplicationManager.getApplication().invokeLater(() -> {
                    clearLogs();
                    if (!state) {
                        logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), MCP_DISABLED_MESSAGE));
                        fullLogs.add(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), MCP_DISABLED_MESSAGE));
                    } else {
                        logListModel.addElement(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), MCP_EMPTY_LOGS_MESSAGE));
                        fullLogs.add(new LogEntry(LocalDateTime.now().format(TIME_FORMATTER), MCP_EMPTY_LOGS_MESSAGE));
                    }
                });
                
                // Notify listeners about MCP state change via the message bus
                ApplicationManager.getApplication().getMessageBus().syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC)
                        .settingsChanged(true);
                
                // Notify the user of the change
                String message = state ? "MCP and MCP logs enabled" : "MCP and MCP logs disabled";
                com.devoxx.genie.ui.util.NotificationUtil.sendNotification(project, message);
            }
            
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                boolean mcpEnabled = DevoxxGenieStateService.getInstance().getMcpEnabled() &&
                                   DevoxxGenieStateService.getInstance().getMcpDebugLogsEnabled();
                
                // Update icon based on state - green play icon when off, red stop icon when on
                e.getPresentation().setIcon(mcpEnabled ? 
                        IconLoader.getIcon("/actions/suspend.svg", MCPLogPanel.class) : 
                        IconLoader.getIcon("/actions/execute.svg", MCPLogPanel.class));
                e.getPresentation().setText(mcpEnabled ? "Stop MCP Logging" : "Start MCP Logging");
            }
        });
        
        // Clear logs action
        actionGroup.add(new AnAction("Clear Logs", "Clear all log entries", 
                IconLoader.getIcon("/actions/gc.svg", MCPLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                clearLogs();
            }
        });
        
        // Pause/Resume toggle
        actionGroup.add(new ToggleAction("Pause", "Pause log collection",
                IconLoader.getIcon("/actions/pause.svg", MCPLogPanel.class)) {
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
        
        // Settings action for log retention
        actionGroup.add(new AnAction("Settings", "Configure log retention",
                IconLoader.getIcon("/general/settings.svg", MCPLogPanel.class)) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showSettingsDialog();
            }
        });
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("MCPLogPanelToolbar", actionGroup, true);
        setToolbar(toolbar.getComponent());
    }
    
    /**
     * Shows a dialog to configure log retention settings
     */
    private void showSettingsDialog() {
        String result = Messages.showInputDialog(
                project, 
                "Maximum number of log entries to keep in memory:", 
                "MCP Log Settings", 
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
            } catch (NumberFormatException ignored) {}
        }
    }
    
    /**
     * Prune logs to the maximum size
     */
    private void pruneLogsToMaxSize() {
        ApplicationManager.getApplication().invokeLater(() -> {
            // Prune full logs
            while (fullLogs.size() > maxLogEntries) {
                fullLogs.remove(0);
            }
            
            // Prune displayed list model
            while (logListModel.size() > maxLogEntries) {
                logListModel.remove(0);
            }
        });
    }
    
    /**
     * Process logs in batches for better performance
     */
    private synchronized void processPendingLogs() {
        if (pendingLogs.isEmpty()) {
            return;
        }
        
        // Take a copy of the pending logs to process
        List<LogEntry> logsToProcess = new ArrayList<>(pendingLogs);
        pendingLogs.clear();
        
        ApplicationManager.getApplication().invokeLater(() -> {
            // Clear placeholder messages if this is the first real log
            if (fullLogs.size() == 1) {
                String firstLogMessage = fullLogs.get(0).getLogMessage();
                if (firstLogMessage.equals(MCP_DISABLED_MESSAGE) || firstLogMessage.equals(MCP_EMPTY_LOGS_MESSAGE)) {
                    fullLogs.clear();
                    logListModel.clear();
                }
            }
            // Check if we need to remove old entries to make room
            int excess = (fullLogs.size() + logsToProcess.size()) - maxLogEntries;
            if (excess > 0) {
                // Remove oldest entries if we're going to exceed capacity
                for (int i = 0; i < excess && !fullLogs.isEmpty(); i++) {
                    fullLogs.remove(0);
                    if (!logListModel.isEmpty()) {
                        logListModel.remove(0);
                    }
                }
            }
            
            // Add all new entries in one batch
            fullLogs.addAll(logsToProcess);
            for (LogEntry entry : logsToProcess) {
                logListModel.addElement(entry);
            }
            
            // Always scroll to the bottom
            if (!logListModel.isEmpty()) {
                int lastIndex = logListModel.size() - 1;
                logList.ensureIndexIsVisible(lastIndex);
            }
        });
    }
    
    @Override
    public void onMCPLoggingMessage(String message) {
        if (message == null || message.isEmpty() || isPaused) {
            return;
        }
        
        // Create a new log entry with timestamp
        LogEntry entry = new LogEntry(
                LocalDateTime.now().format(TIME_FORMATTER),
                message
        );
        
        // Add to pending logs for batch processing
        synchronized (pendingLogs) {
            pendingLogs.add(entry);
            
            // Process logs in batches for better UI performance
            if (pendingLogs.size() >= BATCH_SIZE) {
                processPendingLogs();
            } else if (pendingLogs.size() == 1) {
                // Schedule processing if this is the first entry in a new batch
                // This ensures logs are still displayed without waiting for the batch to fill
                Timer timer = new Timer(100, e -> processPendingLogs());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }
    
    /**
     * Open the log entry in a new editor tab without custom JSON formatting
     *
     * @param logEntry The log entry to open
     */
    private void openLogInEditor(LogEntry logEntry) {
        if (logEntry == null) {
            return;
        }
        
        try {
            // Get original content
            String content = logEntry.getLogMessage();
            
            // Log to help with debugging
            log.debug("Opening log entry: {} ... ", content.substring(0, Math.min(50, content.length())));

                    // Remove the first character (< or >) to make the JSON valid
            if (content.startsWith("<") || content.startsWith(">")) {
                content = content.substring(1).trim();
            }
            
            // Create a filename based on the timestamp
            String fileName = "MCPLog_" + logEntry.getTimestamp().replace(":", "").replace(".", "_") + ".json";
            
            // Create a virtual file and open it in the editor using invokeLater to avoid threading issues
            String finalContent = content;
            ApplicationManager.getApplication().invokeLater(() -> {
                // Create virtual file
                LightVirtualFile virtualFile = new LightVirtualFile(fileName, JsonFileType.INSTANCE, finalContent);
                
                // Open the file in the editor
                FileEditorManager.getInstance(project).openFile(virtualFile, true);
            });
        } catch (Exception e) {
            // Log any exceptions to help diagnose issues
            log.error("Error opening MCP log entry: " + e.getMessage());
            
            // Notify the user about the error
            com.devoxx.genie.ui.util.NotificationUtil.sendNotification(
                project, 
                "Error opening MCP log: " + e.getMessage()
            );
        }
    }
    
    /**
     * Clear all logs
     */
    private void clearLogs() {
        // Clear pending logs first
        synchronized (pendingLogs) {
            pendingLogs.clear();
        }
        
        // Then clear the displayed logs
        ApplicationManager.getApplication().invokeLater(() -> {
            fullLogs.clear();
            logListModel.clear();
        });
    }
    
    @Override
    public void dispose() {
        // Clean up resources
        synchronized (pendingLogs) {
            pendingLogs.clear();
        }
        fullLogs.clear();
        logListModel.clear();
    }
    
    /**
     * Log entry data class
     */
    @Getter
    private static class LogEntry {
        private final String timestamp;
        private final String logMessage;
        
        public LogEntry(String timestamp, String logMessage) {
            this.timestamp = timestamp;
            this.logMessage = logMessage;
        }

        @Override
        public String toString() {
            return timestamp + " " + logMessage;
        }
    }
    
    /**
     * Custom renderer for log entries with optimization to reduce HTML generation
     */
    private static class LogEntryRenderer extends DefaultListCellRenderer {
        // Colors for different types of messages
        private static final Color TIMESTAMP_COLOR = new Color(127, 127, 127);
        private static final Color INCOMING_COLOR = new Color(76, 175, 80);  // Green
        private static final Color OUTGOING_COLOR = new Color(33, 150, 243); // Blue
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                     boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof LogEntry logEntry) {
                // Only use HTML when needed for complex formatting
                // For simple cases, we'll just set the text directly with colors
                if (!isSelected) {
                    String message = logEntry.getLogMessage();

                    label.setText(logEntry.getTimestamp() + " " + message);
                    
                    // Set text color based on message type
                    if (message.startsWith("<")) {
                        label.setForeground(INCOMING_COLOR);
                    } else if (message.startsWith(">")) {
                        label.setForeground(OUTGOING_COLOR);
                    }
                }
                
                // Set tooltip with just the message part for clarity
                label.setToolTipText(logEntry.getLogMessage());
            }
            
            return label;
        }
    }
}