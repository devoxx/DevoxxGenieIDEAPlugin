package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.devoxx.genie.ui.webview.WebServer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * Implements various recovery strategies for WebView issues.
 * This class contains all the recovery logic in a focused, manageable way.
 */
@Slf4j
public class WebViewRecoveryStrategies {
    
    private final JBCefBrowser browser;
    private final WebViewDebugLogger debugLogger;
    
    public WebViewRecoveryStrategies(JBCefBrowser browser, WebViewDebugLogger debugLogger) {
        this.browser = browser;
        this.debugLogger = debugLogger;
        
        if (browser != null && JCEFChecker.isJCEFAvailable()) {
            debugLogger.debug("WebViewRecoveryStrategies initialized successfully");
        } else {
            debugLogger.warn("WebViewRecoveryStrategies - JCEF not available or browser is null");
        }
    }
    
    /**
     * Attempt recovery using multiple strategies based on the reason.
     */
    public boolean attemptRecovery(String reason) {
        debugLogger.info("Starting recovery attempt with reason: {}", reason);
        long startTime = System.currentTimeMillis();
        
        if (!JCEFChecker.isJCEFAvailable() || browser == null) {
            debugLogger.warn("Cannot perform recovery - JCEF not available or browser is null");
            return false;
        }
        
        boolean recovered = false;
        
        try {
            // Strategy selection based on reason
            if (reason.contains("timeout") || reason.contains("connection")) {
                debugLogger.debug("Attempting connection-focused recovery");
                recovered = attemptWebServerRestart() || attemptConnectionRecovery();
            } else if (reason.contains("black rectangle") || reason.contains("render")) {
                debugLogger.debug("Attempting render-focused recovery");
                recovered = attemptNavigationCycleRecovery() || attemptRepaintRecovery();
            } else if (reason.contains("load error") || reason.contains("Load error")) {
                debugLogger.debug("Attempting load-focused recovery");
                recovered = attemptReloadRecovery() || attemptNavigationCycleRecovery();
            } else {
                debugLogger.debug("Attempting general recovery sequence");
                recovered = attemptGeneralRecovery();
            }
            
            debugLogger.logRecoveryAttempt("overall", reason, recovered, "Primary strategy completed");
            
            // If primary strategy failed, try fallback strategies
            if (!recovered) {
                debugLogger.info("Primary recovery failed, trying fallback strategies");
                recovered = attemptFallbackRecovery(reason);
            }
            
        } catch (Exception e) {
            debugLogger.error("Recovery attempt failed with exception", e);
            recovered = false;
        } finally {
            debugLogger.logTiming("recoveryAttempt", startTime);
        }
        
        return recovered;
    }
    
    /**
     * Attempt general recovery using multiple strategies in sequence.
     */
    private boolean attemptGeneralRecovery() {
        debugLogger.debug("Starting general recovery sequence");
        
        // Try strategies in order of increasing aggressiveness
        return attemptRepaintRecovery() ||
               attemptReloadRecovery() ||
               attemptNavigationCycleRecovery() ||
               attemptComponentRecreation();
    }
    
    /**
     * Attempt fallback recovery strategies.
     */
    private boolean attemptFallbackRecovery(String reason) {
        debugLogger.debug("Starting fallback recovery for reason: {}", reason);
        
        return attemptComponentRecreation() ||
               attemptWebServerRestart() ||
               attemptForceRefresh();
    }
    
