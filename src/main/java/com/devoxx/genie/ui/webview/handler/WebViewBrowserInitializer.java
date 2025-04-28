package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.util.ThreadUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles browser initialization and ensures callbacks are executed only when the browser is ready.
 */
public class WebViewBrowserInitializer {
    private static final Logger LOG = Logger.getInstance(WebViewBrowserInitializer.class);
    
    private final AtomicBoolean initialized;
    private final WebViewJavaScriptExecutor jsExecutor;
    
    public WebViewBrowserInitializer(AtomicBoolean initialized, WebViewJavaScriptExecutor jsExecutor) {
        this.initialized = initialized;
        this.jsExecutor = jsExecutor;
    }
    
    /**
     * Ensures the browser is initialized before executing a callback.
     * If already initialized, executes immediately, otherwise waits.
     */
    public void ensureBrowserInitialized(Runnable callback) {
        if (initialized.get() && jsExecutor.isLoaded()) {
            // Already initialized and loaded, execute immediately
            LOG.debug("Browser already initialized, executing callback immediately");
            callback.run();
        } else {
            // Wait for initialization in a background thread
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                LOG.info("Waiting for browser to initialize...");
                long startTime = System.currentTimeMillis();
                
                // Wait up to 15 seconds for initialization (increased timeout)
                while ((!initialized.get() || !jsExecutor.isLoaded()) && System.currentTimeMillis() - startTime < 15000) {
                    // Log status periodically
                    if (System.currentTimeMillis() - startTime > 5000 && System.currentTimeMillis() % 1000 < 100) {
                        LOG.info("Still waiting for browser... initialized=" + initialized.get() + ", loaded=" + jsExecutor.isLoaded());
                    }
                    ThreadUtils.sleep(100, "Interrupted while waiting for browser to initialize");
                    if (Thread.currentThread().isInterrupted()) {
                        LOG.error("Interrupted while waiting for browser to initialize");
                        return;
                    }
                }
                
                if (initialized.get() && jsExecutor.isLoaded()) {
                    LOG.info("Browser fully initialized, executing callback");
                    // Add a small delay to ensure the DOM is ready
                    ThreadUtils.sleep(200);
                    
                    // Make sure we're running on the UI thread
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            callback.run();
                        } catch (Exception e) {
                            LOG.error("Error executing browser callback: " + e.getMessage());
                        }
                    });
                } else {
                    LOG.error("Browser failed to initialize within timeout (initialized=" + initialized.get() + " loaded=" + jsExecutor.isLoaded());
                }
            });
        }
    }
}