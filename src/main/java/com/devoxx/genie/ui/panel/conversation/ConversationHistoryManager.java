package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.conversationhistory.ConversationHistoryPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Manages operations related to conversation history, including
 * loading, displaying, and restoring conversations.
 */
@Slf4j
public class ConversationHistoryManager {
    private final Project project;
    private final ConversationHistoryPanel historyPanel;
    private final MessageRenderer messageRenderer;

    /**
     * Creates a new conversation history manager.
     *
     * @param project The active project
     * @param historyPanel The history panel to use
     * @param messageRenderer The message renderer to use
     */
    public ConversationHistoryManager(Project project, 
                                      ConversationHistoryPanel historyPanel,
                                      MessageRenderer messageRenderer) {
        this.project = project;
        this.historyPanel = historyPanel;
        this.messageRenderer = messageRenderer;
    }

    /**
     * Load conversation history into the history panel.
     */
    public void loadConversationHistory() {
        historyPanel.loadConversations();
    }

    /**
     * Show the conversation history popup.
     *
     * @param referenceButton The button to position the popup relative to
     */
    public void showConversationHistoryPopup(@NotNull JButton referenceButton) {
        // Always reload the conversation history before showing it
        loadConversationHistory();
        
        JBPopup historyPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(historyPanel, null)
                .setTitle("Conversation History")
                .setMovable(true)
                .setResizable(true)
                .setMinSize(new Dimension(500, 400))
                .createPopup();

        // Allow the history panel to dismiss the popup on selection
        historyPanel.setPopup(historyPopup);

        // Calculate the position for the popup
        int x = referenceButton.getX() + referenceButton.getWidth() - 500;
        int y = referenceButton.getY() + referenceButton.getHeight();

        // Convert to screen coordinates
        Point screenPoint = new Point(x, y);
        SwingUtilities.convertPointToScreen(screenPoint, referenceButton.getParent());

        // Show the popup at the calculated position
        historyPopup.show(new RelativePoint(screenPoint));
        
        log.debug("Conversation history popup shown with freshly loaded conversations");
    }

    /**
     * Restore a conversation's messages.
     *
     * @param conversation The conversation to restore
     */
    public void restoreConversation(@NotNull Conversation conversation) {
        log.debug("Starting conversation restoration for ID: {}", conversation.getId());

        // Set restoration flag to prevent welcome content from showing
        messageRenderer.setRestorationInProgress(true);

        // Check if conversation has any messages
        List<ChatMessage> messages = conversation.getMessages();
        if (messages == null || messages.isEmpty()) {
            log.warn("Selected conversation has no messages to restore");
            messageRenderer.setRestorationInProgress(false);
            return;
        }
        
        log.debug("Restoring conversation with {} messages", messages.size());

        // Make sure browser is fully initialized before adding messages
        if (!messageRenderer.isInitialized()) {
            log.info("Browser not yet fully initialized, waiting before restoration");
            messageRenderer.ensureBrowserInitialized(() -> processConversationMessages(conversation, messages));
        } else {
            processConversationMessages(conversation, messages);
        }
    }
    
