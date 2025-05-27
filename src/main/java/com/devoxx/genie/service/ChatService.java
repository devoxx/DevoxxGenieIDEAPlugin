package com.devoxx.genie.service;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.panel.conversation.ConversationManager;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class ChatService implements ConversationEventListener {

    private final ConversationStorageService storageService;
    private final Project project;
    private ConversationManager conversationManager;

    public ChatService(@NotNull Project project) {
        this.storageService = ConversationStorageService.getInstance();
        this.project = project;

        project.getMessageBus()
                .connect()
                .subscribe(AppTopics.CONVERSATION_TOPIC, this);
    }
    
    /**
     * Set the conversation manager for conversation tracking.
     * 
     * @param conversationManager The conversation manager
     */
    public void setConversationManager(@Nullable ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    @Override
    public void onNewConversation(@NotNull ChatMessageContext chatMessageContext) {
        saveConversation(chatMessageContext);
    }

    private void saveConversation(@NotNull ChatMessageContext chatMessageContext) {
        String userPrompt = chatMessageContext.getUserPrompt();
        if (!chatMessageContext.getProject().getName().equalsIgnoreCase(project.getName()) ||
                userPrompt == null || userPrompt.trim().isEmpty()) {
            return;
        }
        
        // Check if we have an active conversation to append to
        Conversation conversation = null;
        if (conversationManager != null) {
            conversation = conversationManager.getCurrentConversation();
        }
        
        if (conversation == null) {
            // Create a new conversation
            conversation = new Conversation();
            conversation.setId(String.valueOf(System.currentTimeMillis()));
            conversation.setTitle(extractTitle(userPrompt));
            conversation.setTimestamp(LocalDateTime.now().toString());
            conversation.setModelName(chatMessageContext.getLanguageModel().getModelName());
            conversation.setExecutionTimeMs(chatMessageContext.getExecutionTimeMs());
            conversation.setApiKeyUsed(chatMessageContext.getLanguageModel().isApiKeyUsed());
            conversation.setLlmProvider(chatMessageContext.getLanguageModel().getProvider().name());
            conversation.setMessages(new ArrayList<>());
            
            // Set this as the current conversation
            if (conversationManager != null) {
                conversationManager.setCurrentConversation(conversation);
            }
        } else {
            // Update execution time for the existing conversation
            conversation.setExecutionTimeMs(conversation.getExecutionTimeMs() + chatMessageContext.getExecutionTimeMs());
        }

        // Add the new messages to the conversation
        String currentTime = LocalDateTime.now().toString();
        conversation.getMessages().add(new ChatMessage(true, chatMessageContext.getUserPrompt(), currentTime));
        conversation.getMessages().add(new ChatMessage(false, chatMessageContext.getAiMessage().text(), currentTime));

        // Save or update the conversation
        storageService.addConversation(project, conversation);
    }
    
    /**
     * Extract a suitable title from the user prompt.
     * Limits the title to a reasonable length.
     */
    private String extractTitle(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "New conversation";
        }
        
        String cleanPrompt = userPrompt.trim();
        // Remove line breaks and extra whitespace
        cleanPrompt = cleanPrompt.replaceAll("\\s+", " ");
        
        // Limit length for title
        int maxTitleLength = 100;
        if (cleanPrompt.length() > maxTitleLength) {
            return cleanPrompt.substring(0, maxTitleLength).trim() + "...";
        }
        
        return cleanPrompt;
    }

    public void startNewConversation(String title) {
        Conversation conversation = new Conversation();
        conversation.setId(String.valueOf(System.currentTimeMillis()));
        
        // Use provided title or default to "New conversation"
        if (title != null && !title.trim().isEmpty()) {
            conversation.setTitle(title);
        } else {
            conversation.setTitle("New conversation");
        }
        
        conversation.setTimestamp(LocalDateTime.now().toString());
        // Initialize messages list to prevent NullPointerException
        conversation.setMessages(new ArrayList<>());
        
        // Initialize required fields to prevent NullPointerException
        conversation.setApiKeyUsed(false); // Default to false
        conversation.setInputCost(0L);
        conversation.setOutputCost(0L);
        conversation.setContextWindow(0);
        conversation.setExecutionTimeMs(0);
        conversation.setModelName("None"); // Default model name
        conversation.setLlmProvider("Unknown"); // Default provider
        
        // storageService.addConversation(project, conversation);
        
        // Reload conversation history
        project.getMessageBus()
               .syncPublisher(AppTopics.CONVERSATION_TOPIC)
               .onNewConversation(ChatMessageContext.builder().project(project).build());
    }
}
