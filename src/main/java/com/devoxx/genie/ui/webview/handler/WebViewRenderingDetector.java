package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Detects rendering issues including the black rectangle problem.
 * This class focuses specifically on visual rendering problems.
 */
@Slf4j
public class WebViewRenderingDetector {
    
    private final JBCefBrowser browser;
    private final WebViewDebugLogger debugLogger;
    private final AtomicInteger consecutiveRenderIssues = new AtomicInteger(0);
    private final AtomicBoolean suspectedBlackRectangle = new AtomicBoolean(false);
    private final AtomicLong lastRenderCheck = new AtomicLong(System.currentTimeMillis());
    
    private Consumer<String> recoveryCallback;
    private Timer renderCheckTimer;
    
    // Black rectangle detection
    private static final long RENDER_CHECK_INTERVAL = 30000; // 30 seconds - reduced frequency
    private static final int MAX_CONSECUTIVE_RENDER_ISSUES = 3;
    private static final long COMPONENT_VISIBILITY_TIMEOUT = 30000; // 30 seconds
    
    public WebViewRenderingDetector(JBCefBrowser browser, WebViewDebugLogger debugLogger) {
        this.browser = browser;
        this.debugLogger = debugLogger;
        
        if (browser != null && JCEFChecker.isJCEFAvailable()) {
            debugLogger.debug("WebViewRenderingDetector initialized successfully");
        } else {
            debugLogger.warn("WebViewRenderingDetector - JCEF not available or browser is null");
        }
    }
    
    /**
     * Set callback to be called when recovery is needed.
     */
    public void setRecoveryCallback(Consumer<String> callback) {
        this.recoveryCallback = callback;
        debugLogger.debug("Recovery callback set for rendering detector");
    }
    
    /**
     * Start monitoring for rendering issues.
     */
    public void startMonitoring() {
        if (renderCheckTimer != null) {
            debugLogger.debug("Rendering monitoring already started, skipping");
            return;
        }
        
        debugLogger.debug("Starting rendering issue monitoring");
        
        // Start periodic render checks
        renderCheckTimer = new Timer((int) RENDER_CHECK_INTERVAL, e -> performRenderCheck());
        renderCheckTimer.start();
        
        debugLogger.info("Rendering monitoring started with {}ms interval", RENDER_CHECK_INTERVAL);
    }
    
    /**
     * Check if there are any rendering issues detected.
     */
    public boolean hasIssues() {
        boolean hasIssues = consecutiveRenderIssues.get() >= MAX_CONSECUTIVE_RENDER_ISSUES || 
                           suspectedBlackRectangle.get() ||
                           isComponentVisibilityStale();
        
        if (hasIssues) {
            debugLogger.logState("renderingHasIssues",
                                "consecutiveIssues", consecutiveRenderIssues.get(),
                                "suspectedBlackRectangle", suspectedBlackRectangle.get(),
                                "visibilityStale", isComponentVisibilityStale());
        }
        
        return hasIssues;
    }
    
    /**
     * Reset detection counters.
     */
    public void resetDetectionCounters() {
        consecutiveRenderIssues.set(0);
        suspectedBlackRectangle.set(false);
        lastRenderCheck.set(System.currentTimeMillis());
        debugLogger.debug("Rendering detection counters reset");
    }
    
    /**
     * Perform a render health check to detect black rectangle and other issues.
     */
    private void performRenderCheck() {
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.debug("Skipping render check - JCEF not available or browser is null");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Performing render health check");
        
        try {
            // Check component state
            JComponent component = browser.getComponent();
            if (component == null) {
                debugLogger.warn("Browser component is null during render check");
                incrementRenderIssues("Component is null");
                return;
            }
            
            boolean isVisible = component.isShowing();
            boolean isDisplayable = component.isDisplayable();
            int width = component.getWidth();
            int height = component.getHeight();
            
            debugLogger.logComponentInfo("renderCheck", isVisible, isDisplayable, width, height, "");
            
            // Check for suspicious component state
            if (isVisible && isDisplayable && (width <= 0 || height <= 0)) {
                debugLogger.warn("Component visible but has invalid size: {}x{}", width, height);
                incrementRenderIssues("Invalid component size");
                return;
            }
            
            if (isVisible && isDisplayable) {
                // Component looks good, perform JavaScript-based content check
                performContentRenderCheck();
            } else if (isVisible && !isDisplayable) {
                debugLogger.warn("Component is visible but not displayable - potential issue");
                incrementRenderIssues("Component visible but not displayable");
            } else {
                debugLogger.debug("Component not visible - skipping render check");
                resetDetectionCounters(); // Reset if component is not visible
            }
            
            lastRenderCheck.set(System.currentTimeMillis());
            
        } catch (Exception e) {
            debugLogger.error("Render check failed", e);
            incrementRenderIssues("Render check exception: " + e.getMessage());
        } finally {
            debugLogger.logTiming("renderCheck", startTime);
        }
    }
    
