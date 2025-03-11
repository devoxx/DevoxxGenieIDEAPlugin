package com.devoxx.genie.service.analyzer.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scans project directories while respecting .gitignore rules and using caching.
 * Uses a controlled thread pool and timeout mechanisms to prevent hangs.
 */
public class CachedProjectScanner {
    private static final Logger LOG = Logger.getInstance(CachedProjectScanner.class);
    
    // Scanner constraints
    private static final int MAX_DEPTH = 50; // Reduced max depth to prevent excessive recursion
    private static final long SCAN_TIMEOUT_MS = 20000; // 20 seconds timeout - lower to prevent IDE freezing
    private static final int MAX_FILES = 10000; // Reduced maximum to improve responsiveness
    private static final int MAX_DIRECTORIES = 1000; // Maximum number of directories to scan
    
    // Thread pool for scanning - use a reasonable fixed size to prevent resource exhaustion
    private static final ExecutorService SCAN_EXECUTOR = 
            Executors.newFixedThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
                    r -> {
                        Thread t = new Thread(r, "ProjectScanner-Worker");
                        t.setDaemon(true); // Allow JVM to exit even if threads are running
                        return t;
                    });
    
    // Cache for scan results
    private static final ConcurrentHashMap<String, CachedScanResult> scanCache = new ConcurrentHashMap<>();

    // Gitignore parser instance
    private final GitignoreParser gitignoreParser;
    private final VirtualFile baseDir;
    
    // Scan statistics
    private final AtomicInteger fileCount = new AtomicInteger(0);
    private final AtomicInteger dirCount = new AtomicInteger(0);
    private final AtomicBoolean scanCancelled = new AtomicBoolean(false);

    /**
     * Cached scan result
     */
    private static class CachedScanResult {
        final long lastModified;
        final List<VirtualFile> files;

        CachedScanResult(long lastModified, List<VirtualFile> files) {
            this.lastModified = lastModified;
            this.files = files;
        }
    }

    public CachedProjectScanner(VirtualFile baseDir) {
        this.baseDir = baseDir;
        this.gitignoreParser = new GitignoreParser(baseDir);
    }

    /**
     * Scans a given directory while respecting .gitignore rules and using caching.
     * @return list of VirtualFiles found
     */
    public List<VirtualFile> scanDirectoryWithCache() {
        // Reset counters for this scan
        fileCount.set(0);
        dirCount.set(0);
        scanCancelled.set(false);

        String key = baseDir.getPath();
        long modStamp = baseDir.getTimeStamp();
        long startTime = System.currentTimeMillis();

        // Check cache first
        CachedScanResult cached = scanCache.get(key);
        if (cached != null && cached.lastModified == modStamp) {
            // ProjectScannerLogPanel.log("Using cached scan result with " + cached.files.size() + " files");
            return cached.files; // Return cached result if directory is unchanged
        }

        // ProjectScannerLogPanel.log("Starting project scan with timeout of " + SCAN_TIMEOUT_MS/1000 + " seconds");
        
        try {
            // Create a container for collected files
            List<VirtualFile> collectedFiles = Collections.synchronizedList(new ArrayList<>());
            
            // Use a CompletableFuture with timeout for the entire scan operation
            CompletableFuture<List<VirtualFile>> scanFuture = safePerformScan(baseDir, collectedFiles);
            
            // Wait for the scan to complete or timeout
            List<VirtualFile> result = scanFuture.get(SCAN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            // Cache the result
            scanCache.put(key, new CachedScanResult(modStamp, result));
            
            return result;
        } catch (TimeoutException e) {
            // Scan timed out - mark as cancelled and return partial results
            scanCancelled.set(true);
            LOG.warn("Scan timed out after " + SCAN_TIMEOUT_MS/1000 + " seconds");
            
            // Get whatever results we have so far (don't cache them)
            return Collections.emptyList();
        } catch (Exception e) {
            // Some other error occurred
            // ProjectScannerLogPanel.log("Error during scan: " + e.getMessage());
            LOG.error("Error during scan", e);
            return Collections.emptyList();
        }
    }

    /**
     * Performs a scan of the directory and its subdirectories with proper timeout and error handling.
     * 
     * @param rootDir The directory to scan
     * @param collectedFiles Container for collected files
     * @return CompletableFuture that will complete with the scan results
     */
    private CompletableFuture<List<VirtualFile>> safePerformScan(
            @NotNull VirtualFile rootDir, 
            @NotNull List<VirtualFile> collectedFiles) {
        
        // Create a CompletableFuture for the entire operation
        CompletableFuture<List<VirtualFile>> result = new CompletableFuture<>();
        
        // Submit the initial scan task
        CompletableFuture.runAsync(() -> {
            try {
                // Start recursive scan from depth 0
                scanDirectory(rootDir, collectedFiles, 0);
                
                // Complete the future with collected files
                result.complete(collectedFiles);
            } catch (Exception e) {
                // Handle any exceptions that occur during scanning
                result.completeExceptionally(e);
            }
        }, SCAN_EXECUTOR);
        
        return result;
    }
    
    /**
     * Recursively scans a directory with depth tracking and timeout awareness.
     * 
     * @param directory The directory to scan
     * @param collectedFiles Container for collected files
     * @param depth Current recursion depth
     */
    private void scanDirectory(
            @NotNull VirtualFile directory, 
            @NotNull List<VirtualFile> collectedFiles, 
            int depth) {
        
        // Check if scan has been cancelled
        if (scanCancelled.get()) {
            return;
        }
        
        // Check depth limit to prevent infinite recursion
        if (depth > MAX_DEPTH) {
            LOG.warn("Max depth reached at: " + directory.getPath());
            return;
        }
        
        // Check file count limit
        if (fileCount.get() > MAX_FILES) {
            LOG.warn("Max file count reached (" + MAX_FILES + "), stopping scan");
            scanCancelled.set(true);
            return;
        }
        
        // Check directory count limit
        if (dirCount.get() > MAX_DIRECTORIES) {
            LOG.warn("Max directory count reached (" + MAX_DIRECTORIES + "), stopping scan");
            scanCancelled.set(true);
            return;
        }
        
        // Track directory count
        dirCount.incrementAndGet();
        
        // First, check if directory should be ignored based on gitignore rules
        String dirRelativePath = getRelativePath(directory);
        if (!dirRelativePath.isEmpty() && gitignoreParser.shouldIgnore(dirRelativePath, true)) {
            // Ignore this directory
            return;
        }
        
        // Get children safely
        VirtualFile[] children = directory.getChildren();
        if (children == null || children.length == 0) {
            return;
        }
        
        // For moderate number of children, process synchronously
        // This avoids creating too many tasks for small directories
        if (children.length <= 20) {
            for (VirtualFile child : children) {
                processChild(child, collectedFiles, depth);
            }
        } else {
            // For larger directories, process children in parallel but with controlled concurrency
            int batchSize = 20;
            List<List<VirtualFile>> batches = new ArrayList<>();
            
            for (int i = 0; i < children.length; i += batchSize) {
                int end = Math.min(children.length, i + batchSize);
                batches.add(Arrays.asList(children).subList(i, end));
            }
            
            // Process each batch in parallel
            CountDownLatch latch = new CountDownLatch(batches.size());
            
            for (List<VirtualFile> batch : batches) {
                // Skip if scan has been cancelled
                if (scanCancelled.get()) {
                    latch.countDown();
                    continue;
                }
                
                // Submit batch processing task
                CompletableFuture.runAsync(() -> {
                    try {
                        for (VirtualFile child : batch) {
                            if (scanCancelled.get()) {
                                break;
                            }
                            processChild(child, collectedFiles, depth);
                        }
                    } finally {
                        latch.countDown();
                    }
                }, SCAN_EXECUTOR);
            }
            
            // Wait for all batches to complete but with a timeout
            try {
                if (!latch.await(15, TimeUnit.SECONDS)) {
                    // Timeout waiting for children to be processed
                    LOG.warn("Timeout waiting for directory children to be processed: " + directory.getPath());
                    scanCancelled.set(true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scanCancelled.set(true);
            }
        }
    }
    
    /**
     * Process a single file or directory
     */
    private void processChild(
            @NotNull VirtualFile child, 
            @NotNull List<VirtualFile> collectedFiles, 
            int depth) {
        
        // Skip if scan has been cancelled
        if (scanCancelled.get()) {
            return;
        }
        
        // Get relative path and check gitignore rules
        String relativePath = getRelativePath(child);
        boolean isDirectory = child.isDirectory();
        
        // Skip if this path should be ignored
        if (gitignoreParser.shouldIgnore(relativePath, isDirectory)) {
            return;
        }
        
        if (isDirectory) {
            // Recursively scan subdirectory
            scanDirectory(child, collectedFiles, depth + 1);
        } else {
            // Add file to results and track count
            collectedFiles.add(child);
            int count = fileCount.incrementAndGet();
            // Check if we've reached the file limit
            if (count >= MAX_FILES) {
                scanCancelled.set(true);
            }
        }
    }

    /**
     * Gets the relative path of a file/directory compared to the project's base directory.
     * @param file The file or directory to get the relative path for
     * @return The relative path
     */
    private @NotNull String getRelativePath(@NotNull VirtualFile file) {
        String basePath = baseDir.getPath();
        String filePath = file.getPath();

        if (filePath.startsWith(basePath)) {
            String relativePath = filePath.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }
        return filePath; // Return absolute path as fallback
    }
    
    /**
     * Clears the cache
     */
    public static void clearCache() {
        scanCache.clear();
    }

    /**
     * Shutdown the scanner executor service
     */
    public static void shutdown() {
        // Make sure we're on EDT before accessing any IntelliJ Platform components
        try {
            if (!ApplicationManager.getApplication().isDispatchThread()) {
                try {
                    ApplicationManager.getApplication().invokeAndWait(CachedProjectScanner::performShutdown);
                } catch (Exception e) {
                    LOG.error("Error during scanner shutdown", e);
                    // Fallback to direct shutdown if EDT access fails
                    performShutdown();
                }
            } else {
                performShutdown();
            }
        } catch (Exception e) {
            LOG.error("Unexpected error during scanner shutdown", e);
            // Last resort emergency shutdown
            SCAN_EXECUTOR.shutdownNow();
        }
    }
    
    /**
     * Performs the actual shutdown operations
     */
    private static void performShutdown() {
        SCAN_EXECUTOR.shutdown();
        try {
            // Wait for ongoing tasks to complete
            if (!SCAN_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                // Force shutdown if tasks don't complete
                SCAN_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCAN_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
