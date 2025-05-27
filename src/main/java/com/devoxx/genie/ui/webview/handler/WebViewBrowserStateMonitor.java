package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Monitors browser state and detects issues that might require recovery.
 * This class focuses on browser loading, connection issues, and state tracking.
 */
@Slf4j
public class WebViewBrowserStateMonitor {
    
    private final JBCefBrowser browser;
    private final WebViewDebugLogger debugLogger;
    private final AtomicBoolean browserLoaded = new AtomicBoolean(false);
    private final AtomicBoolean hasConnectionIssues = new AtomicBoolean(false);
    private final AtomicInteger consecutiveLoadErrors = new AtomicInteger(0);
    private final AtomicLong lastSuccessfulLoad = new AtomicLong(System.currentTimeMillis());
    
    private Consumer<String> recoveryCallback;
    private Timer healthCheckTimer;
    
    private static final int MAX_CONSECUTIVE_ERRORS = 3;
    private static final long HEALTH_CHECK_INTERVAL = 15000; // 15 seconds
    private static final long MAX_TIME_SINCE_SUCCESSFUL_LOAD = 5 * 60 * 1000; // 5 minutes
    
    public WebViewBrowserStateMonitor(JBCefBrowser browser, WebViewDebugLogger debugLogger) {
        this.browser = browser;
        this.debugLogger = debugLogger;
        
        if (browser != null && JCEFChecker.isJCEFAvailable()) {
            setupBrowserHandlers();
            debugLogger.debug("WebViewBrowserStateMonitor initialized successfully");
        } else {
            debugLogger.warn("WebViewBrowserStateMonitor - JCEF not available or browser is null");
        }
    }
    
    /**
     * Set callback to be called when recovery is needed.
     */
    public void setRecoveryCallback(Consumer<String> callback) {
        this.recoveryCallback = callback;
        debugLogger.debug("Recovery callback set");
    }
    
    /**
     * Start monitoring browser state.
     */
    public void startMonitoring() {
        if (healthCheckTimer != null) {
            debugLogger.debug("Monitoring already started, skipping");
            return;
        }
        
        debugLogger.debug("Starting browser state monitoring");
        
        // Start periodic health checks
        healthCheckTimer = new Timer((int) HEALTH_CHECK_INTERVAL, e -> performHealthCheck());
        healthCheckTimer.start();
        
        debugLogger.info("Browser state monitoring started with {}ms interval", HEALTH_CHECK_INTERVAL);
    }
    
    /**
     * Check if there are any issues detected.
     */
    public boolean hasIssues() {
        boolean hasIssues = !browserLoaded.get() || 
                           hasConnectionIssues.get() || 
                           consecutiveLoadErrors.get() >= MAX_CONSECUTIVE_ERRORS ||
                           (System.currentTimeMillis() - lastSuccessfulLoad.get()) > MAX_TIME_SINCE_SUCCESSFUL_LOAD;
        
        if (hasIssues) {
            debugLogger.logState("hasIssues", 
                                "browserLoaded", browserLoaded.get(),
                                "hasConnectionIssues", hasConnectionIssues.get(),
                                "consecutiveLoadErrors", consecutiveLoadErrors.get(),
                                "timeSinceLastSuccess", System.currentTimeMillis() - lastSuccessfulLoad.get());
        }
        
        return hasIssues;
    }
    
