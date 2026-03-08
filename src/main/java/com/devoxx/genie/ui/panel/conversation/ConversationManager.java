package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.listener.ConversationStarter;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.panel.PromptPanelRegistry;
import com.devoxx.genie.ui.window.ConversationTabRegistry;
import com.devoxx.genie.ui.window.DevoxxGenieToolWindowContent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ResourceBundle;

import static com.devoxx.genie.ui.util.TimestampUtil.getCurrentTimestamp;

/**
 * Manages conversation operations like starting new conversations,
 * handling conversation selection, and managing conversation state.
 */
@Slf4j
public class ConversationManager implements ConversationEventListener, ConversationSelectionListener, ConversationStarter {
    private final Project project;
    private final ChatService chatService;
    private final ConversationHistoryManager historyManager;
    private final MessageRenderer messageRenderer;
    private final JLabel conversationLabel;
    private final String tabId;
    private static final int MAX_TITLE_LENGTH = 50;

    // Track the current active conversation
    @Setter
    @Getter
    private Conversation currentConversation;

    // Reference to this tab's own output panel (set externally)
    @Setter
    private PromptOutputPanel ownPanel;

    /**
     * Creates a new conversation manager with webview refresh callback.
     *
     * @param project The active project
     * @param chatService The chat service
     * @param historyManager The history manager
     * @param messageRenderer The message renderer
     * @param conversationLabel The conversation label to update
     */
    public ConversationManager(Project project,
                              ChatService chatService,
                              ConversationHistoryManager historyManager,
                              MessageRenderer messageRenderer,
                              JLabel conversationLabel) {
        this(project, chatService, historyManager, messageRenderer, conversationLabel, null);
    }

    public ConversationManager(Project project,
                              ChatService chatService,
                              ConversationHistoryManager historyManager,
                              MessageRenderer messageRenderer,
                              JLabel conversationLabel,
                              String tabId) {
        this.project = project;
        this.chatService = chatService;
        this.historyManager = historyManager;
        this.messageRenderer = messageRenderer;
        this.conversationLabel = conversationLabel;
        this.tabId = tabId;
    }

    /**
     * Start a new conversation.
     * Clear the conversation panel, prompt input area, prompt output panel, file list and chat memory.
     * Uses tab-scoped clearing when tabId is available.
     */
    @Override
    public void startNewConversation() {
        // Clear everything for a new conversation using tab-scoped operations
        if (tabId != null) {
            FileListManager.getInstance().clear(project, tabId);
            String memoryKey = project.getLocationHash() + "-" + tabId;
            ChatMemoryService.getInstance().clearMemoryByKey(memoryKey);
        } else {
            FileListManager.getInstance().clear(project);
            ChatMemoryService.getInstance().clearMemory(project);
        }

        // Clear the current conversation state
        currentConversation = null;

        chatService.startNewConversation("");

        ApplicationManager.getApplication().invokeLater(() -> {
            updateNewConversationLabel();

            messageRenderer.clear();
            ResourceBundle resourceBundle = ResourceBundle.getBundle(Constant.MESSAGES);
            messageRenderer.showWelcome(resourceBundle);

            // Only clear this tab's output panel, not all panels
            if (ownPanel != null) {
                ownPanel.clear();
            } else {
                // Fallback: clear all panels for this project
                for (PromptOutputPanel panel : PromptPanelRegistry.getInstance().getPanels(project)) {
                    panel.clear();
                }
            }
        });
    }
    /**
     * Handle selection of a conversation from history.
     *
     * @param conversation The selected conversation
     */
    @Override
    public void onConversationSelected(@NotNull Conversation conversation) {
        // Set this as the current active conversation
        currentConversation = conversation;

        String displayTitle = conversation.getTitle().length() > MAX_TITLE_LENGTH ?
                conversation.getTitle().substring(0, MAX_TITLE_LENGTH) + "..." :
                conversation.getTitle();

        // Update the conversation label with the title
        conversationLabel.setText(displayTitle);

        // Update the tab display name to reflect the restored conversation
        updateTabDisplayName(conversation);

        // Clear the current conversation in the web view without showing welcome screen
        messageRenderer.clearWithoutWelcome();

        // Mark this as not a new conversation in this tab's panel
        if (ownPanel != null) {
            ownPanel.markConversationAsStarted();
        } else {
            for (PromptOutputPanel panel : PromptPanelRegistry.getInstance().getPanels(project)) {
                panel.markConversationAsStarted();
            }
        }

        // Restore all messages for this conversation
        historyManager.restoreConversation(conversation);
    }

    /**
     * Update the tab display name to match the restored conversation title.
     */
    private void updateTabDisplayName(@NotNull Conversation conversation) {
        if (tabId == null) {
            return;
        }
        ConversationTabRegistry registry = ConversationTabRegistry.getInstance();
        for (DevoxxGenieToolWindowContent twc : registry.getContentsForProject(project)) {
            if (tabId.equals(twc.getTabId())) {
                Content content = twc.getTabContent();
                if (content != null) {
                    String title = conversation.getTitle();
                    if (title != null && !title.isEmpty()) {
                        String modelName = conversation.getModelName();
                        String displayName = modelName != null && !modelName.isEmpty()
                                ? modelName + ": " + title
                                : title;
                        if (displayName.length() > 40) {
                            displayName = displayName.substring(0, 40) + "...";
                        }
                        content.setDisplayName(displayName);
                    }
                }
                break;
            }
        }
    }

    /**
     * Update the conversation label with new timestamp.
     */
    public void updateNewConversationLabel() {
        conversationLabel.setText("New conversation " + getCurrentTimestamp());
    }

    /**
     * Handle notification when a new conversation is created.
     *
     * @param chatMessageContext The context of the chat message
     */
    @Override
    public void onNewConversation(ChatMessageContext chatMessageContext) {
        // Reload the conversation history when a new conversation is created
        ApplicationManager.getApplication().invokeLater(() -> {
            historyManager.loadConversationHistory();
            log.debug("Conversation history reloaded after new conversation event");
        });
    }
    
}