    /**
     * Perform JavaScript-based content rendering check.
     */
    private void performContentRenderCheck() {
        try {
            if (browser.getCefBrowser() == null) {
                debugLogger.debug("Cannot perform content render check - CEF browser is null");
                incrementRenderIssues("CEF browser is null");
                return;
            }
            
            String renderCheckJs = 
                "try { " +
                "  var body = document.body; " +
                "  var container = document.getElementById('conversation-container'); " +
                "  var computedStyle = body ? window.getComputedStyle(body) : null; " +
                "  " +
                "  window._renderCheck = { " +
                "    timestamp: Date.now(), " +
                "    bodyExists: !!body, " +
                "    bodyVisible: body ? body.offsetHeight > 0 && body.offsetWidth > 0 : false, " +
                "    backgroundColor: computedStyle ? computedStyle.backgroundColor : 'unknown', " +
                "    containerExists: !!container, " +
                "    containerVisible: container ? container.offsetHeight > 0 && container.offsetWidth > 0 : false, " +
                "    hasContent: container ? container.children.length > 0 || container.textContent.trim().length > 0 : false, " +
                "    documentReady: document.readyState === 'complete', " +
                "    windowVisible: document.visibilityState === 'visible' " +
                "  }; " +
                "  " +
                "  // Check for potential black rectangle indicators " +
                "  var suspiciousBlack = false; " +
                "  if (computedStyle) { " +
                "    var bgColor = computedStyle.backgroundColor; " +
                "    suspiciousBlack = bgColor === 'rgb(0, 0, 0)' || bgColor === 'rgba(0, 0, 0, 1)' || bgColor === 'black'; " +
                "  } " +
                "  window._renderCheck.suspiciousBlack = suspiciousBlack; " +
                "  " +
                "  console.log('Render check results:', window._renderCheck); " +
                "} catch(e) { " +
                "  window._renderCheck = { " +
                "    error: e.message, " +
                "    timestamp: Date.now() " +
                "  }; " +
                "  console.error('Render check JS error:', e); " +
                "}";
            
            browser.getCefBrowser().executeJavaScript(renderCheckJs, "", 0);
            debugLogger.debug("Content render check JavaScript injected");
            
            // Schedule evaluation of results
            Timer resultTimer = new Timer(3000, evt -> evaluateRenderCheckResults());
            resultTimer.setRepeats(false);
            resultTimer.start();
            
        } catch (Exception e) {
            debugLogger.error("Failed to perform content render check", e);
            incrementRenderIssues("Content render check failed: " + e.getMessage());
        }
    }
    
    /**
     * Evaluate render check results using heuristics.
     */
    private void evaluateRenderCheckResults() {
        try {
            // Since we can't directly access JavaScript results, we use heuristic evaluation
            // based on component state and timing patterns
            
            JComponent component = browser.getComponent();
            if (component == null) {
                incrementRenderIssues("Component became null during evaluation");
                return;
            }
            
            // Check if component appears to be in a problematic state
            boolean componentIssue = checkComponentForRenderingIssues(component);
            
            if (componentIssue) {
                incrementRenderIssues("Component rendering issue detected");
            } else {
                // If we reach here without issues, reset the counter
                if (consecutiveRenderIssues.get() > 0) {
                    debugLogger.debug("Render check passed - resetting issue counter");
                    consecutiveRenderIssues.set(0);
                    suspectedBlackRectangle.set(false);
                }
            }
            
        } catch (Exception e) {
            debugLogger.error("Failed to evaluate render check results", e);
            incrementRenderIssues("Evaluation failed: " + e.getMessage());
        }
    }
    
