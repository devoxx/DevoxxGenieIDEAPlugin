package com.devoxx.genie.ui.window;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.ui.panel.spec.SpecBrowserPanel;
import com.devoxx.genie.ui.panel.spec.SpecKanbanPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
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

public final class DevoxxGenieToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final int MAX_TABS = 8;
    private static final String NEW_CHAT_TAB_NAME = "New Chat";

    /**
     * Marker key to distinguish spec Content tabs from chat Content tabs.
     */
    static final Key<Boolean> IS_SPEC_CONTENT = Key.create("DevoxxGenie.isSpecContent");

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

        // Add spec tabs if the feature is enabled
        addSpecTabsIfEnabled(project, toolWindow);

        // Add "New Chat" and "Settings" actions to the tool window title bar
        toolWindow.setTitleActions(List.of(
                new NewChatTabAction(project, toolWindow),
                new OpenSettingsAction(project)
        ));

        // Register listener for tab close cleanup and state persistence
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                Content content = event.getContent();

                // Skip cleanup for spec tabs — they are managed separately
                if (Boolean.TRUE.equals(content.getUserData(IS_SPEC_CONTENT))) {
                    return;
                }

                cleanupTab(project, content);

                // Persist remaining open tabs
                persistOpenTabs(project);

                // If all chat tabs are closed, create a new default tab
                if (getChatTabCount(toolWindow.getContentManager()) == 0) {
                    SwingUtilities.invokeLater(() -> createNewTab(project, toolWindow));
                }
            }
        });

        // Listen for settings changes to dynamically add/remove spec tabs
        MessageBusConnection settingsConnection = project.getMessageBus().connect(toolWindow.getDisposable());
        settingsConnection.subscribe(AppTopics.SETTINGS_CHANGED_TOPIC,
                (com.devoxx.genie.ui.listener.SettingsChangeListener) hasKey -> {
                    SwingUtilities.invokeLater(() -> updateSpecTabs(project, toolWindow));
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

        if (getChatTabCount(contentManager) >= MAX_TABS) {
            return null;
        }

        DevoxxGenieToolWindowContent toolWindowContent = tabId != null
                ? new DevoxxGenieToolWindowContent(toolWindow, tabId)
                : new DevoxxGenieToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(
                toolWindowContent.getContentPanel(), NEW_CHAT_TAB_NAME, false);
        content.setCloseable(true); // All chat tabs are closeable; closing last tab auto-creates a new one
        toolWindowContent.setTabContent(content);

        // Insert before spec tabs so chat tabs always come first
        int insertIndex = getChatTabCount(contentManager);
        contentManager.addContent(content, insertIndex);
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
     * Returns the number of chat (non-spec) tabs in the content manager.
     */
    static int getChatTabCount(@NotNull ContentManager cm) {
        int count = 0;
        for (Content c : cm.getContents()) {
            if (!Boolean.TRUE.equals(c.getUserData(IS_SPEC_CONTENT))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Add spec tabs (Task List + Kanban Board) if the SDD feature is enabled.
     */
    private void addSpecTabsIfEnabled(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (!isSpecEnabled(project)) {
            return;
        }

        ContentManager contentManager = toolWindow.getContentManager();

        // Task List tab
        SpecBrowserPanel specBrowserPanel = new SpecBrowserPanel(project);
        Content taskListContent = ContentFactory.getInstance().createContent(
                specBrowserPanel, "Task List", false);
        taskListContent.putUserData(IS_SPEC_CONTENT, true);
        taskListContent.setCloseable(false);
        taskListContent.setPinned(true);
        contentManager.addContent(taskListContent);

        // Kanban Board tab
        SpecKanbanPanel kanbanPanel = new SpecKanbanPanel(project);
        Content kanbanContent = ContentFactory.getInstance().createContent(
                kanbanPanel, "Kanban Board", false);
        kanbanContent.putUserData(IS_SPEC_CONTENT, true);
        kanbanContent.setCloseable(false);
        kanbanContent.setPinned(true);
        contentManager.addContent(kanbanContent);

        // Register kanban panel for disposal on IDE shutdown
        Disposer.register(toolWindow.getDisposable(), kanbanPanel);
    }

    /**
     * Dynamically add or remove spec tabs when settings change.
     */
    private void updateSpecTabs(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        boolean hasSpecTabs = hasSpecTabs(contentManager);
        boolean shouldHaveSpecTabs = isSpecEnabled(project);

        if (shouldHaveSpecTabs && !hasSpecTabs) {
            // Add spec tabs
            addSpecTabsIfEnabled(project, toolWindow);
        } else if (!shouldHaveSpecTabs && hasSpecTabs) {
            // Remove spec tabs — switch to a chat tab first if a spec tab is selected
            Content selected = contentManager.getSelectedContent();
            if (selected != null && Boolean.TRUE.equals(selected.getUserData(IS_SPEC_CONTENT))) {
                Content firstChatTab = findFirstChatTab(contentManager);
                if (firstChatTab != null) {
                    contentManager.setSelectedContent(firstChatTab);
                }
            }

            // Remove and dispose spec tabs
            for (Content c : contentManager.getContents()) {
                if (Boolean.TRUE.equals(c.getUserData(IS_SPEC_CONTENT))) {
                    JComponent component = c.getComponent();
                    if (component instanceof SpecBrowserPanel sbp) {
                        sbp.dispose();
                    } else if (component instanceof SpecKanbanPanel skp) {
                        skp.dispose();
                    }
                    contentManager.removeContent(c, true);
                }
            }
        }
    }

    private static boolean hasSpecTabs(@NotNull ContentManager cm) {
        for (Content c : cm.getContents()) {
            if (Boolean.TRUE.equals(c.getUserData(IS_SPEC_CONTENT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpecEnabled(@NotNull Project project) {
        return Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getSpecBrowserEnabled())
                || SpecService.getInstance(project).hasSpecDirectory();
    }

    /**
     * Find the first non-spec (chat) Content tab.
     */
    public static Content findFirstChatTab(@NotNull ContentManager cm) {
        for (Content c : cm.getContents()) {
            if (!Boolean.TRUE.equals(c.getUserData(IS_SPEC_CONTENT))) {
                return c;
            }
        }
        return null;
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
                Disposer.dispose(toolWindowContent.getTabDisposable());
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
                    getChatTabCount(toolWindow.getContentManager()) < MAX_TABS);
        }
    }

    /**
     * Action to open the DevoxxGenie settings dialog.
     */
    private static class OpenSettingsAction extends AnAction implements DumbAware {
        private final Project project;

        OpenSettingsAction(@NotNull Project project) {
            super("Settings", "Open DevoxxGenie settings", com.intellij.icons.AllIcons.General.GearPlain);
            this.project = project;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            com.devoxx.genie.ui.util.SettingsDialogUtil.showSettingsDialog(project);
        }
    }
}
