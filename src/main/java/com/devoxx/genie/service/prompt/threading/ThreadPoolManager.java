package com.devoxx.genie.service.prompt.threading;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized thread pool manager for the application.
 * Provides standardized thread pools for various types of tasks.
 */
@Slf4j
@Getter
public class ThreadPoolManager {

    /**
     * -- GETTER --
     *  Returns the thread pool for prompt execution tasks.
     */
    private final ExecutorService promptExecutionPool;
    /**
     * -- GETTER --
     *  Returns the thread pool for scheduled tasks.
     */
    private final ScheduledExecutorService scheduledTaskPool;
    
    public static ThreadPoolManager getInstance() {
        return ApplicationManager.getApplication().getService(ThreadPoolManager.class);
    }
    
    public ThreadPoolManager() {
        int cores = Runtime.getRuntime().availableProcessors();
        
        // Create thread pools with custom thread factories for better naming
        promptExecutionPool = Executors.newFixedThreadPool(cores, 
            new NamedThreadFactory("prompt-exec"));
        scheduledTaskPool = Executors.newScheduledThreadPool(2,
            new NamedThreadFactory("scheduled-task"));
            
        log.info("ThreadPoolManager initialized with " + cores + " threads for prompt execution");
    }
    
    /**
     * Shuts down all thread pools managed by this class.
     * Should be called during application shutdown.
     */
    public void shutdown() {
        log.info("Shutting down thread pools");
        promptExecutionPool.shutdown();
        scheduledTaskPool.shutdown();
        
        try {
            if (!promptExecutionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Prompt execution pool did not terminate in time, forcing shutdown");
                promptExecutionPool.shutdownNow();
            }
            if (!scheduledTaskPool.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Scheduled task pool did not terminate in time, forcing shutdown");
                scheduledTaskPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Thread pool shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * A thread factory that creates named threads for better debugging.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public @NotNull Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }
}
