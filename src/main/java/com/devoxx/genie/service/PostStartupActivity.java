package com.devoxx.genie.service;

import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolShutdownManager;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
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
        
        // Setup MCP Tool Window visibility handler
        MessageBusConnection messageBusConnection = project.getMessageBus().connect();
        
        // Subscribe to settings changes to update MCP tool window visibility
        MessageBusUtil.subscribe(messageBusConnection, AppTopics.SETTINGS_CHANGED_TOPIC, hasKey -> {
            // Update MCP tool window visibility if settings change
            ApplicationManager.getApplication().invokeLater(() -> {
                updateMCPToolWindowVisibility(project);
            });
        });
        
        // Set initial state of MCP tool window
        updateMCPToolWindowVisibility(project);

        return Unit.INSTANCE;
    }
    
    /**
     * Update the visibility of the MCP tool window based on MCP enabled state
     * 
     * @param project The current project
     */
    private void updateMCPToolWindowVisibility(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow mcpToolWindow = toolWindowManager.getToolWindow("DevoxxGenieMCPLogs");
            
            if (mcpToolWindow != null) {
                boolean mcpEnabled = MCPService.isMCPEnabled();
                
                if (mcpEnabled) {
                    // Show tool window if not already visible
                    if (!mcpToolWindow.isAvailable()) {
                        mcpToolWindow.setAvailable(true);
                        log.debug("Made MCP tool window available");
                    }
                } else {
                    // Hide tool window if currently visible
                    if (mcpToolWindow.isAvailable()) {
                        mcpToolWindow.setAvailable(false);
                        log.debug("Made MCP tool window unavailable");
                    }
                }
            }
        });
    }
}
