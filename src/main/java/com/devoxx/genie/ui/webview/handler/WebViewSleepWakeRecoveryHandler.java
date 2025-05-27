package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main coordinator for sleep/wake recovery for JCEF browsers.
 * This class coordinates multiple specialized handlers to address the black rectangle issue.
 */
@Slf4j
public class WebViewSleepWakeRecoveryHandler {
    
    private final JBCefBrowser browser;
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean needsRecovery = new AtomicBoolean(false);
    
    // Specialized handlers
    private WebViewBrowserStateMonitor browserStateMonitor;
    private WebViewRenderingDetector renderingDetector;
    private WebViewRecoveryStrategies recoveryStrategies;
    private WebViewDebugLogger debugLogger;
    
    // Recovery state tracking
    private Timer monitoringTimer;
    private Timer recoveryTimer;
    
    // Sleep detection threshold (3 minutes of inactivity suggests sleep)
    private static final long SLEEP_DETECTION_THRESHOLD = 3 * 60 * 1000; // 3 minutes
    
    // Component event debouncing to reduce aggressive recovery attempts
    private static final long COMPONENT_EVENT_DEBOUNCE_MS = 1000; // 1 second
    private final AtomicLong lastComponentEventTime = new AtomicLong(0);
    
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);

    public WebViewSleepWakeRecoveryHandler(JBCefBrowser browser) {
        this.browser = browser;
        
        if (browser != null && JCEFChecker.isJCEFAvailable()) {
            initializeHandlers();
            initializeMonitoring();
        } else {
            log.warn("WebViewSleepWakeRecoveryHandler - JCEF not available or browser is null");
        }
    }
    
    /**
     * Initialize specialized handlers.
     */
    private void initializeHandlers() {
        try {
            debugLogger = new WebViewDebugLogger("WebViewSleepWakeRecoveryHandler");
            debugLogger.debug("Initializing specialized handlers");
            
            browserStateMonitor = new WebViewBrowserStateMonitor(browser, debugLogger);
            renderingDetector = new WebViewRenderingDetector(browser, debugLogger);
            recoveryStrategies = new WebViewRecoveryStrategies(browser, debugLogger);
            
            // Set up callbacks
            browserStateMonitor.setRecoveryCallback(this::onRecoveryNeeded);
            renderingDetector.setRecoveryCallback(this::onRecoveryNeeded);
            
            debugLogger.debug("All specialized handlers initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize specialized handlers", e);
        }
    }
    
    /**
     * Initialize monitoring for sleep/wake events.
     */
    private void initializeMonitoring() {
        if (isMonitoring.get()) {
            debugLogger.debug("Monitoring already initialized, skipping");
            return;
        }
        
        try {
            debugLogger.debug("Starting monitoring initialization");
            
            // Set up component listeners for visibility changes
            setupComponentListeners();
            
            // Set up periodic monitoring for sleep detection
            setupSleepDetectionMonitoring();
            
            // Start specialized monitoring
            browserStateMonitor.startMonitoring();
            renderingDetector.startMonitoring();
            
            isMonitoring.set(true);
            debugLogger.info("Sleep/wake recovery monitoring initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize sleep/wake recovery monitoring", e);
            debugLogger.error("Monitoring initialization failed", e);
        }
    }
    
    /**
     * Set up component listeners to detect visibility and focus changes.
     */
    private void setupComponentListeners() {
        if (browser == null || browser.getComponent() == null) {
            debugLogger.warn("Cannot setup component listeners - browser or component is null");
            return;
        }
        
        JComponent component = browser.getComponent();
        debugLogger.debug("Setting up component listeners for browser component");
        
        // Monitor component visibility changes
        component.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (component.isShowing()) {
                        debugLogger.debug("Component became visible");
                        onComponentBecameVisible();
                    } else {
                        debugLogger.debug("Component became hidden");
                        onComponentBecameHidden();
                    }
                }
            }
        });
        
        // Monitor component resize/move events (can indicate wake from sleep)
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                debugLogger.debug("Component shown event");
                onComponentBecameVisible();
            }
            
            @Override
            public void componentHidden(ComponentEvent e) {
                debugLogger.debug("Component hidden event");
                onComponentBecameHidden();
            }
            
            @Override
            public void componentResized(ComponentEvent e) {
                debugLogger.debug("Component resized - potential wake event");
                checkForPotentialWakeEventWithDebounce("Component resized");
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {
                debugLogger.debug("Component moved - potential wake event");
                checkForPotentialWakeEventWithDebounce("Component moved");
            }
        });
        
        // Monitor focus changes
        component.addPropertyChangeListener("focusOwner", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null) {
                    debugLogger.debug("Component gained focus");
                    onComponentGainedFocus();
                }
            }
        });
        
        debugLogger.debug("Component listeners setup completed");
    }
    
    /**
     * Set up periodic monitoring to detect potential sleep events.
     */
    private void setupSleepDetectionMonitoring() {
        debugLogger.debug("Setting up sleep detection monitoring");
        
        // Monitor system time jumps that might indicate sleep/wake - reduced frequency
        monitoringTimer = new Timer(5000, e -> {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCheck = currentTime - lastActiveTime.get();
            
            // If more than our threshold has passed, we might have woken from sleep
            if (timeSinceLastCheck > SLEEP_DETECTION_THRESHOLD) {
                debugLogger.warn("Detected potential wake from sleep - time gap: {} ms", timeSinceLastCheck);
                onPotentialWakeFromSleep("Time gap detected: " + timeSinceLastCheck + "ms");
            }
            
            lastActiveTime.set(currentTime);
        });
        
        monitoringTimer.start();
        debugLogger.debug("Sleep detection monitoring started with {}ms interval", 5000);
    }
    
    /**
     * Called when component becomes visible after being hidden.
     */
    private void onComponentBecameVisible() {
        debugLogger.debug("WebView component became visible - checking for recovery needs");
        checkForPotentialWakeEvent("Component became visible");
    }
    
    /**
     * Called when component becomes hidden.
     */
    private void onComponentBecameHidden() {
        debugLogger.debug("WebView component became hidden - resetting detection counters");
        if (renderingDetector != null) {
            renderingDetector.resetDetectionCounters();
        }
    }
    
    /**
     * Called when component gains focus.
     */
    private void onComponentGainedFocus() {
        debugLogger.debug("WebView component gained focus - checking for recovery needs");
        checkForPotentialWakeEvent("Component gained focus");
    }
    
    /**
     * Check if this might be a wake event and trigger recovery if needed.
     * Made less aggressive to avoid unnecessary recoveries during normal UI operations.
     */
    private void checkForPotentialWakeEvent(String reason) {
        debugLogger.debug("Checking for potential wake event - reason: {}", reason);
        
        // Only trigger recovery if explicitly flagged as needed or if we have clear indicators of issues
        if (needsRecovery.get()) {
            debugLogger.debug("Recovery already flagged as needed");
            scheduleRecovery(reason);
        } else {
            // Be more conservative - only trigger recovery if we have actual issues
            boolean hasActualIssues = shouldAttemptRecoveryConservatively();
            if (hasActualIssues) {
                debugLogger.debug("Recovery determined to be needed based on actual detected issues");
                scheduleRecovery(reason);
            } else {
                debugLogger.debug("No recovery needed - component operations appear normal");
            }
        }
    }
    
    /**
     * Debounced version of checkForPotentialWakeEvent for component move/resize events.
     * This prevents excessive recovery attempts during normal UI operations like resizing.
     */
    private void checkForPotentialWakeEventWithDebounce(String reason) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastEvent = currentTime - lastComponentEventTime.get();
        
        // Update the last event time
        lastComponentEventTime.set(currentTime);
        
        // If this is happening too frequently, it's likely normal UI operations, not a wake event
        if (timeSinceLastEvent < COMPONENT_EVENT_DEBOUNCE_MS) {
            debugLogger.debug("Component event debounced - too frequent ({}ms since last), likely normal UI operation: {}", 
                             timeSinceLastEvent, reason);
            return;
        }
        
        debugLogger.debug("Component event passed debounce check - proceeding with wake event check: {}", reason);
        checkForPotentialWakeEvent(reason);
    }
    
    /**
     * Conservative recovery check - only triggers recovery for actual detected issues,
     * not just component movements or normal UI operations.
     */
    private boolean shouldAttemptRecoveryConservatively() {
        debugLogger.debug("Evaluating if conservative recovery should be attempted");
        
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.debug("JCEF not available or browser is null - no recovery needed");
            return false;
        }
        
        try {
            JComponent component = browser.getComponent();
            if (component == null || !component.isDisplayable()) {
                debugLogger.debug("Component is null or not displayable - no recovery needed");
                return false;
            }
            
            // Only check for actual issues, not potential ones
            boolean browserStateIssue = browserStateMonitor != null && browserStateMonitor.hasIssues();
            boolean renderingIssue = renderingDetector != null && renderingDetector.hasIssues();
            
            // Additional conservative checks - only recover if we have strong indicators of problems
            boolean hasStrongIndicators = false;
            
            // Check if browser is showing but has suspicious size (could indicate black rectangle)
            if (component.isShowing() && (component.getWidth() <= 0 || component.getHeight() <= 0)) {
                debugLogger.debug("Component showing but has invalid size - strong indicator of issues");
                hasStrongIndicators = true;
            }
            
            // Check if component background is black (potential black rectangle)
            Color backgroundColor = component.getBackground();
            if (backgroundColor != null && backgroundColor.equals(Color.BLACK) && component.isShowing()) {
                debugLogger.debug("Component has black background while showing - potential black rectangle");
                hasStrongIndicators = true;
            }
            
            boolean shouldRecover = browserStateIssue || renderingIssue || hasStrongIndicators;
            
            debugLogger.debug("Conservative recovery evaluation - browserStateIssue: {}, renderingIssue: {}, strongIndicators: {}, shouldRecover: {}", 
                             browserStateIssue, renderingIssue, hasStrongIndicators, shouldRecover);
            
            return shouldRecover;
            
        } catch (Exception e) {
            debugLogger.error("Exception during conservative recovery check - being cautious, not recovering", e);
            return false; // Be conservative - if we can't check properly, don't recover
        }
    }
    
    /**
     * Called when we detect a potential wake from sleep event.
     */
    private void onPotentialWakeFromSleep(String reason) {
        debugLogger.info("Potential wake from sleep detected: {}", reason);
        needsRecovery.set(true);
        scheduleRecovery(reason);
    }
    
    /**
     * Callback from specialized handlers when recovery is needed.
     */
    private void onRecoveryNeeded(String reason) {
        debugLogger.info("Recovery callback triggered: {}", reason);
        needsRecovery.set(true);
        scheduleRecovery(reason);
    }
    
    /**
     * Determine if we should attempt recovery based on various factors.
     */
    private boolean shouldAttemptRecovery() {
        debugLogger.debug("Evaluating if recovery should be attempted");
        
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.debug("JCEF not available or browser is null - no recovery needed");
            return false;
        }
        
        try {
            JComponent component = browser.getComponent();
            if (component == null || !component.isDisplayable()) {
                debugLogger.debug("Component is null or not displayable - no recovery needed");
                return false;
            }
            
            // Check with specialized handlers
            boolean browserStateIssue = browserStateMonitor != null && browserStateMonitor.hasIssues();
            boolean renderingIssue = renderingDetector != null && renderingDetector.hasIssues();
            
            debugLogger.debug("Recovery evaluation - needsRecovery: {}, browserStateIssue: {}, renderingIssue: {}", 
                             needsRecovery.get(), browserStateIssue, renderingIssue);
            
            return needsRecovery.get() || browserStateIssue || renderingIssue;
            
        } catch (Exception e) {
            debugLogger.error("Exception while checking browser state - assuming recovery needed", e);
            return true; // If we can't check state, better to attempt recovery
        }
    }
    
    /**
     * Schedule a recovery attempt with reason logging.
     */
    private void scheduleRecovery(String reason) {
        if (recoveryInProgress.get()) {
            debugLogger.debug("Recovery already in progress, skipping (reason: {})", reason);
            return;
        }
        
        debugLogger.info("Scheduling recovery - reason: {}", reason);
        
        // Cancel any existing recovery timer
        if (recoveryTimer != null) {
            recoveryTimer.stop();
            debugLogger.debug("Cancelled existing recovery timer");
        }
        
        // Schedule recovery with a delay to avoid immediate recovery after wake
        recoveryTimer = new Timer(3000, e -> attemptRecovery(reason));
        recoveryTimer.setRepeats(false);
        recoveryTimer.start();
        
        debugLogger.info("Recovery scheduled in 3 seconds (reason: {})", reason);
    }
    
    /**
     * Attempt to recover the browser from a potentially corrupted state.
     */
    private void attemptRecovery(String reason) {
        if (!recoveryInProgress.compareAndSet(false, true)) {
            debugLogger.debug("Recovery already in progress, skipping");
            return;
        }
        
        debugLogger.info("Starting recovery attempt (reason: {})", reason);
        
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (!JCEFChecker.isJCEFAvailable() || browser == null) {
                    debugLogger.warn("Cannot perform recovery - JCEF not available or browser is null");
                    return;
                }
                
                // Delegate to recovery strategies
                boolean recovered = recoveryStrategies.attemptRecovery(reason);
                
                if (recovered) {
                    debugLogger.info("Browser recovery successful (reason: {})", reason);
                    needsRecovery.set(false);
                    if (renderingDetector != null) {
                        renderingDetector.resetDetectionCounters();
                    }
                } else {
                    debugLogger.warn("Browser recovery failed (reason: {}), may need IDE restart", reason);
                }
                
            } catch (Exception e) {
                debugLogger.error("Error during browser recovery attempt", e);
            } finally {
                recoveryInProgress.set(false);
                debugLogger.debug("Recovery process completed, lock released");
            }
        });
    }
    
    /**
     * Manually trigger recovery (can be called externally).
     */
    public void triggerRecovery() {
        debugLogger.info("Manual recovery triggered");
        needsRecovery.set(true);
        scheduleRecovery("Manual trigger");
    }
    
    /**
     * Clean up resources when the handler is no longer needed.
     */
    public void dispose() {
        debugLogger.info("Disposing sleep/wake recovery handler");
        
        isMonitoring.set(false);
        
        if (monitoringTimer != null) {
            monitoringTimer.stop();
            monitoringTimer = null;
            debugLogger.debug("Monitoring timer stopped");
        }
        
        if (recoveryTimer != null) {
            recoveryTimer.stop();
            recoveryTimer = null;
            debugLogger.debug("Recovery timer stopped");
        }
        
        if (browserStateMonitor != null) {
            browserStateMonitor.dispose();
            debugLogger.debug("Browser state monitor disposed");
        }
        
        if (renderingDetector != null) {
            renderingDetector.dispose();
            debugLogger.debug("Rendering detector disposed");
        }
        
        if (recoveryStrategies != null) {
            recoveryStrategies.dispose();
            debugLogger.debug("Recovery strategies disposed");
        }
        
        debugLogger.info("Sleep/wake recovery handler disposed successfully");
    }
}
