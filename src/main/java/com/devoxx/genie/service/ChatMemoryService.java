package com.devoxx.genie.service;

import com.devoxx.genie.ui.listener.ChatMemorySizeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChatMemoryService implements ChatMemorySizeListener {

    private final InMemoryChatMemoryStore inMemoryChatMemoryStore = new InMemoryChatMemoryStore();

    private MessageWindowChatMemory chatMemory;

    /**
     * Initialize the chat memory service triggered by PostStartupActivity
     * @link PostStartupActivity
     */
    public void init() {
        createChatMemory(SettingsStateService.getInstance().getChatMemorySize());
        createChangeListener();
    }

    private void createChangeListener() {
        ApplicationManager.getApplication().getMessageBus()
            .connect()
            .subscribe(AppTopics.CHAT_MEMORY_SIZE_TOPIC, this);
    }

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

    @Override
    public void onChatMemorySizeChanged(int chatMemorySize) {
        createChatMemory(chatMemorySize);
    }

    private void createChatMemory(int chatMemorySize) {
        chatMemory = MessageWindowChatMemory.builder()
            .id("devoxxgenie")
            .chatMemoryStore(inMemoryChatMemoryStore)
            .maxMessages(chatMemorySize)
            .build();
    }
}
