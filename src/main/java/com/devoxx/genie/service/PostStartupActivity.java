package com.devoxx.genie.service;

import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolShutdownManager;
import com.devoxx.genie.ui.util.ThemeChangeListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class PostStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // Initialize chat memory for this project
        ChatMemoryManager chatMemoryManager = ChatMemoryManager.getInstance();
        if (chatMemoryManager != null) {
            chatMemoryManager.initializeMemory(project);
        } else {
            log.error("chatMemoryManager is null");
        }
        
        // Initialize thread pool manager and shutdown handler
        ThreadPoolManager threadPoolManager = ThreadPoolManager.getInstance();
        if (threadPoolManager != null) {
            log.debug("Thread pool manager initialized");
            
            // Force class loading of shutdown manager to register listeners
            try {
                Class.forName(ThreadPoolShutdownManager.class.getName());
                log.debug("Thread pool shutdown manager registered");
            } catch (ClassNotFoundException e) {
                log.error("Failed to load ThreadPoolShutdownManager", e);
            }
        } else {
            log.error("threadPoolManager is null");
        }

        // Register theme change listener
        if (project.isDefault()) {
            // Only register the listener once during application startup
            ThemeChangeListener.register();
            log.debug("Registered ThemeChangeListener for theme changes");
        }

        return Unit.INSTANCE;
    }
}
