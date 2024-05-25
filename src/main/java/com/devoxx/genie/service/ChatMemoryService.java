package com.devoxx.genie.service;

import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChatMemoryService {

    private final MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

    @NotNull
    public static ChatMemoryService getInstance() {
        return ApplicationManager.getApplication().getService(ChatMemoryService.class);
    }

    public void clear() {
        chatMemory.clear();
    }

    public void add(ChatMessage chatMessage) {
        chatMemory.add(chatMessage);
    }

    public List<ChatMessage> messages() {
        return chatMemory.messages();
    }

    public boolean isEmpty() {
        return chatMemory.messages().isEmpty();
    }
}