    /**
     * Process and restore all messages in a conversation.
     *
     * @param conversation The conversation to restore
     * @param messages The messages to process
     */
    private void processConversationMessages(Conversation conversation,
                                            @NotNull List<ChatMessage> messages) {
        try {
            // Check if the conversation has any messages
            if (messages.isEmpty()) {
                log.warn("No messages to restore");
                messageRenderer.setRestorationInProgress(false);
                return;
            }

            log.debug("Starting to process {} messages for conversation restoration", messages.size());

        // Clear the current view before restoring messages
        messageRenderer.clearWithoutWelcome();

        // Process all messages
        int messageIndex = 0;

        // If the first message is an AI message, handle it specially
        if (messageIndex < messages.size() && !messages.get(messageIndex).isUser()) {
            log.debug("First message is an AI message, creating an empty user message");
            
            // Create context for an empty user message paired with this AI message
            ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .executionTimeMs(conversation.getExecutionTimeMs() > 0 ? conversation.getExecutionTimeMs() : 1000)
                .build();
            
            // Set a stable, unique message ID based on conversation ID + message index
            String messageId = conversation.getId() + "_msg_" + messageIndex;
            context.setId(messageId);
            
            // Set empty user prompt
            context.setUserPrompt(""); 
            
            // Set the AI message
            ChatMessage aiMessage = messages.get(messageIndex);
            context.setAiMessage(AiMessage.from(aiMessage.getContent()));
            
            // Add LLM information
            populateModelInfo(context, conversation);
            
            // Add the message pair
            messageRenderer.addCompleteChatMessage(context);
            
            // Advance to next message
            messageIndex++;
        }

        // Process remaining messages
        while (messageIndex < messages.size()) {
            // Get the current message
            ChatMessage currentMessage = messages.get(messageIndex);
            
            // Create the context for this message or message pair
            ChatMessageContext context = ChatMessageContext.builder()
                .project(project)
                .executionTimeMs(conversation.getExecutionTimeMs() > 0 ? conversation.getExecutionTimeMs() : 1000)
                .build();
            
            // Set a stable, unique message ID based on conversation ID + message index
            // This ensures IDs are consistent if we restore the same conversation multiple times
            String messageId = conversation.getId() + "_msg_" + messageIndex;
            context.setId(messageId);
            
            // Add LLM information
            populateModelInfo(context, conversation);
            
            if (currentMessage.isUser()) {
                // Handle user message
                context.setUserPrompt(currentMessage.getContent());
                
                // Check if there's a next message that's an AI response
                if (messageIndex + 1 < messages.size() && !messages.get(messageIndex + 1).isUser()) {
                    // We have a matching pair
                    ChatMessage aiMessage = messages.get(messageIndex + 1);
                    context.setAiMessage(AiMessage.from(aiMessage.getContent()));
                    
                    // Add the complete message pair
                    messageRenderer.addCompleteChatMessage(context);
                    
                    // Advance by 2 messages
                    messageIndex += 2;
                } else {
                    // Just a user message with no response
                    messageRenderer.addUserMessageOnly(context);
                    messageIndex++;
                }
            } else {
                // Handle standalone AI message that wasn't paired with a user message
                log.warn("Found unpaired AI message at position {}", messageIndex);
                context.setUserPrompt(""); // Empty user prompt
                context.setAiMessage(AiMessage.from(currentMessage.getContent()));
                
                // Add the message pair with empty user message
                messageRenderer.addCompleteChatMessage(context);
                messageIndex++;
            }
        }
            
            // After adding all messages, scroll to the top
            messageRenderer.scrollToTop();
            
            // Clear the restoration flag now that we're done
            messageRenderer.clearRestorationFlag();
            messageRenderer.setRestorationInProgress(false);
            
            log.debug("Completed conversation restoration for ID: {} with {} messages", conversation.getId(), messages.size());
        } catch (Exception e) {
            log.error("Error during conversation restoration", e);
            // Always clear the restoration flag on error
            messageRenderer.setRestorationInProgress(false);
            messageRenderer.clearRestorationFlag();
            throw e; // Re-throw to maintain existing error handling
        }
    }

    /**
     * Helper to populate model information.
     */
    private void populateModelInfo(ChatMessageContext context, @NotNull Conversation conversation) {
        if (conversation.getModelName() != null && !conversation.getModelName().isEmpty()) {
            LanguageModel model = new LanguageModel();
            model.setModelName(conversation.getModelName());
            
            // Add provider information if available
            if (conversation.getLlmProvider() != null && !conversation.getLlmProvider().isEmpty()) {
                try {
                    model.setProvider(ModelProvider.valueOf(conversation.getLlmProvider()));
                } catch (IllegalArgumentException e) {
                    // Handle case where provider name might have changed
                    log.warn("Unknown provider name in saved conversation: {}", conversation.getLlmProvider());
                }
            }
            
            model.setApiKeyUsed(conversation.getApiKeyUsed() != null ? conversation.getApiKeyUsed() : false);
            model.setDisplayName(conversation.getModelName());
            context.setLanguageModel(model);
        }
    }
}
