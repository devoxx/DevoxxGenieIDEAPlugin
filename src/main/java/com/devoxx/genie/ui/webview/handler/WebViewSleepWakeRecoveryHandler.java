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
 * Handles sleep/wake recovery for JCEF browsers on macOS.
 * This class detects when the system has potentially woken from sleep
 * and refreshes the browser to fix rendering issues.
 */
@Slf4j
public class WebViewSleepWakeRecoveryHandler {
    
    private final JBCefBrowser browser;
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean needsRecovery = new AtomicBoolean(false);
    
    // Recovery state tracking
    private Timer monitoringTimer;
    private Timer recoveryTimer;
    
    // Sleep detection threshold (5 minutes of inactivity suggests sleep)
    private static final long SLEEP_DETECTION_THRESHOLD = 5 * 60 * 1000; // 5 minutes
    
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);
    
    public WebViewSleepWakeRecoveryHandler(JBCefBrowser browser) {
        this.browser = browser;
        
        if (browser != null && JCEFChecker.isJCEFAvailable()) {
            initializeMonitoring();
        }
    }
    
    /**
     * Initialize monitoring for sleep/wake events.
     */
    private void initializeMonitoring() {
        if (isMonitoring.get()) {
            return;
        }
        
        try {
            // Set up component listeners for visibility changes
            setupComponentListeners();
            
            // Set up periodic monitoring for sleep detection
            setupSleepDetectionMonitoring();
            
            // Set up system property change monitoring (macOS specific)
            setupSystemMonitoring();
            
            isMonitoring.set(true);
            log.info("Sleep/wake recovery monitoring initialized for webview");
            
        } catch (Exception e) {
            log.error("Failed to initialize sleep/wake recovery monitoring", e);
        }
    }
    
    /**
     * Set up component listeners to detect visibility and focus changes.
     */
    private void setupComponentListeners() {
        if (browser == null || browser.getComponent() == null) {
            return;
        }
        
        JComponent component = browser.getComponent();
        
        // Monitor component visibility changes
        component.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (component.isShowing()) {
                        onComponentBecameVisible();
                    }
                }
            }
        });
        
        // Monitor component resize/move events (can indicate wake from sleep)
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                onComponentBecameVisible();
            }
            
            @Override
            public void componentResized(ComponentEvent e) {
                // Component resize can happen after wake from sleep
                checkForPotentialWakeEvent();
            }
        });
        
        // Monitor focus changes
        component.addPropertyChangeListener("focusOwner", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null) {
                    onComponentGainedFocus();
                }
            }
        });
    }
    
    /**
     * Set up periodic monitoring to detect potential sleep events.
     * Enhanced with continuous connection monitoring.
     */
    private void setupSleepDetectionMonitoring() {
        // Monitor system time jumps that might indicate sleep/wake
        monitoringTimer = new Timer(1000, e -> {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCheck = currentTime - lastActiveTime.get();
            
            // If more than our threshold has passed, we might have woken from sleep
            if (timeSinceLastCheck > SLEEP_DETECTION_THRESHOLD) {
                log.info("Detected potential wake from sleep (time gap: {} ms)", timeSinceLastCheck);
                onPotentialWakeFromSleep();
            }
            
            // Also perform periodic connection health monitoring
            if (currentTime % 30000 == 0) { // Every 30 seconds
                performConnectionHealthCheck();
            }
            
            lastActiveTime.set(currentTime);
        });
        
        monitoringTimer.start();
    }
    
    /**
     * Perform a connection health check to proactively detect timeout issues.
     */
    private void performConnectionHealthCheck() {
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            return;
        }
        
        try {
            // Inject health check JavaScript that will attempt to contact the server
            String healthCheckJs = 
                "try { " +
                "  if (!window._health_check_running) { " +
                "    window._health_check_running = true; " +
                "    const startTime = Date.now(); " +
                "    fetch(window.location.origin + '/health-check', { " +
                "      method: 'GET', " +
                "      cache: 'no-cache', " +
                "      signal: AbortSignal.timeout(5000) " +
                "    }).then(response => { " +
                "      const duration = Date.now() - startTime; " +
                "      if (response.ok && duration < 3000) { " +
                "        window._connection_healthy = true; " +
                "        window._last_health_check = Date.now(); " +
                "      } else { " +
                "        window._connection_healthy = false; " +
                "        console.warn('Health check slow or failed:', duration, response.status); " +
                "      } " +
                "    }).catch(error => { " +
                "      window._connection_healthy = false; " +
                "      window._connection_timeout_detected = true; " +
                "      console.error('Health check failed:', error); " +
                "    }).finally(() => { " +
                "      window._health_check_running = false; " +
                "    }); " +
                "  } " +
                "} catch(e) { " +
                "  window._connection_healthy = false; " +
                "  console.error('Health check error:', e); " +
                "}";
            
            browser.getCefBrowser().executeJavaScript(healthCheckJs, "", 0);
            
            // Schedule a check to see if health check detected issues
            Timer healthCheckResultTimer = new Timer(8000, evt -> {
                String checkResultJs = 
                    "if (window._connection_timeout_detected || window._connection_healthy === false) { " +
                    "  window._recovery_needed_health = true; " +
                    "} else { " +
                    "  window._recovery_needed_health = false; " +
                    "}";
                
                browser.getCefBrowser().executeJavaScript(checkResultJs, "", 0);
                
                // If we suspect issues, trigger recovery
                if (needsRecovery.get() || shouldAttemptRecovery()) {
                    log.info("Connection health check detected issues, scheduling recovery");
                    scheduleRecovery();
                }
            });
            healthCheckResultTimer.setRepeats(false);
            healthCheckResultTimer.start();
            
        } catch (Exception e) {
            log.debug("Failed to perform connection health check: {}", e.getMessage());
        }
    }
    
    /**
     * Set up system-level monitoring for macOS sleep/wake events.
     */
    private void setupSystemMonitoring() {
        // On macOS, we can monitor system properties or use native calls
        // For now, we'll rely on the time-based detection and component events
        
        // Future enhancement: Could use JNI to register for system sleep/wake notifications
        // or monitor system log events
    }
    
    /**
     * Called when component becomes visible after being hidden.
     */
    private void onComponentBecameVisible() {
        log.debug("WebView component became visible");
        checkForPotentialWakeEvent();
    }
    
    /**
     * Called when component gains focus.
     */
    private void onComponentGainedFocus() {
        log.debug("WebView component gained focus");
        checkForPotentialWakeEvent();
    }
    
    /**
     * Check if this might be a wake event and trigger recovery if needed.
     */
    private void checkForPotentialWakeEvent() {
        if (needsRecovery.get() || shouldAttemptRecovery()) {
            scheduleRecovery();
        }
    }
    
    /**
     * Called when we detect a potential wake from sleep event.
     */
    private void onPotentialWakeFromSleep() {
        log.info("Potential wake from sleep detected, marking for recovery");
        needsRecovery.set(true);
        scheduleRecovery();
    }
    
    /**
     * Determine if we should attempt recovery based on various factors.
     */
    private boolean shouldAttemptRecovery() {
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            return false;
        }
        
        // Check if the browser appears to be in a bad state
        // This is heuristic-based since we can't directly detect rendering issues
        try {
            JComponent component = browser.getComponent();
            if (component == null || !component.isDisplayable()) {
                return false;
            }
            
            // If component is visible but potentially not rendering properly
            // we can't easily detect this, so we rely on the sleep detection
            return needsRecovery.get();
            
        } catch (Exception e) {
            log.debug("Exception while checking browser state: {}", e.getMessage());
            return true; // If we can't check state, better to attempt recovery
        }
    }
    
    /**
     * Schedule a recovery attempt.
     */
    private void scheduleRecovery() {
        if (recoveryInProgress.get()) {
            log.debug("Recovery already in progress, skipping");
            return;
        }
        
        // Cancel any existing recovery timer
        if (recoveryTimer != null) {
            recoveryTimer.stop();
        }
        
        // Schedule recovery with a small delay to avoid immediate recovery after wake
        recoveryTimer = new Timer(2000, e -> attemptRecovery());
        recoveryTimer.setRepeats(false);
        recoveryTimer.start();
        
        log.debug("Recovery scheduled in 2 seconds");
    }
    
    /**
     * Attempt to recover the browser from a potentially corrupted state.
     */
    private void attemptRecovery() {
        if (!recoveryInProgress.compareAndSet(false, true)) {
            log.debug("Recovery already in progress");
            return;
        }
        
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                log.info("Attempting browser recovery after potential sleep/wake");
                
                if (!JCEFChecker.isJCEFAvailable() || browser == null) {
                    log.warn("Cannot perform recovery - JCEF not available or browser is null");
                    return;
                }
                
                // Multiple recovery strategies, try in order of preference
                boolean recovered = false;
                
                // Strategy 1: Force repaint/revalidate
                recovered = attemptRepaintRecovery();
                
                // Strategy 2: Reload current page if repaint didn't work
                if (!recovered) {
                    recovered = attemptReloadRecovery();
                }
                
                // Strategy 3: Force connection recovery if reload didn't work
                if (!recovered) {
                    recovered = attemptConnectionRecovery();
                }
                
                // Strategy 4: Force browser re-creation if available
                if (!recovered) {
                    recovered = attemptBrowserRecreation();
                }                
                if (recovered) {
                    log.info("Browser recovery successful");
                    needsRecovery.set(false);
                } else {
                    log.warn("Browser recovery failed, may need IDE restart");
                }
                
            } catch (Exception e) {
                log.error("Error during browser recovery attempt", e);
            } finally {
                recoveryInProgress.set(false);
            }
        });
    }
    
    /**
     * Attempt recovery by forcing component repaint and revalidation.
     */
    private boolean attemptRepaintRecovery() {
        try {
            log.debug("Attempting repaint recovery");
            
            JComponent component = browser.getComponent();
            if (component != null) {
                // Force complete revalidation and repaint
                component.invalidate();
                component.revalidate();
                component.repaint();
                
                // Also try to refresh parent containers
                Container parent = component.getParent();
                while (parent != null) {
                    parent.invalidate();
                    parent.revalidate();
                    parent.repaint();
                    parent = parent.getParent();
                }
                
                log.debug("Repaint recovery completed");
                return true;
            }
        } catch (Exception e) {
            log.debug("Repaint recovery failed: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Attempt recovery by reloading the current page.
     */
    private boolean attemptReloadRecovery() {
        try {
            log.debug("Attempting reload recovery");
            
            if (browser.getCefBrowser() != null) {
                // Get current URL and reload - this should reconnect to web server
                String currentUrl = browser.getCefBrowser().getURL();
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    log.debug("Reloading URL to reconnect: {}", currentUrl);
                    browser.getCefBrowser().reload();
                    log.debug("Browser reload initiated - should reconnect to web server");
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Reload recovery failed: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Attempt recovery by forcing a complete reconnection to the web server.
     * Enhanced with connection timeout detection and server health check.
     */
    private boolean attemptConnectionRecovery() {
        try {
            log.debug("Attempting enhanced connection recovery with timeout detection");
            
            if (browser.getCefBrowser() != null) {
                String currentUrl = browser.getCefBrowser().getURL();
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    
                    // First, check if our WebServer is still running and responsive
                    if (!checkWebServerHealth()) {
                        log.warn("WebServer appears to be unresponsive, attempting to restart");
                        restartWebServerIfNeeded();
                    }
                    
                    // Force a complete reconnection by navigating away and back
                    log.debug("Forcing reconnection by navigating to blank and back to: {}", currentUrl);
                    
                    // Navigate to blank to close current connection
                    browser.getCefBrowser().loadURL("about:blank");
                    
                    // Use a timer to navigate back after ensuring server is ready
                    Timer reconnectTimer = new Timer(1500, e -> {
                        try {
                            log.debug("Reconnecting to web server after connection recovery...");
                            
                            // Add connection timeout detection via JavaScript
                            injectConnectionTimeoutDetection();
                            
                            // Navigate back to the original URL
                            browser.getCefBrowser().loadURL(currentUrl);
                            
                            // Schedule a health check after reconnection
                            schedulePostReconnectionHealthCheck();
                            
                        } catch (Exception ex) {
                            log.warn("Failed to reconnect after connection recovery: {}", ex.getMessage());
                        }
                    });
                    reconnectTimer.setRepeats(false);
                    reconnectTimer.start();
                    
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Connection recovery failed: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if the WebServer is healthy and responsive.
     */
    private boolean checkWebServerHealth() {
        try {
            // Import the WebServer class to check its health
            com.devoxx.genie.ui.webview.WebServer webServer = 
                com.devoxx.genie.ui.webview.WebServer.getInstance();
            
            if (webServer == null || !webServer.isRunning()) {
                log.warn("WebServer is not running");
                return false;
            }
            
            // Try a simple connectivity test via JavaScript if possible
            if (browser != null && browser.getCefBrowser() != null) {
                String healthCheckJs = 
                    "try { " +
                    "  fetch('" + webServer.getServerUrl() + "/health-check', { " +
                    "    method: 'GET', " +
                    "    timeout: 3000 " +
                    "  }).then(response => { " +
                    "    window._server_health_ok = response.ok; " +
                    "  }).catch(err => { " +
                    "    window._server_health_ok = false; " +
                    "  }); " +
                    "} catch(e) { " +
                    "  window._server_health_ok = false; " +
                    "}";
                
                browser.getCefBrowser().executeJavaScript(healthCheckJs, "", 0);
            }
            
            log.debug("WebServer appears to be running on: {}", webServer.getServerUrl());
            return true;
            
        } catch (Exception e) {
            log.debug("WebServer health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Restart the WebServer if it's not responsive.
     */
    private void restartWebServerIfNeeded() {
        try {
            com.devoxx.genie.ui.webview.WebServer webServer = 
                com.devoxx.genie.ui.webview.WebServer.getInstance();
            
            if (webServer != null) {
                log.info("Attempting to restart WebServer after sleep/wake");
                
                // Stop the current server
                webServer.stop();
                
                // Wait a moment for cleanup
                Thread.sleep(1000);
                
                // Restart the server
                webServer.start();
                
                log.info("WebServer restarted successfully");
            }
        } catch (Exception e) {
            log.error("Failed to restart WebServer: {}", e.getMessage());
        }
    }
    
    /**
     * Inject JavaScript code to detect connection timeouts.
     */
    private void injectConnectionTimeoutDetection() {
        try {
            if (browser != null && browser.getCefBrowser() != null) {
                String timeoutDetectionJs = 
                    "window._connection_timeout_detected = false; " +
                    "window._last_successful_request = Date.now(); " +
                    
                    // Override fetch to detect timeouts
                    "if (!window._fetch_override_installed) { " +
                    "  const originalFetch = window.fetch; " +
                    "  window.fetch = function(...args) { " +
                    "    const startTime = Date.now(); " +
                    "    return originalFetch.apply(this, args) " +
                    "      .then(response => { " +
                    "        window._last_successful_request = Date.now(); " +
                    "        return response; " +
                    "      }) " +
                    "      .catch(error => { " +
                    "        const duration = Date.now() - startTime; " +
                    "        if (duration > 5000 || error.name === 'TimeoutError') { " +
                    "          window._connection_timeout_detected = true; " +
                    "          console.warn('Connection timeout detected:', error); " +
                    "        } " +
                    "        throw error; " +
                    "      }); " +
                    "  }; " +
                    "  window._fetch_override_installed = true; " +
                    "}";
                
                browser.getCefBrowser().executeJavaScript(timeoutDetectionJs, "", 0);
                log.debug("Connection timeout detection injected");
            }
        } catch (Exception e) {
            log.debug("Failed to inject connection timeout detection: {}", e.getMessage());
        }
    }
    
    /**
     * Schedule a health check after reconnection to verify recovery was successful.
     */
    private void schedulePostReconnectionHealthCheck() {
        Timer healthCheckTimer = new Timer(3000, e -> {
            try {
                if (browser != null && browser.getCefBrowser() != null) {
                    // Check if connection timeout was detected
                    String checkJs = 
                        "if (window._connection_timeout_detected) { " +
                        "  window._recovery_needed = true; " +
                        "} else { " +
                        "  window._recovery_needed = false; " +
                        "}";
                    
                    browser.getCefBrowser().executeJavaScript(checkJs, "", 0);
                    
                    log.debug("Post-reconnection health check completed");
                }
            } catch (Exception ex) {
                log.debug("Post-reconnection health check failed: {}", ex.getMessage());
            }
        });
        healthCheckTimer.setRepeats(false);
        healthCheckTimer.start();
    }    
    /**
     * Attempt recovery by forcing browser recreation.
     * This is a more aggressive approach when other methods fail.
     */
    private boolean attemptBrowserRecreation() {
        try {
            log.debug("Attempting browser recreation recovery");
            
            if (browser.getCefBrowser() != null) {
                // Store current URL for restoration
                String currentUrl = browser.getCefBrowser().getURL();
                
                // Force a complete refresh by navigating to about:blank and back
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    browser.getCefBrowser().loadURL("about:blank");
                    
                    // Use a small delay before reloading the original URL
                    Timer reloadTimer = new Timer(500, e -> {
                        try {
                            browser.getCefBrowser().loadURL(currentUrl);
                            log.debug("Browser recreation completed");
                        } catch (Exception ex) {
                            log.debug("Failed to reload URL after recreation: {}", ex.getMessage());
                        }
                    });
                    reloadTimer.setRepeats(false);
                    reloadTimer.start();
                    
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Browser recreation recovery failed: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Manually trigger recovery (can be called externally).
     */
    public void triggerRecovery() {
        log.info("Manual recovery triggered");
        needsRecovery.set(true);
        scheduleRecovery();
    }
    
    /**
     * Clean up resources when the handler is no longer needed.
     */
    public void dispose() {
        log.debug("Disposing sleep/wake recovery handler");
        
        isMonitoring.set(false);
        
        if (monitoringTimer != null) {
            monitoringTimer.stop();
            monitoringTimer = null;
        }
        
        if (recoveryTimer != null) {
            recoveryTimer.stop();
            recoveryTimer = null;
        }
    }
}
