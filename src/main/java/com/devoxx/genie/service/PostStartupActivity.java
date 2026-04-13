package com.devoxx.genie.service;

import com.devoxx.genie.service.analytics.AnalyticsConsentNotifier;
import com.devoxx.genie.service.analytics.AnalyticsSessionSnapshotService;
import com.devoxx.genie.service.automation.listeners.BuildCompilationListener;
import com.devoxx.genie.service.automation.listeners.FileEventListener;
import com.devoxx.genie.service.automation.listeners.FileSaveListener;
import com.devoxx.genie.service.automation.listeners.ProcessExitListener;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolShutdownManager;
import com.devoxx.genie.ui.util.ThemeChangeListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.messages.MessageBusConnection;
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

        ThemeChangeListener.register();
        log.debug("Registered ThemeChangeListener for theme changes");

        // Register event automation listeners (project-scoped)
        registerEventAutomationListeners(project);

        // First-launch analytics consent notification (task-206). Self-disables after firing once.
        AnalyticsConsentNotifier.maybeShow(project);

        // Feature enablement snapshot — app-level, emitted at most once per IDE session (task-209).
        AnalyticsSessionSnapshotService.getInstance().snapshotIfNeeded();

        return Unit.INSTANCE;
    }

    /**
     * Registers IDE event listeners that feed into the Event Automation system.
     * Listeners are tied to the project lifecycle and disposed when the project closes.
     */
    private void registerEventAutomationListeners(@NotNull Project project) {
        MessageBusConnection connection = project.getMessageBus().connect();

        // File editor events (FILE_OPENED)
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEventListener(project));

        // File save events (FILE_SAVED) — application-level topic
        connection.subscribe(FileDocumentManagerListener.TOPIC,
                new FileSaveListener());

        // Build/compilation events (BUILD_FAILED, BUILD_SUCCEEDED)
        // CompilerTopics is only available in IDEs with compiler support (e.g. IntelliJ IDEA),
        // not in PhpStorm, WebStorm, etc.
        try {
            connection.subscribe(CompilerTopics.COMPILATION_STATUS,
                    new BuildCompilationListener(project));
        } catch (NoClassDefFoundError e) {
            log.debug("CompilerTopics not available in this IDE, skipping build compilation listener");
        }

        // Process exit events (PROCESS_CRASHED)
        connection.subscribe(ExecutionManager.EXECUTION_TOPIC,
                new ProcessExitListener());

        log.debug("Registered event automation listeners for project: {}", project.getName());
    }
}
