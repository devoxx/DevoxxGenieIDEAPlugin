package com.devoxx.genie.service;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

public class ChatService implements ConversationEventListener {

    private final ConversationStorageService storageService;
    private final Project project;

    public ChatService(Project project) {
        this.storageService = ConversationStorageService.getInstance();
        this.project = project;

        project.getMessageBus()
                .connect()
                .subscribe(AppTopics.CONVERSATION_TOPIC, this);
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
        Conversation conversation = new Conversation();
        conversation.setId(String.valueOf(System.currentTimeMillis()));
        conversation.setTitle(userPrompt);
        conversation.setTimestamp(LocalDateTime.now().toString());
        conversation.setModelName(chatMessageContext.getLanguageModel().getModelName());
        conversation.setExecutionTimeMs(chatMessageContext.getExecutionTimeMs());
        conversation.setApiKeyUsed(chatMessageContext.getLanguageModel().isApiKeyUsed());
        conversation.setLlmProvider(chatMessageContext.getLanguageModel().getProvider().name());

        conversation.getMessages().add(new ChatMessage(true, chatMessageContext.getUserPrompt(), LocalDateTime.now().toString()));
        conversation.getMessages().add(new ChatMessage(false, chatMessageContext.getAiMessage().text(), LocalDateTime.now().toString()));

        storageService.addConversation(project, conversation);
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
        
        storageService.addConversation(project, conversation);
        
        // Reload conversation history
        project.getMessageBus()
               .syncPublisher(AppTopics.CONVERSATION_TOPIC)
               .onNewConversation(ChatMessageContext.builder().project(project).build());
    }
}
