package com.devoxx.genie.ui.webview;

import com.intellij.ui.jcef.JBCefApp;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to check if JCEF is available in the current environment.
 */
@Slf4j
public class JCEFChecker {

    private static Boolean cachedJcefAvailability = null;

    private JCEFChecker() {
        // Utility class, no instances needed
    }

    /**
     * Checks if JCEF is available in the current environment.
     * Uses cached result after first check to avoid repeated expensive operations.
     *
     * @return true if JCEF is available, false otherwise
     */
    public static boolean isJCEFAvailable() {
        // Return cached result if available
        if (cachedJcefAvailability != null) {
            return cachedJcefAvailability;
        }

        try {
            // First check if JCEF is supported in this environment without accessing getInstance
            boolean isSupported = JBCefApp.isSupported();
            if (!isSupported) {
                log.warn("JCEF is not supported in this environment");
                cachedJcefAvailability = false;
                return false;
            }
            
            try {
                // Try to get an instance to ensure it's initialized properly
                // This is done in a separate try-catch to handle specific JCEF initialization failures
                JBCefApp.getInstance();
                return true;
            } catch (UnsatisfiedLinkError | IllegalStateException e) {
                log.warn("JCEF is supported but failed to initialize: {}", e.getMessage());
                cachedJcefAvailability = false;
                return false;
            }
        } catch (Throwable e) {
            // Catch any other exceptions that might occur when checking JCEF availability
            log.warn("JCEF checking failed with exception: {}", e.getMessage());
            cachedJcefAvailability = false;
            return false;
        }
    }
}
