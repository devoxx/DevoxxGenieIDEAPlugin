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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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
    private static final int MAX_TITLE_LENGTH = 50;

    /**
     * Creates a new conversation manager.
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
        this.project = project;
        this.chatService = chatService;
        this.historyManager = historyManager;
        this.messageRenderer = messageRenderer;
        this.conversationLabel = conversationLabel;
    }

    /**
     * Start a new conversation.
     * Clear the conversation panel, prompt input area, prompt output panel, file list and chat memory.
     */
    @Override
    public void startNewConversation() {
        FileListManager.getInstance().clear(project);
        ChatMemoryService.getInstance().clearMemory(project);

        chatService.startNewConversation("");

        ApplicationManager.getApplication().invokeLater(() -> {
            updateNewConversationLabel();
            messageRenderer.clear();
            // Use ResourceBundle.getBundle to get the resource bundle
            ResourceBundle resourceBundle = ResourceBundle.getBundle(Constant.MESSAGES);
            messageRenderer.showWelcome(resourceBundle);
            
            // Make sure all panels know this is a new conversation
            for (PromptOutputPanel panel : PromptPanelRegistry.getInstance().getPanels(project)) {
                // The clear() method already resets isNewConversation = true
                panel.clear();
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

        String displayTitle = conversation.getTitle().length() > MAX_TITLE_LENGTH ?
                conversation.getTitle().substring(0, MAX_TITLE_LENGTH) + "..." :
                conversation.getTitle();

        // Update the conversation label with the title
        conversationLabel.setText(displayTitle);

        // Clear the current conversation in the web view without showing welcome screen
        messageRenderer.clear();

        // Mark this as not a new conversation in any panel that might be registered
        for (PromptOutputPanel panel : PromptPanelRegistry.getInstance().getPanels(project)) {
            panel.markConversationAsStarted();
        }

        // Restore all messages for this conversation
        historyManager.restoreConversation(conversation);
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
