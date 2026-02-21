package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced JavaScript executor with better error handling and debugging.
 * This class handles JavaScript execution with improved reliability and debugging capabilities.
 */
@Slf4j
public class WebViewJavaScriptExecutor {

    private final JBCefBrowser browser;
    private final WebViewDebugLogger debugLogger;
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private final AtomicInteger executionCounter = new AtomicInteger(0);
    private final AtomicLong lastExecutionTime = new AtomicLong(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    // Queue for pending JavaScript executions when browser is not ready
    private final java.util.concurrent.ConcurrentLinkedQueue<PendingJSExecution> pendingExecutions =
        new java.util.concurrent.ConcurrentLinkedQueue<>();

    private Timer pendingExecutionTimer;
    private static final int MAX_PENDING_EXECUTIONS = 100;
    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    /**
     * After this many milliseconds of inactivity, a plain repaint() may not be
     * enough to wake CEF's rendering pipeline (e.g. Ollama model cold-start).
     * We schedule a second, delayed invalidate+revalidate+repaint in that case.
     */
    private static final long LONG_IDLE_THRESHOLD_MS = 5_000;
    
    /**
     * Class to hold pending JavaScript executions.
     */
    private static class PendingJSExecution {
        final String script;
        final long timestamp;
        final int attempts;
        
        PendingJSExecution(String script, int attempts) {
            this.script = script;
            this.timestamp = System.currentTimeMillis();
            this.attempts = attempts;
        }
    }
    
    public WebViewJavaScriptExecutor(JBCefBrowser browser) {
        this.browser = browser;
        this.debugLogger = new WebViewDebugLogger("WebViewJavaScriptExecutor");
        
        if (browser != null) {
            debugLogger.debug("WebViewJavaScriptExecutor initialized for browser");
            startPendingExecutionProcessor();
        } else {
            debugLogger.warn("WebViewJavaScriptExecutor initialized with null browser");
        }
    }
    
    /**
     * Get the loaded state.
     */
    public boolean isLoaded() {
        return isLoaded.get();
    }
    
    /**
     * Set the loaded state with debugging.
     */
    public void setLoaded(boolean loaded) {
        boolean wasLoaded = isLoaded.getAndSet(loaded);
        if (wasLoaded != loaded) {
            debugLogger.info("Browser loaded state changed: {} -> {}", wasLoaded, loaded);
            if (loaded && !pendingExecutions.isEmpty()) {
                debugLogger.info("Browser became loaded, processing {} pending executions", pendingExecutions.size());
                processPendingExecutions();
            }
        }
    }

    /**
     * Execute JavaScript in the browser with enhanced error handling.
     *
     * @param script The JavaScript to execute
     */
    public void executeJavaScript(String script) {
        if (script == null || script.trim().isEmpty()) {
            debugLogger.warn("Attempted to execute null or empty JavaScript");
            return;
        }
        
        // Skip JavaScript execution if JCEF is not available or browser is null
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.debug("JCEF not available or browser is null, skipping JavaScript execution: {}", 
                    script.length() > 50 ? script.substring(0, 50) + "..." : script);
            return;
        }
        
        int execNumber = executionCounter.incrementAndGet();
        debugLogger.debug("JavaScript execution #{}: {}", execNumber, 
                         script.length() > 100 ? script.substring(0, 100) + "..." : script);
        
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (isLoaded.get() && browser.getCefBrowser() != null) {
                    long startTime = System.currentTimeMillis();
                    long idleMs = lastExecutionTime.get() > 0 ? startTime - lastExecutionTime.get() : 0;

                    browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
                    lastExecutionTime.set(System.currentTimeMillis());
                    failureCount.set(0); // Reset failure count on success

                    // Force JCEF component repaint — in OSR mode, DOM changes via
                    // executeJavaScript may not trigger the Swing paint cycle automatically
                    browser.getComponent().repaint();

                    // After a long idle (e.g. Ollama model cold-start), a single repaint()
                    // is sometimes not enough to wake CEF's rendering pipeline.  Schedule a
                    // second, more aggressive repaint shortly after to guarantee the content
                    // becomes visible without the user having to click/move the panel.
                    if (idleMs > LONG_IDLE_THRESHOLD_MS) {
                        debugLogger.debug("Long idle detected ({}ms) — scheduling aggressive repaint after JS execution", idleMs);
                        Timer aggressiveRepaint = new Timer(150, e -> {
                            Component comp = browser.getComponent();
                            if (comp != null) {
                                comp.invalidate();
                                comp.revalidate();
                                comp.repaint();
                                for (Container parent = comp.getParent(); parent != null; parent = parent.getParent()) {
                                    parent.invalidate();
                                    parent.revalidate();
                                    parent.repaint();
                                }
                            }
                        });
                        aggressiveRepaint.setRepeats(false);
                        aggressiveRepaint.start();
                    }

                    debugLogger.logTiming("jsExecution#" + execNumber, startTime);
                } else {
                    String reason = !isLoaded.get() ? "not loaded" : "CEF browser is null";
                    debugLogger.warn("Browser not ready for JavaScript execution ({}), queueing script #{}", reason, execNumber);
                    queuePendingExecution(script);
                }
            } catch (Exception e) {
                int failures = failureCount.incrementAndGet();
                debugLogger.error("Error executing JavaScript #{} (failure #{}): {}", execNumber, failures, e.getMessage());
                
                if (failures < MAX_CONSECUTIVE_FAILURES) {
                    debugLogger.debug("Queueing failed execution for retry");
                    queuePendingExecution(script);
                } else {
                    debugLogger.error("Max consecutive failures reached, dropping JavaScript execution");
                }
            }
        });
    }
    
    /**
     * Queue JavaScript execution for later when browser is ready.
     */
    private void queuePendingExecution(String script) {
        if (pendingExecutions.size() >= MAX_PENDING_EXECUTIONS) {
            // Remove oldest execution to make room
            PendingJSExecution removed = pendingExecutions.poll();
            if (removed != null) {
                debugLogger.warn("Pending execution queue full, dropped oldest execution");
            }
        }
        
        pendingExecutions.offer(new PendingJSExecution(script, 0));
        debugLogger.debug("Queued JavaScript execution, {} pending", pendingExecutions.size());
    }
    
    /**
     * Start the processor for pending executions.
     */
    private void startPendingExecutionProcessor() {
        if (pendingExecutionTimer != null) {
            return;
        }
        
        pendingExecutionTimer = new Timer(2000, e -> processPendingExecutions());
        pendingExecutionTimer.start();
        debugLogger.debug("Started pending execution processor with 2s interval");
    }
    
    /**
     * Process pending JavaScript executions.
     */
    private void processPendingExecutions() {
        if (pendingExecutions.isEmpty() || !isLoaded.get() || browser == null || browser.getCefBrowser() == null) {
            return;
        }
        
        int processed = 0;
        int failed = 0;
        long startTime = System.currentTimeMillis();
        
        while (!pendingExecutions.isEmpty() && processed < 10) { // Limit batch size
            PendingJSExecution pending = pendingExecutions.poll();
            if (pending == null) break;
            
            // Check if execution is too old (avoid executing stale JavaScript)
            if (System.currentTimeMillis() - pending.timestamp > 30000) { // 30 seconds
                debugLogger.debug("Discarded stale pending execution");
                continue;
            }
            
            try {
                browser.getCefBrowser().executeJavaScript(pending.script, browser.getCefBrowser().getURL(), 0);
                processed++;
                debugLogger.debug("Executed pending JavaScript (attempt {})", pending.attempts + 1);
            } catch (Exception e) {
                failed++;
                debugLogger.warn("Failed to execute pending JavaScript (attempt {}): {}", pending.attempts + 1, e.getMessage());
                
                // Retry with increased attempt count
                if (pending.attempts < 3) {
                    pendingExecutions.offer(new PendingJSExecution(pending.script, pending.attempts + 1));
                } else {
                    debugLogger.error("Dropping JavaScript execution after {} attempts", pending.attempts + 1);
                }
            }
        }
        
        if (processed > 0 || failed > 0) {
            debugLogger.info("Processed pending executions: {} successful, {} failed, {} remaining", 
                           processed, failed, pendingExecutions.size());
            debugLogger.logTiming("processPendingExecutions", startTime);
        }
    }
    
    /**
     * Execute JavaScript with retry capability.
     */
    public void executeJavaScriptWithRetry(String script, int maxRetries) {
        executeJavaScriptWithRetry(script, maxRetries, 1000);
    }
    
    /**
     * Execute JavaScript with retry capability and custom delay.
     */
    public void executeJavaScriptWithRetry(String script, int maxRetries, int retryDelayMs) {
        if (script == null || script.trim().isEmpty()) {
            debugLogger.warn("Attempted to execute null or empty JavaScript with retry");
            return;
        }
        
        debugLogger.debug("Executing JavaScript with retry (max retries: {}, delay: {}ms): {}", 
                         maxRetries, retryDelayMs, 
                         script.length() > 50 ? script.substring(0, 50) + "..." : script);
        
        attemptJavaScriptExecution(script, maxRetries, retryDelayMs, 0);
    }
    
    /**
     * Attempt JavaScript execution with retry logic.
     */
    private void attemptJavaScriptExecution(String script, int maxRetries, int retryDelayMs, int currentAttempt) {
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.debug("Cannot execute JavaScript - JCEF not available or browser is null");
            return;
        }
        
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (isLoaded.get() && browser.getCefBrowser() != null) {
                    browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
                    debugLogger.debug("JavaScript execution successful on attempt {}", currentAttempt + 1);
                } else if (currentAttempt < maxRetries) {
                    debugLogger.debug("Browser not ready, scheduling retry {} in {}ms", currentAttempt + 1, retryDelayMs);
                    Timer retryTimer = new Timer(retryDelayMs, e -> 
                        attemptJavaScriptExecution(script, maxRetries, retryDelayMs, currentAttempt + 1));
                    retryTimer.setRepeats(false);
                    retryTimer.start();
                } else {
                    debugLogger.warn("JavaScript execution failed after {} attempts - browser not ready", maxRetries + 1);
                }
            } catch (Exception e) {
                if (currentAttempt < maxRetries) {
                    debugLogger.debug("JavaScript execution failed on attempt {}, scheduling retry: {}", 
                                    currentAttempt + 1, e.getMessage());
                    Timer retryTimer = new Timer(retryDelayMs, evt -> 
                        attemptJavaScriptExecution(script, maxRetries, retryDelayMs, currentAttempt + 1));
                    retryTimer.setRepeats(false);
                    retryTimer.start();
                } else {
                    debugLogger.error("JavaScript execution failed after {} attempts: {}", maxRetries + 1, e.getMessage());
                }
            }
        });
    }
    
    /**
     * Get execution statistics for debugging.
     */
    public String getExecutionStats() {
        return String.format("Executions: %d, Failures: %d, Pending: %d, Last execution: %dms ago", 
                           executionCounter.get(), 
                           failureCount.get(),
                           pendingExecutions.size(),
                           lastExecutionTime.get() > 0 ? System.currentTimeMillis() - lastExecutionTime.get() : 0);
    }
    
    /**
     * Force process all pending executions (for debugging).
     */
    public void forcePendingExecution() {
        debugLogger.info("Force processing pending executions - {} queued", pendingExecutions.size());
        processPendingExecutions();
    }
    
    /**
     * Clear all pending executions.
     */
    public void clearPendingExecutions() {
        int cleared = pendingExecutions.size();
        pendingExecutions.clear();
        debugLogger.info("Cleared {} pending JavaScript executions", cleared);
    }
    
    /**
     * Escape JavaScript string literals.
     * This prevents issues when inserting HTML into JavaScript template literals.
     *
     * @param text The text to escape
     * @return Escaped text suitable for use in JavaScript
     */
    public @NotNull String escapeJS(@NotNull String text) {
        return text.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${");
    }
    
    /**
     * Escape HTML special characters.
     *
     * @param text The text to escape
     * @return Escaped HTML text
     */
    public @NotNull String escapeHtml(@NotNull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
    /**
     * Dispose resources.
     */
    public void dispose() {
        debugLogger.debug("Disposing WebViewJavaScriptExecutor");
        
        if (pendingExecutionTimer != null) {
            pendingExecutionTimer.stop();
            pendingExecutionTimer = null;
            debugLogger.debug("Pending execution timer stopped");
        }
        
        int pendingCount = pendingExecutions.size();
        pendingExecutions.clear();
        
        debugLogger.info("WebViewJavaScriptExecutor disposed - cleared {} pending executions", pendingCount);
    }
}
