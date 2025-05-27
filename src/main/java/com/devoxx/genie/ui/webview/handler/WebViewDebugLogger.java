package com.devoxx.genie.ui.webview.handler;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced debug logger for WebView recovery operations.
 * Provides detailed logging with context and history tracking.
 */
@Slf4j
public class WebViewDebugLogger {
    
    private final String context;
    private final AtomicInteger debugCounter = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<String> recentLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_LOGS = 50;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    public WebViewDebugLogger(String context) {
        this.context = context;
    }
    
    /**
     * Log debug message with enhanced context.
     */
    public void debug(String message, Object... args) {
        String formattedMessage = formatMessage("DEBUG", message, args);
        log.debug("[{}] {}", context, formattedMessage);
        addToRecentLogs("DEBUG", formattedMessage);
    }
    
    /**
     * Log info message with enhanced context.
     */
    public void info(String message, Object... args) {
        String formattedMessage = formatMessage("INFO", message, args);
        log.info("[{}] {}", context, formattedMessage);
        addToRecentLogs("INFO", formattedMessage);
    }
    
    /**
     * Log warning message with enhanced context.
     */
    public void warn(String message, Object... args) {
        String formattedMessage = formatMessage("WARN", message, args);
        log.warn("[{}] {}", context, formattedMessage);
        addToRecentLogs("WARN", formattedMessage);
    }
    
    /**
     * Log error message with enhanced context.
     */
    public void error(String message, Throwable throwable) {
        String formattedMessage = formatMessage("ERROR", message);
        log.error("[{}] {}", context, formattedMessage, throwable);
        addToRecentLogs("ERROR", formattedMessage + " - " + throwable.getMessage());
    }
    
    /**
     * Log error message with enhanced context.
     */
    public void error(String message, Object... args) {
        String formattedMessage = formatMessage("ERROR", message, args);
        log.error("[{}] {}", context, formattedMessage);
        addToRecentLogs("ERROR", formattedMessage);
    }
    
    /**
     * Log state information for debugging.
     */
    public void logState(String operation, Object... stateInfo) {
        StringBuilder stateBuilder = new StringBuilder();
        stateBuilder.append("STATE[").append(operation).append("]: ");
        
        for (int i = 0; i < stateInfo.length; i += 2) {
            if (i + 1 < stateInfo.length) {
                stateBuilder.append(stateInfo[i]).append("=").append(stateInfo[i + 1]);
                if (i + 2 < stateInfo.length) {
                    stateBuilder.append(", ");
                }
            }
        }
        
        String stateMessage = stateBuilder.toString();
        log.debug("[{}] {}", context, stateMessage);
        addToRecentLogs("STATE", stateMessage);
    }
    
    /**
     * Log timing information for performance debugging.
     */
    public void logTiming(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        String timingMessage = String.format("TIMING[%s]: %dms", operation, duration);
        log.debug("[{}] {}", context, timingMessage);
        addToRecentLogs("TIMING", timingMessage);
    }
    
    /**
     * Log browser information for debugging.
     */
    public void logBrowserInfo(String operation, String url, int statusCode, String additionalInfo) {
        String browserMessage = String.format("BROWSER[%s]: url=%s, status=%d, info=%s", 
                                             operation, url, statusCode, additionalInfo);
        log.debug("[{}] {}", context, browserMessage);
        addToRecentLogs("BROWSER", browserMessage);
    }
    
    /**
     * Log component information for debugging.
     */
    public void logComponentInfo(String operation, boolean visible, boolean displayable, 
                                int width, int height, String additionalInfo) {
        String componentMessage = String.format("COMPONENT[%s]: visible=%b, displayable=%b, size=%dx%d, info=%s", 
                                               operation, visible, displayable, width, height, additionalInfo);
        log.debug("[{}] {}", context, componentMessage);
        addToRecentLogs("COMPONENT", componentMessage);
    }
    
    /**
     * Log recovery attempt with detailed information.
     */
    public void logRecoveryAttempt(String strategy, String reason, boolean success, String details) {
        String recoveryMessage = String.format("RECOVERY[%s]: reason=%s, success=%b, details=%s", 
                                              strategy, reason, success, details);
        if (success) {
            log.info("[{}] {}", context, recoveryMessage);
        } else {
            log.warn("[{}] {}", context, recoveryMessage);
        }
        addToRecentLogs("RECOVERY", recoveryMessage);
    }
    
    /**
     * Get recent log entries for debugging.
     */
    public String getRecentLogs() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Recent Log Entries (").append(context).append(") ===\\n");
        
        for (String logEntry : recentLogs) {
            sb.append(logEntry).append("\\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Dump all recent logs to the main logger (for debugging purposes).
     */
    public void dumpRecentLogs() {
        log.info("[{}] Dumping recent logs for debugging:", context);
        for (String logEntry : recentLogs) {
            log.info("[{}] HISTORY: {}", context, logEntry);
        }
    }
    
    /**
     * Format message with arguments and add sequence number.
     */
    private String formatMessage(String level, String message, Object... args) {
        String formattedMessage;
        try {
            formattedMessage = String.format(message, args);
        } catch (Exception e) {
            formattedMessage = message + " [FORMATTING_ERROR: " + e.getMessage() + "]";
        }
        
        int sequenceNumber = debugCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        
        return String.format("[%s][%s][#%d] %s", timestamp, level, sequenceNumber, formattedMessage);
    }
    
    /**
     * Add message to recent logs queue.
     */
    private void addToRecentLogs(String level, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String logEntry = String.format("[%s][%s] %s", timestamp, level, message);
        
        recentLogs.offer(logEntry);
        
        // Keep only the most recent logs
        while (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.poll();
        }
    }
}