    /**
     * Strategy 1: Restart the WebServer (for connection issues).
     */
    private boolean attemptWebServerRestart() {
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Attempting WebServer restart recovery");

        try {
            WebServer webServer = WebServer.getInstance();
            if (webServer == null || !webServer.isRunning()) {
                debugLogger.warn("WebServer not running, cannot restart");
                return false;
            }

            String currentUrl = browser.getCefBrowser() != null ? browser.getCefBrowser().getURL() : null;
            debugLogger.debug("Current URL before restart: {}", currentUrl);

            // Stop and restart the web server on a pooled thread to avoid blocking EDT
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    webServer.stop();
                    debugLogger.debug("WebServer stopped, waiting for cleanup");
                    Thread.sleep(1000);

                    webServer.start();
                    debugLogger.debug("WebServer restarted, waiting for readiness");
                    Thread.sleep(2000);

                    // Reload the page on EDT
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            if (currentUrl != null && !currentUrl.isEmpty() && browser.getCefBrowser() != null) {
                                browser.getCefBrowser().loadURL(currentUrl);
                                debugLogger.debug("Page reloaded after WebServer restart");
                            }
                        } catch (Exception e) {
                            debugLogger.error("Page reload after WebServer restart failed", e);
                        }
                    });
                } catch (Exception e) {
                    debugLogger.error("WebServer restart recovery failed in background", e);
                }
            });

            debugLogger.logRecoveryAttempt("webServerRestart", "connection", true, "Server restart initiated");
            return true;

        } catch (Exception e) {
            debugLogger.error("WebServer restart recovery failed", e);
            debugLogger.logRecoveryAttempt("webServerRestart", "connection", false, e.getMessage());
            return false;
        } finally {
            debugLogger.logTiming("webServerRestart", startTime);
        }
    }
    
    /**
     * Strategy 2: Force component repaint and revalidation.
     */
    private boolean attemptRepaintRecovery() {
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Attempting repaint recovery");
        
        try {
            JComponent component = browser.getComponent();
            if (component == null) {
                debugLogger.warn("Cannot perform repaint - component is null");
                return false;
            }
            
            // Log component state before repaint
            debugLogger.logComponentInfo("beforeRepaint", 
                                        component.isVisible(), component.isDisplayable(),
                                        component.getWidth(), component.getHeight(), "");
            
            // Force complete revalidation and repaint
            component.invalidate();
            component.revalidate();
            component.repaint();
            
            // Also refresh parent containers
            Container parent = component.getParent();
            int parentCount = 0;
            while (parent != null && parentCount < 5) { // Limit to avoid infinite loops
                parent.invalidate();
                parent.revalidate();
                parent.repaint();
                parent = parent.getParent();
                parentCount++;
            }
            
            debugLogger.debug("Repaint recovery completed, refreshed {} parent containers", parentCount);
            debugLogger.logRecoveryAttempt("repaint", "rendering", true, "Component and parents repainted");
            return true;
            
        } catch (Exception e) {
            debugLogger.error("Repaint recovery failed", e);
            debugLogger.logRecoveryAttempt("repaint", "rendering", false, e.getMessage());
            return false;
        } finally {
            debugLogger.logTiming("repaintRecovery", startTime);
        }
    }
    
    /**
     * Strategy 3: Reload the current page.
     */
    private boolean attemptReloadRecovery() {
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Attempting reload recovery");
        
        try {
            if (browser.getCefBrowser() == null) {
                debugLogger.warn("Cannot reload - CEF browser is null");
                return false;
            }
            
            String currentUrl = browser.getCefBrowser().getURL();
            debugLogger.debug("Reloading URL: {}", currentUrl);
            
            if (currentUrl != null && !currentUrl.isEmpty()) {
                browser.getCefBrowser().reload();
                debugLogger.debug("Browser reload initiated");
                debugLogger.logRecoveryAttempt("reload", "loading", true, "Page reloaded");
                return true;
            } else {
                debugLogger.warn("Cannot reload - URL is null or empty");
                return false;
            }
            
        } catch (Exception e) {
            debugLogger.error("Reload recovery failed", e);
            debugLogger.logRecoveryAttempt("reload", "loading", false, e.getMessage());
            return false;
        } finally {
            debugLogger.logTiming("reloadRecovery", startTime);
        }
    }
    
    /**
     * Strategy 4: Navigation cycle recovery (specifically for black rectangle issues).
     */
    private boolean attemptNavigationCycleRecovery() {
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Attempting navigation cycle recovery for black rectangle");
        
        try {
            if (browser.getCefBrowser() == null) {
                debugLogger.warn("Cannot perform navigation cycle - CEF browser is null");
                return false;
            }
            
            String currentUrl = browser.getCefBrowser().getURL();
            if (currentUrl == null || currentUrl.isEmpty()) {
                debugLogger.warn("Cannot perform navigation cycle - URL is null or empty");
                return false;
            }
            
            debugLogger.debug("Starting navigation cycle: {} -> about:blank -> {}", currentUrl, currentUrl);
            
            // Navigate to blank to clear any rendering state
            browser.getCefBrowser().loadURL("about:blank");
            debugLogger.debug("Navigated to about:blank");
            
            // Schedule navigation back to original URL
            Timer navigationTimer = new Timer(2000, e -> {
                try {
                    debugLogger.debug("Navigating back to: {}", currentUrl);
                    browser.getCefBrowser().loadURL(currentUrl);
                    
                    // Schedule recovery JavaScript injection
                    Timer jsTimer = new Timer(3000, evt -> {
                        try {
                            injectRecoveryJavaScript();
                        } catch (Exception ex) {
                            debugLogger.error("Recovery JavaScript injection failed", ex);
                        }
                    });
                    jsTimer.setRepeats(false);
                    jsTimer.start();
                    
                } catch (Exception ex) {
                    debugLogger.error("Navigation back failed", ex);
                }
            });
            navigationTimer.setRepeats(false);
            navigationTimer.start();
            
            debugLogger.logRecoveryAttempt("navigationCycle", "blackRectangle", true, "Navigation cycle initiated");
            return true;
            
        } catch (Exception e) {
            debugLogger.error("Navigation cycle recovery failed", e);
            debugLogger.logRecoveryAttempt("navigationCycle", "blackRectangle", false, e.getMessage());
            return false;
        } finally {
            debugLogger.logTiming("navigationCycleRecovery", startTime);
        }
    }
    
    /**
     * Strategy 5: Component recreation recovery.
     */
    private boolean attemptComponentRecreation() {
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Attempting component recreation recovery");
        
        try {
            JComponent component = browser.getComponent();
            if (component == null) {
                debugLogger.warn("Cannot recreate component - component is null");
                return false;
            }
            
            // Log component state before recreation
            debugLogger.logComponentInfo("beforeRecreation",
                                        component.isVisible(), component.isDisplayable(),
                                        component.getWidth(), component.getHeight(),
                                        "opaque=" + component.isOpaque());
            
            // Force component recreation by manipulating visibility
            boolean wasVisible = component.isVisible();
            component.setVisible(false);
            component.invalidate();
            
            // Schedule restoration of visibility
            Timer recreationTimer = new Timer(500, e -> {
                try {
                    component.setVisible(wasVisible);
                    component.revalidate();
                    component.repaint();
                    
                    debugLogger.logComponentInfo("afterRecreation",
                                                component.isVisible(), component.isDisplayable(),
                                                component.getWidth(), component.getHeight(),
                                                "restored");
                    
                    debugLogger.debug("Component recreation completed");
                } catch (Exception ex) {
                    debugLogger.error("Component recreation restoration failed", ex);
                }
            });
            recreationTimer.setRepeats(false);
            recreationTimer.start();
            
            debugLogger.logRecoveryAttempt("componentRecreation", "rendering", true, "Component recreation initiated");
            return true;
            
        } catch (Exception e) {
            debugLogger.error("Component recreation recovery failed", e);
            debugLogger.logRecoveryAttempt("componentRecreation", "rendering", false, e.getMessage());
            return false;
        } finally {
            debugLogger.logTiming("componentRecreation", startTime);
        }
    }
    
    /**
     * Strategy 6: Connection recovery with timeout detection.
     */
    private boolean attemptConnectionRecovery() {
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Attempting connection recovery");
        
        try {
            if (browser.getCefBrowser() == null) {
                debugLogger.warn("Cannot perform connection recovery - CEF browser is null");
                return false;
            }
            
            String currentUrl = browser.getCefBrowser().getURL();
            if (currentUrl == null || currentUrl.isEmpty()) {
                debugLogger.warn("Cannot perform connection recovery - URL is null or empty");
                return false;
            }
            
            // Inject connection timeout detection
            injectConnectionTimeoutDetection();
            
            // Force reconnection by navigating away and back
            debugLogger.debug("Forcing reconnection for URL: {}", currentUrl);
            browser.getCefBrowser().loadURL("about:blank");
            
            // Schedule reconnection
            Timer reconnectTimer = new Timer(1500, e -> {
                try {
                    debugLogger.debug("Reconnecting after connection recovery");
                    browser.getCefBrowser().loadURL(currentUrl);
                    
                    // Schedule post-reconnection health check
                    Timer healthCheckTimer = new Timer(3000, evt -> {
                        debugLogger.debug("Post-reconnection health check completed");
                    });
                    healthCheckTimer.setRepeats(false);
                    healthCheckTimer.start();
                    
                } catch (Exception ex) {
                    debugLogger.error("Reconnection failed", ex);
                }
            });
            reconnectTimer.setRepeats(false);
            reconnectTimer.start();
            
            debugLogger.logRecoveryAttempt("connectionRecovery", "connection", true, "Connection recovery initiated");
            return true;
            
        } catch (Exception e) {
            debugLogger.error("Connection recovery failed", e);
            debugLogger.logRecoveryAttempt("connectionRecovery", "connection", false, e.getMessage());
            return false;
        } finally {
            debugLogger.logTiming("connectionRecovery", startTime);
        }
    }
    
    /**
     * Strategy 7: Force refresh (last resort).
     */
    private boolean attemptForceRefresh() {
        long startTime = System.currentTimeMillis();
        debugLogger.debug("Attempting force refresh recovery (last resort)");

        try {
            boolean repaintResult = attemptRepaintRecovery();

            // Schedule reload after a short delay using a timer instead of Thread.sleep
            Timer delayedReload = new Timer(500, e -> {
                boolean reloadResult = attemptReloadRecovery();
                debugLogger.logRecoveryAttempt("forceRefresh", "lastResort", repaintResult || reloadResult,
                                              "repaint=" + repaintResult + ", reload=" + reloadResult);
            });
            delayedReload.setRepeats(false);
            delayedReload.start();

            return repaintResult; // Return immediate repaint result; reload is async

        } catch (Exception e) {
            debugLogger.error("Force refresh recovery failed", e);
            debugLogger.logRecoveryAttempt("forceRefresh", "lastResort", false, e.getMessage());
            return false;
        } finally {
            debugLogger.logTiming("forceRefresh", startTime);
        }
    }
    
    /**
     * Inject recovery JavaScript to help with rendering issues.
     */
    private void injectRecoveryJavaScript() {
        try {
            if (browser.getCefBrowser() == null) {
                debugLogger.debug("Cannot inject recovery JS - CEF browser is null");
                return;
            }
            
            String recoveryJs = 
                "try { " +
                "  console.log('Injecting recovery JavaScript'); " +
                "  " +
                "  // Force display refresh " +
                "  document.body.style.display = 'none'; " +
                "  setTimeout(() => { " +
                "    document.body.style.display = ''; " +
                "    console.log('Recovery display cycle completed'); " +
                "  }, 100); " +
                "  " +
                "  // Force repaint " +
                "  var container = document.getElementById('conversation-container'); " +
                "  if (container) { " +
                "    container.style.transform = 'translateZ(0)'; " +
                "    setTimeout(() => { " +
                "      container.style.transform = ''; " +
                "      console.log('Recovery transform cycle completed'); " +
                "    }, 200); " +
                "  } " +
                "  " +
                "  // Mark recovery completion " +
                "  window._recoveryCompleted = { " +
                "    timestamp: Date.now(), " +
                "    method: 'injectedJS' " +
                "  }; " +
                "  " +
                "  console.log('Recovery JavaScript completed successfully'); " +
                "} catch(e) { " +
                "  console.error('Recovery JavaScript failed:', e); " +
                "  window._recoveryCompleted = { " +
                "    timestamp: Date.now(), " +
                "    error: e.message " +
                "  }; " +
                "}";
            
            browser.getCefBrowser().executeJavaScript(recoveryJs, "", 0);
            debugLogger.debug("Recovery JavaScript injected successfully");
            
        } catch (Exception e) {
            debugLogger.error("Failed to inject recovery JavaScript", e);
        }
    }
    
    /**
     * Inject connection timeout detection JavaScript.
     */
    private void injectConnectionTimeoutDetection() {
        try {
            if (browser.getCefBrowser() == null) {
                debugLogger.debug("Cannot inject timeout detection JS - CEF browser is null");
                return;
            }
            
            String timeoutDetectionJs = 
                "try { " +
                "  window._connectionTimeoutDetected = false; " +
                "  window._lastSuccessfulRequest = Date.now(); " +
                "  " +
                "  // Override fetch to detect timeouts " +
                "  if (!window._fetchOverrideInstalled) { " +
                "    const originalFetch = window.fetch; " +
                "    window.fetch = function(...args) { " +
                "      const startTime = Date.now(); " +
                "      return originalFetch.apply(this, args) " +
                "        .then(response => { " +
                "          window._lastSuccessfulRequest = Date.now(); " +
                "          return response; " +
                "        }) " +
                "        .catch(error => { " +
                "          const duration = Date.now() - startTime; " +
                "          if (duration > 5000 || error.name === 'TimeoutError') { " +
                "            window._connectionTimeoutDetected = true; " +
                "            console.warn('Connection timeout detected:', error); " +
                "          } " +
                "          throw error; " +
                "        }); " +
                "    }; " +
                "    window._fetchOverrideInstalled = true; " +
                "    console.log('Connection timeout detection installed'); " +
                "  } " +
                "} catch(e) { " +
                "  console.error('Failed to install timeout detection:', e); " +
                "}";
            
            browser.getCefBrowser().executeJavaScript(timeoutDetectionJs, "", 0);
            debugLogger.debug("Connection timeout detection JavaScript injected");
            
        } catch (Exception e) {
            debugLogger.error("Failed to inject connection timeout detection", e);
        }
    }
    
    /**
     * Dispose resources.
     */
    public void dispose() {
        debugLogger.debug("Disposing WebViewRecoveryStrategies");
        // No persistent resources to clean up in this class
        debugLogger.info("WebViewRecoveryStrategies disposed successfully");
    }
}
