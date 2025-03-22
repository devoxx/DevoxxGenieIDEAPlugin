package com.devoxx.genie.service.prompt.threading;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Manages the shutdown of thread pools when the application is closing
 * or when projects are closed.
 */
@Slf4j
public class ThreadPoolShutdownManager implements ProjectManagerListener {

    // Register this as a static initializer so it's created when the class is loaded
    static {
        ApplicationManager.getApplication().getMessageBus().connect()
            .subscribe(ProjectManager.TOPIC, new ThreadPoolShutdownManager());
        
        // Register a shutdown hook for application exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Application shutdown hook - closing thread pools");
            shutdownThreadPools();
        }));
    }
    
    /**
     * Performs thread pool shutdown
     */
    private static void shutdownThreadPools() {
        try {
            ThreadPoolManager threadPoolManager = ThreadPoolManager.getInstance();
            if (threadPoolManager != null) {
                log.info("Shutting down thread pools");
                threadPoolManager.shutdown();
            }
        } catch (Exception e) {
            log.error("Error during thread pool shutdown", e);
        }
    }
    
    @Override
    public void projectClosed(@NotNull Project project) {
        log.info("Project closed, cancelling any tasks for project: " + project.getName());
        try {
            PromptTaskTracker taskTracker = PromptTaskTracker.getInstance();
            if (taskTracker != null) {
                int count = taskTracker.cancelAllTasks(project);
                if (count > 0) {
                    log.info("Cancelled " + count + " tasks for closing project");
                }
            }
        } catch (Exception e) {
            log.error("Error cancelling tasks during project close", e);
        }
    }
}