    /**
     * Check component for rendering issues using heuristics.
     */
    private boolean checkComponentForRenderingIssues(JComponent component) {
        try {
            // Get component properties
            boolean isOpaque = component.isOpaque();
            Color background = component.getBackground();
            boolean isVisible = component.isVisible();
            boolean isDisplayable = component.isDisplayable();
            
            debugLogger.logComponentInfo("renderIssueCheck", 
                                        isVisible, isDisplayable, 
                                        component.getWidth(), component.getHeight(),
                                        "opaque=" + isOpaque + ", bg=" + background);
            
            // Check for suspicious background color (potential black rectangle)
            if (background != null && background.equals(Color.BLACK)) {
                debugLogger.warn("Component has black background - potential black rectangle");
                suspectedBlackRectangle.set(true);
                return true;
            }
            
            // Check for component that should be visible but appears problematic
            if (isVisible && isDisplayable && component.getWidth() > 0 && component.getHeight() > 0) {
                // Component appears normal at this level
                return false;
            } else if (isVisible) {
                // Component is visible but has issues
                debugLogger.warn("Component is visible but has rendering issues");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            debugLogger.error("Failed to check component for rendering issues", e);
            return true; // Assume issue if we can't check
        }
    }
    
    /**
     * Check if component visibility state is stale.
     */
    private boolean isComponentVisibilityStale() {
        long timeSinceLastCheck = System.currentTimeMillis() - lastRenderCheck.get();
        boolean isStale = timeSinceLastCheck > COMPONENT_VISIBILITY_TIMEOUT;
        
        if (isStale) {
            debugLogger.debug("Component visibility check is stale: {}ms since last check", timeSinceLastCheck);
        }
        
        return isStale;
    }
    
    /**
     * Increment render issues counter but don't trigger automatic recovery.
     * Recovery will only happen when creating a new conversation.
     */
    private void incrementRenderIssues(String reason) {
        int issueCount = consecutiveRenderIssues.incrementAndGet();
        debugLogger.warn("Render issue detected: {} (consecutive issues: {})", reason, issueCount);
        
        if (issueCount >= MAX_CONSECUTIVE_RENDER_ISSUES) {
            debugLogger.warn("Max consecutive render issues reached - will recover on next new conversation");
            // Don't trigger automatic recovery - let the new conversation flow handle it
            // triggerRecovery("Render issues: " + reason + " (consecutive: " + issueCount + ")");
        }
    }
    
    /**
     * Force check for black rectangle issue.
     */
    public void forceBlackRectangleCheck() {
        debugLogger.debug("Forcing black rectangle check");
        
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.debug("Cannot force black rectangle check - JCEF not available");
            return;
        }
        
        try {
            // Immediate check without waiting for timer
            performRenderCheck();
            
            // Also inject specific black rectangle detection
            String blackRectangleCheckJs = 
                "try { " +
                "  var body = document.body; " +
                "  var computedStyle = body ? window.getComputedStyle(body) : null; " +
                "  var isBlack = false; " +
                "  " +
                "  if (computedStyle) { " +
                "    var bgColor = computedStyle.backgroundColor; " +
                "    isBlack = bgColor === 'rgb(0, 0, 0)' || bgColor === 'rgba(0, 0, 0, 1)' || bgColor === 'black'; " +
                "  } " +
                "  " +
                "  window._blackRectangleCheck = { " +
                "    timestamp: Date.now(), " +
                "    backgroundColor: computedStyle ? computedStyle.backgroundColor : 'unknown', " +
                "    isBlack: isBlack, " +
                "    bodyHeight: body ? body.offsetHeight : 0, " +
                "    bodyWidth: body ? body.offsetWidth : 0 " +
                "  }; " +
                "  " +
                "  if (isBlack) { " +
                "    console.warn('BLACK RECTANGLE DETECTED:', window._blackRectangleCheck); " +
                "  } else { " +
                "    console.log('Black rectangle check:', window._blackRectangleCheck); " +
                "  } " +
                "} catch(e) { " +
                "  console.error('Black rectangle check failed:', e); " +
                "}";
            
            browser.getCefBrowser().executeJavaScript(blackRectangleCheckJs, "", 0);
            debugLogger.debug("Black rectangle check JavaScript injected");
            
        } catch (Exception e) {
            debugLogger.error("Failed to force black rectangle check", e);
        }
    }
    
    /**
     * Trigger recovery callback.
     */
    private void triggerRecovery(String reason) {
        debugLogger.warn("Triggering recovery from rendering detector: {}", reason);
        if (recoveryCallback != null) {
            recoveryCallback.accept("RenderingDetector: " + reason);
        } else {
            debugLogger.warn("Recovery callback is null - cannot trigger recovery");
        }
    }
    
    /**
     * Dispose resources.
     */
    public void dispose() {
        debugLogger.debug("Disposing WebViewRenderingDetector");
        
        if (renderCheckTimer != null) {
            renderCheckTimer.stop();
            renderCheckTimer = null;
            debugLogger.debug("Render check timer stopped");
        }
        
        debugLogger.info("WebViewRenderingDetector disposed successfully");
    }
}
