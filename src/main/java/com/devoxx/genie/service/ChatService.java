package com.devoxx.genie.service;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
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

    public ChatService(ConversationStorageService storageService, Project project) {
        this.storageService = storageService;
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
        conversation.setId(UUID.randomUUID().toString());
        conversation.setTitle(generateTitle(userPrompt));
        conversation.setTimestamp(LocalDateTime.now().toString());
        conversation.setModelName(chatMessageContext.getLanguageModel().getModelName());
        conversation.setExecutionTimeMs(chatMessageContext.getExecutionTimeMs());
        conversation.setApiKeyUsed(chatMessageContext.getLanguageModel().isApiKeyUsed());
        conversation.setLlmProvider(chatMessageContext.getLanguageModel().getProvider().name());

        conversation.getMessages().add(new ChatMessage(true, chatMessageContext.getUserPrompt(), LocalDateTime.now().toString()));
        conversation.getMessages().add(new ChatMessage(false, chatMessageContext.getAiMessage().text(), LocalDateTime.now().toString()));

        storageService.addConversation(project, conversation);
    }

    private String generateTitle(@NotNull String userPrompt) {
        return userPrompt.length() > 30 ? userPrompt.substring(0, 30) + "..." : userPrompt;
    }

    public void startNewConversation(String title) {
        if (title != null && !title.trim().isEmpty()) {
            Conversation conversation = new Conversation();
            conversation.setTitle(title);
            conversation.setTimestamp(LocalDateTime.now().toString());
            conversation.setMessages(new ArrayList<>());
            storageService.addConversation(project, conversation);
        }
    }
}
