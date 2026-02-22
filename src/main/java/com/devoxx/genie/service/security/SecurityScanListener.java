package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.SecurityScanResult;

/**
 * Callback interface for security scan progress updates.
 * All methods are invoked on EDT.
 */
public interface SecurityScanListener {
    default void onScanStarted() {}
    default void onScannerStarted(String name, int index, int total) {}
    default void onScannerCompleted(String name, int findingsCount) {}
    default void onScannerSkipped(String name, String reason) {}
    default void onTasksCreated(int count) {}
    default void onScanCompleted(SecurityScanResult result) {}
    default void onScanFailed(String error) {}
}
