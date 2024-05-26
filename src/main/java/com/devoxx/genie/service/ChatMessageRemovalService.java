package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.listener.ChatMessageManagementService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.ChatMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChatMessageRemovalService implements ChatMessageManagementService {

    @NotNull
    public static ChatMessageRemovalService getInstance() {
        return ApplicationManager.getApplication().getService(ChatMessageRemovalService.class);
    }

    /**
     * Constructor.
     */
    public ChatMessageRemovalService() {
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = bus.connect();
        connection.subscribe(AppTopics.CHAT_MESSAGES_CHANGED_TOPIC, this);
    }

    /**
     * Removes the user and AI response message from the chat memory.
     *
     * @param chatMessageContext the chat message context
     */
    public void removeMessagePair(ChatMessageContext chatMessageContext) {
        List<ChatMessage> messages = ChatMemoryService.getInstance().messages();

        messages.removeIf(m -> (m.equals(chatMessageContext.getAiMessage()) || m.equals(chatMessageContext.getUserMessage())));

        ChatMemoryService.getInstance().clear();
        messages.forEach(ChatMemoryService.getInstance()::add);
    }
}
