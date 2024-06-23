package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.listener.ChatMemorySizeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
        createChatMemory(DevoxxGenieStateService.getInstance().getChatMemorySize());
        createChangeListener();
    }

    /**
     * Create the change listener.
     */
    private void createChangeListener() {
        ApplicationManager.getApplication().getMessageBus()
            .connect()
            .subscribe(AppTopics.CHAT_MEMORY_SIZE_TOPIC, this);
    }

    /**
     * Get the chat memory service instance.
     * @return the chat memory service instance
     */
    @NotNull
    public static ChatMemoryService getInstance() {
        return ApplicationManager.getApplication().getService(ChatMemoryService.class);
    }

    public void clear() {
        chatMemory.clear();
    }

    /**
     * Add the chat message to the chat memory.
     * @param chatMessage the chat message
     */
    public void add(ChatMessage chatMessage) {
        chatMemory.add(chatMessage);
    }

    /**
     * Remove the chat message from the chat memory.
     * @param chatMessageContext the chat message context
     */
    public void remove(@NotNull ChatMessageContext chatMessageContext) {
        List<ChatMessage> messages = chatMemory.messages();
        messages.remove(chatMessageContext.getAiMessage());
        messages.remove(chatMessageContext.getUserMessage());
        chatMemory.clear();
        messages.forEach(this::add);
    }

    /**
     * Remove the last message from the chat memory.
     * This is used when an exception occurs and the last message is not valid.
     */
    public void removeLast() {
        List<ChatMessage> messages = chatMemory.messages();
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
            chatMemory.clear();
            messages.forEach(this::add);
        }
    }

    /**
     * Get the messages from the chat memory.
     * @return the list of chat messages
     */
    public List<ChatMessage> messages() {
        return chatMemory.messages();
    }

    /**
     * Check if the chat memory is empty.
     * @return true if the chat memory is empty
     */
    public boolean isEmpty() {
        return chatMemory.messages().isEmpty();
    }

    /**
     * On chat memory size changed.
     * @param chatMemorySize the chat memory size
     */
    @Override
    public void onChatMemorySizeChanged(int chatMemorySize) {
        createChatMemory(chatMemorySize);
    }

    /**
     * Create the chat memory.
     * @param chatMemorySize the chat memory size
     */
    private void createChatMemory(int chatMemorySize) {
        chatMemory = MessageWindowChatMemory.builder()
            .id("devoxxgenie")
            .chatMemoryStore(inMemoryChatMemoryStore)
            .maxMessages(chatMemorySize)
            .build();
    }
}