    /**
     * Setup browser-specific handlers to detect state issues.
     */
    private void setupBrowserHandlers() {
        // Suppress false positive: getCefBrowser() can return null even when browser is not null
        //noinspection ConstantConditions
        if (browser == null || browser.getCefBrowser() == null) {
            debugLogger.warn("Cannot setup browser handlers - browser or CEF browser is null");
            return;
        }
        
        try {
            debugLogger.debug("Setting up browser load and display handlers");
            
            // Add load handler to track browser state
            browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadStart(CefBrowser cefBrowser, CefFrame frame, CefRequest.TransitionType transitionType) {
                    debugLogger.logBrowserInfo("onLoadStart", 
                                              frame != null ? frame.getURL() : "unknown", 
                                              0, 
                                              "transitionType=" + transitionType);
                    browserLoaded.set(false);
                }
                
                @Override
                public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                    String url = frame != null ? frame.getURL() : "unknown";
                    debugLogger.logBrowserInfo("onLoadEnd", url, httpStatusCode, "");
                    
                    boolean isSuccess = httpStatusCode == 200;
                    browserLoaded.set(isSuccess);
                    
                    if (isSuccess) {
                        consecutiveLoadErrors.set(0);
                        lastSuccessfulLoad.set(System.currentTimeMillis());
                        hasConnectionIssues.set(false);
                        debugLogger.debug("Successful load - resetting error counters");
                    } else {
                        int errorCount = consecutiveLoadErrors.incrementAndGet();
                        debugLogger.warn("Load ended with error status: {} (consecutive errors: {})", 
                                        httpStatusCode, errorCount);
                        
                        if (errorCount >= MAX_CONSECUTIVE_ERRORS) {
                            debugLogger.warn("Max consecutive errors reached, triggering recovery");
                            triggerRecovery("Load error: " + httpStatusCode + " (consecutive: " + errorCount + ")");
                        }
                    }
                }
                
                @Override
                public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode,
                                        String errorText, String failedUrl) {
                    debugLogger.logBrowserInfo("onLoadError", failedUrl, errorCode.getCode(), errorText);
                    
                    browserLoaded.set(false);
                    hasConnectionIssues.set(true);
                    int errorCount = consecutiveLoadErrors.incrementAndGet();
                    
                    debugLogger.error("Browser load error: {} - {} for URL: {} (consecutive errors: {})", 
                                     errorCode, errorText, failedUrl, errorCount);
                    
                    triggerRecovery("Load error: " + errorText + " (code: " + errorCode + ")");
                }
            }, browser.getCefBrowser());
            
            // Add display handler to detect connection issues
            browser.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
                @Override
                public void onStatusMessage(CefBrowser cefBrowser, String value) {
                    if (value != null && (value.contains("timeout") || 
                                         value.contains("connection failed") ||
                                         value.contains("connection refused") ||
                                         value.contains("network error"))) {
                        debugLogger.warn("Connection issue detected in status message: {}", value);
                        hasConnectionIssues.set(true);
                        triggerRecovery("Connection issue: " + value);
                    }
                }
            }, browser.getCefBrowser());
            
            debugLogger.debug("Browser handlers setup completed successfully");
            
        } catch (Exception e) {
            debugLogger.error("Failed to setup browser handlers", e);
        }
    }
    
    /**
     * Perform periodic health check.
     */
    private void performHealthCheck() {
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.debug("Skipping health check - JCEF not available or browser is null");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Performing periodic health check");
        
        try {
            // Check browser state
            //noinspection ConstantConditions
            String currentUrl = browser.getCefBrowser() != null ? browser.getCefBrowser().getURL() : "null";
            boolean isLoaded = browserLoaded.get();
            boolean hasConnIssues = hasConnectionIssues.get();
            int errorCount = consecutiveLoadErrors.get();
            long timeSinceSuccess = System.currentTimeMillis() - lastSuccessfulLoad.get();
            
            debugLogger.logState("healthCheck",
                                "url", currentUrl,
                                "loaded", isLoaded,
                                "connectionIssues", hasConnIssues,
                                "errorCount", errorCount,
                                "timeSinceSuccess", timeSinceSuccess);
            
            // Check for stale state (no successful load for too long)
            if (timeSinceSuccess > MAX_TIME_SINCE_SUCCESSFUL_LOAD) {
                debugLogger.warn("Browser state is stale - {}ms since last successful load", timeSinceSuccess);
                triggerRecovery("Stale browser state - " + timeSinceSuccess + "ms since last success");
            }
            
            // Inject health check JavaScript
            injectHealthCheckJS();
            
        } catch (Exception e) {
            debugLogger.error("Health check failed", e);
        } finally {
            debugLogger.logTiming("healthCheck", startTime);
        }
    }
    
    /**
     * Inject JavaScript to perform client-side health check.
     */
    private void injectHealthCheckJS() {
        try {
            //noinspection ConstantConditions
            if (browser.getCefBrowser() == null) {
                debugLogger.debug("Cannot inject health check JS - CEF browser is null");
                return;
            }
            
            String healthCheckJs = 
                "try { " +
                "  window._browserStateHealth = { " +
                "    timestamp: Date.now(), " +
                "    url: window.location.href, " +
                "    readyState: document.readyState, " +
                "    visible: document.visibilityState === 'visible', " +
                "    bodyExists: !!document.body, " +
                "    containerExists: !!document.getElementById('conversation-container'), " +
                "    hasContent: document.body ? document.body.innerHTML.length > 100 : false " +
                "  }; " +
                "  console.log('Browser health check:', window._browserStateHealth); " +
                "} catch(e) { " +
                "  window._browserStateHealth = { error: e.message, timestamp: Date.now() }; " +
                "  console.error('Health check JS error:', e); " +
                "}";
            
            browser.getCefBrowser().executeJavaScript(healthCheckJs, "", 0);
            debugLogger.debug("Health check JavaScript injected successfully");
            
            // Schedule a check to evaluate the results
            Timer jsResultTimer = new Timer(2000, evt -> evaluateHealthCheckResults());
            jsResultTimer.setRepeats(false);
            jsResultTimer.start();
            
        } catch (Exception e) {
            debugLogger.error("Failed to inject health check JavaScript", e);
        }
    }
    
    /**
     * Evaluate health check results (heuristic approach).
     */
    private void evaluateHealthCheckResults() {
        // Since we can't directly access JS results, we use heuristics
        // If browser appears loaded but we suspect issues, flag for recovery
        if (browserLoaded.get() && hasConnectionIssues.get()) {
            debugLogger.warn("Health check suggests browser state inconsistency");
            triggerRecovery("Browser state inconsistency detected");
        }
    }
    
    /**
     * Reset state counters.
     */
    public void resetCounters() {
        consecutiveLoadErrors.set(0);
        hasConnectionIssues.set(false);
        debugLogger.debug("State counters reset");
    }
    
    /**
     * Get current browser load state.
     */
    public boolean isBrowserLoaded() {
        return browserLoaded.get();
    }
    
    /**
     * Trigger recovery callback.
     */
    private void triggerRecovery(String reason) {
        debugLogger.warn("Triggering recovery from browser state monitor: {}", reason);
        if (recoveryCallback != null) {
            recoveryCallback.accept("BrowserStateMonitor: " + reason);
        } else {
            debugLogger.warn("Recovery callback is null - cannot trigger recovery");
        }
    }
    
    /**
     * Dispose resources.
     */
    public void dispose() {
        debugLogger.debug("Disposing WebViewBrowserStateMonitor");
        
        if (healthCheckTimer != null) {
            healthCheckTimer.stop();
            healthCheckTimer = null;
            debugLogger.debug("Health check timer stopped");
        }
        
        debugLogger.info("WebViewBrowserStateMonitor disposed successfully");
    }
}
