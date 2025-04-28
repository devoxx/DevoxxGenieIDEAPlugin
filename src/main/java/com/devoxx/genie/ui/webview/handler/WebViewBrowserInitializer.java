package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.util.ThreadUtils;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles browser initialization and ensures callbacks are executed only when the browser is ready.
 */
@Slf4j
public class WebViewBrowserInitializer {

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
            log.debug("Browser already initialized, executing callback immediately");
            callback.run();
        } else {
            // Wait for initialization in a background thread
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                log.info("Waiting for browser to initialize...");
                long startTime = System.currentTimeMillis();
                
                // Wait up to 15 seconds for initialization (increased timeout)
                while ((!initialized.get() || !jsExecutor.isLoaded()) && System.currentTimeMillis() - startTime < 15000) {
                    // Log status periodically
                    if (System.currentTimeMillis() - startTime > 5000 && System.currentTimeMillis() % 1000 < 100) {
                        log.info("Still waiting for browser... initialized=" + initialized.get() + ", loaded=" + jsExecutor.isLoaded());
                    }
                    ThreadUtils.sleep(100, "Interrupted while waiting for browser to initialize");
                    if (Thread.currentThread().isInterrupted()) {
                        log.error("Interrupted while waiting for browser to initialize");
                        return;
                    }
                }
                
                if (initialized.get() && jsExecutor.isLoaded()) {
                    log.info("Browser fully initialized, executing callback");
                    // Add a small delay to ensure the DOM is ready
                    ThreadUtils.sleep(200);
                    
                    // Make sure we're running on the UI thread
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            callback.run();
                        } catch (Exception e) {
                            log.error("Error executing browser callback: " + e.getMessage());
                        }
                    });
                } else {
                    log.error("Browser failed to initialize within timeout (initialized=" + initialized.get() + " loaded=" + jsExecutor.isLoaded());
                }
            });
        }
    }
}