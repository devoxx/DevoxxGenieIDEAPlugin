package com.devoxx.genie.service;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatService implements ConversationEventListener {

    private final ConversationStorageService storageService;

    public ChatService(ConversationStorageService storageService) {
        this.storageService = storageService;

        ApplicationManager.getApplication().getMessageBus()
            .connect()
            .subscribe(AppTopics.CONVERSATION_TOPIC, this);
    }

    @Override
    public void onNewConversation(@NotNull ChatMessageContext chatMessageContext) {
        saveConversation(chatMessageContext);
    }

    private void saveConversation(@NotNull ChatMessageContext chatMessageContext) {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setTitle(generateTitle(chatMessageContext.getUserPrompt()));
        conversation.setTimestamp(LocalDateTime.now().toString());
        conversation.setModelName(chatMessageContext.getLanguageModel().getModelName());
        conversation.setExecutionTimeMs(chatMessageContext.getExecutionTimeMs());
        conversation.setApiKeyUsed(chatMessageContext.getLanguageModel().isApiKeyUsed());
        conversation.setLlmProvider(chatMessageContext.getLanguageModel().getProvider().name());

        conversation.getMessages().add(new ChatMessage(true, chatMessageContext.getUserPrompt(), LocalDateTime.now().toString()));
        conversation.getMessages().add(new ChatMessage(false, chatMessageContext.getAiMessage().text(), LocalDateTime.now().toString()));

        storageService.addConversation(conversation);
    }

    private String generateTitle(@NotNull String userPrompt) {
        return userPrompt.length() > 30 ? userPrompt.substring(0, 30) + "..." : userPrompt;
    }

    public void startNewConversation(String title) {
        Conversation conversation = new Conversation();
        conversation.setTitle(title);
        conversation.setTimestamp(LocalDateTime.now().toString());
        conversation.setMessages(new ArrayList<>());
        storageService.addConversation(conversation);
    }

    // TODO: Implement the loadConversations method
    public List<Conversation> loadConversations() {
        return storageService.getConversations();
    }
}
