package com.devoxx.genie.ui.window;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

final class DevoxxGenieToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final int MAX_TABS = 8;
    private static final String NEW_CHAT_TAB_NAME = "New Chat";

    @Override
    public void createToolWindowContent(@NotNull Project project,
                                        @NotNull ToolWindow toolWindow) {
        // Try to restore tabs from previous session
        List<String> savedTabIds = DevoxxGenieStateService.getInstance()
                .getOpenTabIds(project.getLocationHash());

        if (savedTabIds.isEmpty()) {
            // No saved tabs — create a fresh default tab
            createNewTab(project, toolWindow);
        } else {
            // Restore saved tabs
            for (String tabId : savedTabIds) {
                createNewTabWithId(project, toolWindow, tabId);
            }
        }

        // Add "New Chat" action to the tool window title bar
        toolWindow.setTitleActions(List.of(new NewChatTabAction(project, toolWindow)));

        // Register listener for tab close cleanup and state persistence
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                Content content = event.getContent();
                cleanupTab(project, content);

                // Persist remaining open tabs
                persistOpenTabs(project);

                // If all tabs are closed, create a new default tab
                if (toolWindow.getContentManager().getContentCount() == 0) {
                    SwingUtilities.invokeLater(() -> createNewTab(project, toolWindow));
                }
            }

        });
    }

    /**
     * Creates a new chat tab in the tool window with a generated tabId.
     */
    static DevoxxGenieToolWindowContent createNewTab(@NotNull Project project,
                                                      @NotNull ToolWindow toolWindow) {
        return createNewTabWithId(project, toolWindow, null);
    }

    /**
     * Creates a new chat tab with a specific tabId (null generates a new one).
     */
    static DevoxxGenieToolWindowContent createNewTabWithId(@NotNull Project project,
                                                            @NotNull ToolWindow toolWindow,
                                                            String tabId) {
        ContentManager contentManager = toolWindow.getContentManager();

        if (contentManager.getContentCount() >= MAX_TABS) {
            return null;
        }

        DevoxxGenieToolWindowContent toolWindowContent = tabId != null
                ? new DevoxxGenieToolWindowContent(toolWindow, tabId)
                : new DevoxxGenieToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(
                toolWindowContent.getContentPanel(), NEW_CHAT_TAB_NAME, false);
        content.setCloseable(true); // All tabs are closeable; closing last tab auto-creates a new one
        toolWindowContent.setTabContent(content);

        contentManager.addContent(content);
        contentManager.setSelectedContent(content);

        // Register in tab registry for lifecycle management
        ConversationTabRegistry.getInstance().register(content, toolWindowContent);

        // Subscribe to settings changes with a per-tab connection
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(AppTopics.SETTINGS_CHANGED_TOPIC, toolWindowContent);

        // Store connection for cleanup on tab close
        ConversationTabRegistry.getInstance().registerConnection(content, connection);

        // Persist after registry registration so the new tab is included
        persistOpenTabs(project);

        return toolWindowContent;
    }

    /**
     * Persist the currently open tab IDs for this project.
     */
    private static void persistOpenTabs(@NotNull Project project) {
        List<String> tabIds = ConversationTabRegistry.getInstance()
                .getContentsForProject(project)
                .stream()
                .map(DevoxxGenieToolWindowContent::getTabId)
                .collect(Collectors.toList());
        DevoxxGenieStateService.getInstance().setOpenTabIds(project.getLocationHash(), tabIds);
    }

    /**
     * Cleanup resources when a tab is closed.
     */
    private void cleanupTab(@NotNull Project project, @NotNull Content content) {
        ConversationTabRegistry registry = ConversationTabRegistry.getInstance();
        DevoxxGenieToolWindowContent toolWindowContent = registry.getToolWindowContent(content);
        if (toolWindowContent != null) {
            String memoryKey = toolWindowContent.getMemoryKey();
            String tabId = toolWindowContent.getTabId();

            // Clean up chat memory for this tab
            ChatMemoryManager.getInstance().removeMemory(memoryKey);

            // Clean up file lists for this tab
            FileListManager.getInstance().clear(project, tabId);

            // Dispose the per-tab disposable (disconnects all message bus listeners)
            if (toolWindowContent.getTabDisposable() != null) {
                com.intellij.openapi.util.Disposer.dispose(toolWindowContent.getTabDisposable());
            }

            // Dispose conversation panel resources
            if (toolWindowContent.getPromptOutputPanel() != null
                    && toolWindowContent.getPromptOutputPanel().getConversationPanel() != null) {
                toolWindowContent.getPromptOutputPanel().getConversationPanel().dispose();
            }

            // Unregister from registry (also disconnects per-tab settings connection)
            registry.unregister(content);
        }
    }

    /**
     * Action to create a new chat tab.
     */
    private static class NewChatTabAction extends AnAction implements DumbAware {
        private final Project project;
        private final ToolWindow toolWindow;

        NewChatTabAction(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            super("New Chat", "Open a new chat tab", com.intellij.icons.AllIcons.General.Add);
            this.project = project;
            this.toolWindow = toolWindow;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DevoxxGenieToolWindowContent content = createNewTab(project, toolWindow);
            if (content == null) {
                com.devoxx.genie.ui.util.NotificationUtil.sendNotification(
                        project, "Maximum number of tabs (" + MAX_TABS + ") reached.");
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(
                    toolWindow.getContentManager().getContentCount() < MAX_TABS);
        }
    }
}
