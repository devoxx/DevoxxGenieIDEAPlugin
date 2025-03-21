package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for persistence of chat memory.
 * Handles only data storage and retrieval operations.
 */
@Slf4j
public class ChatMemoryService implements ChatMemoryProvider {

    private final Map<String, MessageWindowChatMemory> projectConversations = new ConcurrentHashMap<>();
    private final InMemoryChatMemoryStore inMemoryChatMemoryStore = new InMemoryChatMemoryStore();

    public static ChatMemoryService getInstance() {
        return ApplicationManager.getApplication().getService(ChatMemoryService.class);
    }

    /**
     * Initialize chat memory storage for a project with the specified size.
     *
     * @param project The project to initialize memory for
     * @param chatMemorySize The maximum number of messages to store
     */
    public void initialize(@NotNull Project project, int chatMemorySize) {
        String projectHash = project.getLocationHash();
        log.debug("Initializing chat memory for project: " + projectHash + " with size: " + chatMemorySize);
        createChatMemory(projectHash, chatMemorySize);
    }

    /**
     * Clear all messages in the chat memory for a project.
     *
     * @param project The project to clear memory for
     */
    public void clear(@NotNull Project project) {
        projectConversations.get(project.getLocationHash()).clear();
    }

    /**
     * Add a message to the chat memory for a project.
     *
     * @param project The project to add the message to
     * @param chatMessage The message to add
     */
    public void add(@NotNull Project project, ChatMessage chatMessage) {
        projectConversations.get(project.getLocationHash()).add(chatMessage);
    }

    /**
     * Get all messages in the chat memory for a project.
     *
     * @param project The project to get messages for
     * @return List of chat messages
     */
    public List<ChatMessage> getMessages(@NotNull Project project) {
        return projectConversations.get(project.getLocationHash()).messages();
    }

    /**
     * Check if the chat memory is empty for a project.
     *
     * @param project The project to check
     * @return True if the chat memory is empty
     */
    public boolean isEmpty(@NotNull Project project) {
        return projectConversations.get(project.getLocationHash()).messages().isEmpty();
    }

    /**
     * Remove the most recent message from the chat memory for a project.
     *
     * @param project The project to remove a message from
     */
    public void removeLastMessage(@NotNull Project project) {
        List<ChatMessage> messages = getMessages(project);
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
            projectConversations.get(project.getLocationHash()).clear();
            messages.forEach(message -> add(project, message));
        }
    }

    /**
     * Remove specific messages from the chat memory.
     *
     * @param project The project to remove messages from
     * @param messagesToRemove The list of messages to remove
     */
    public void removeMessages(@NotNull Project project, List<ChatMessage> messagesToRemove) {
        List<ChatMessage> messages = getMessages(project);
        messages.removeAll(messagesToRemove);
        
        projectConversations.get(project.getLocationHash()).clear();
        messages.forEach(message -> add(project, message));
    }

    /**
     * Restore a conversation from a saved conversation model.
     *
     * @param project The project to restore the conversation for
     * @param conversation The conversation to restore
     */
    public void restoreConversation(@NotNull Project project, @NotNull Conversation conversation) {
        clear(project);
        for (com.devoxx.genie.model.conversation.ChatMessage message : conversation.getMessages()) {
            if (message.isUser()) {
                add(project, UserMessage.from(message.getContent()));
            } else {
                add(project, AiMessage.from(message.getContent()));
            }
        }
    }

    /**
     * Create a new chat memory for a project hash.
     *
     * @param projectHash The project hash to create memory for
     * @param chatMemorySize The maximum number of messages to store
     */
    private void createChatMemory(@NotNull String projectHash, int chatMemorySize) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id("devoxxgenie-" + projectHash)
                .chatMemoryStore(inMemoryChatMemoryStore)
                .maxMessages(chatMemorySize)
                .build();
        projectConversations.put(projectHash, chatMemory);
    }

    @Override
    public ChatMemory get(Object projectHash) {
        return projectConversations.get((String) projectHash);
    }
}
