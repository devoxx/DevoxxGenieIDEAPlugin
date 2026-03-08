package com.devoxx.genie.ui.window;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that maps Content objects to their DevoxxGenieToolWindowContent instances.
 * Used for tab lifecycle management (cleanup on close, routing external prompts).
 */
@Service
public final class ConversationTabRegistry {

    private final Map<Content, DevoxxGenieToolWindowContent> contentMap = new ConcurrentHashMap<>();
    private final Map<Content, MessageBusConnection> connectionMap = new ConcurrentHashMap<>();

    @NotNull
    public static ConversationTabRegistry getInstance() {
        return ApplicationManager.getApplication().getService(ConversationTabRegistry.class);
    }

    public void register(@NotNull Content content, @NotNull DevoxxGenieToolWindowContent toolWindowContent) {
        contentMap.put(content, toolWindowContent);
    }

    public void registerConnection(@NotNull Content content, @NotNull MessageBusConnection connection) {
        connectionMap.put(content, connection);
    }

    public void unregister(@NotNull Content content) {
        contentMap.remove(content);
        // Dispose the message bus connection to prevent listener leaks
        MessageBusConnection connection = connectionMap.remove(content);
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Nullable
    public DevoxxGenieToolWindowContent getToolWindowContent(@NotNull Content content) {
        return contentMap.get(content);
    }

    /**
     * Returns all registered tool window contents for a given project.
     */
    @NotNull
    public java.util.List<DevoxxGenieToolWindowContent> getContentsForProject(@NotNull Project project) {
        return contentMap.values().stream()
                .filter(twc -> twc.getProject().equals(project))
                .toList();
    }

    /**
     * Returns the tabId of the currently selected (active) tab for the given project,
     * or null if no tab is selected.
     */
    @Nullable
    public String getActiveTabId(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevoxxGenie");
        if (toolWindow == null) return null;
        Content selected = toolWindow.getContentManager().getSelectedContent();
        if (selected == null) return null;
        DevoxxGenieToolWindowContent twc = contentMap.get(selected);
        return twc != null ? twc.getTabId() : null;
    }
}
