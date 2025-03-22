package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.service.prompt.error.MemoryException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.ChatMessage;
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
 * Provides low-level chat memory storage and operations.
 * This class is responsible for the direct management of memory storage
 * and handles the raw operations on chat messages without business logic.
 */
@Slf4j
public class ChatMemoryService implements ChatMemoryProvider {

    private final Map<String, MessageWindowChatMemory> projectConversations = new ConcurrentHashMap<>();
    private final InMemoryChatMemoryStore inMemoryChatMemoryStore = new InMemoryChatMemoryStore();

    public static ChatMemoryService getInstance() {
        return ApplicationManager.getApplication().getService(ChatMemoryService.class);
    }

    /**
     * Initializes chat memory for a project with the specified size
     * @param project The project to initialize
     * @param chatMemorySize The maximum number of messages to retain
     */
    public void initialize(@NotNull Project project, int chatMemorySize) {
        String projectHash = project.getLocationHash();
        try {
            log.debug("Initializing chat memory for project: {} with size: {}", projectHash, chatMemorySize);

            // If memory already exists for this project, clear it first
            if (projectConversations.containsKey(projectHash)) {
                projectConversations.get(projectHash).clear();
                log.debug("Cleared existing memory for project: {}", projectHash);
            }

            createChatMemory(projectHash, chatMemorySize);
        } catch (Exception e) {
            throw new MemoryException("Failed to initialize chat memory for project: " + projectHash, e);
        }
    }

    /**
     * Clears all messages for a project
     * @param project The project to clear memory for
     */
    public void clearMemory(@NotNull Project project) {
        try {
            String projectHash = project.getLocationHash();
            MessageWindowChatMemory memory = projectConversations.get(projectHash);
            if (memory != null) {
                memory.clear();
                log.debug("Cleared memory for project: {}", projectHash);
            } else {
                log.warn("Attempted to clear memory for non-existent project: {}", projectHash);
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to clear memory", e);
        }
    }

    /**
     * Adds a chat message to the project's memory
     * @param project The project to add the message to
     * @param chatMessage The message to add
     */
    public void addMessage(@NotNull Project project, ChatMessage chatMessage) {
        try {
            String projectHash = project.getLocationHash();
            MessageWindowChatMemory memory = projectConversations.get(projectHash);
            if (memory != null) {
                memory.add(chatMessage);
                log.debug("Added message to project: {}", projectHash);
            } else {
                throw new MemoryException("Chat memory not initialized for project: " + projectHash);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException("Failed to add message to memory", e);
            }
            throw e;
        }
    }

    /**
     * Gets all messages for a project
     * @param project The project to get messages for
     * @return List of chat messages
     */
    public List<ChatMessage> getMessages(@NotNull Project project) {
        try {
            String projectHash = project.getLocationHash();
            MessageWindowChatMemory memory = projectConversations.get(projectHash);
            if (memory != null) {
                return memory.messages();
            } else {
                throw new MemoryException("Chat memory not initialized for project: " + projectHash);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException("Failed to get messages from memory", e);
            }
            throw e;
        }
    }

    /**
     * Checks if memory is empty for a project
     * @param project The project to check
     * @return true if empty, false otherwise
     */
    public boolean isEmpty(@NotNull Project project) {
        try {
            String projectHash = project.getLocationHash();
            MessageWindowChatMemory memory = projectConversations.get(projectHash);
            if (memory != null) {
                return memory.messages().isEmpty();
            } else {
                // If memory doesn't exist, consider it empty
                return true;
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to check if memory is empty", e);
        }
    }

    /**
     * Removes the last message from a project's memory
     * @param project The project to remove from
     */
    public void removeLastMessage(@NotNull Project project) {
        try {
            String projectHash = project.getLocationHash();
            MessageWindowChatMemory memory = projectConversations.get(projectHash);
            if (memory != null) {
                List<ChatMessage> messages = memory.messages();
                if (!messages.isEmpty()) {
                    // Clear and re-add all messages except the last one
                    ChatMessage lastMessage = messages.get(messages.size() - 1);
                    memory.clear();

                    for (int i = 0; i < messages.size() - 1; i++) {
                        memory.add(messages.get(i));
                    }

                    log.debug("Removed last message ({}) from project: {}",
                            lastMessage.getClass().getSimpleName(), projectHash);
                }
            } else {
                throw new MemoryException("Chat memory not initialized for project: " + projectHash);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException("Failed to remove last message from memory", e);
            }
            throw e;
        }
    }

    /**
     * Removes specific messages from a project's memory
     * @param project The project to remove from
     * @param messagesToRemove The messages to remove
     */
    public void removeMessages(@NotNull Project project, List<ChatMessage> messagesToRemove) {
        try {
            String projectHash = project.getLocationHash();
            MessageWindowChatMemory memory = projectConversations.get(projectHash);
            if (memory != null) {
                List<ChatMessage> currentMessages = memory.messages();

                if (!currentMessages.isEmpty() && !messagesToRemove.isEmpty()) {
                    // Create a new list excluding the messages to remove
                    memory.clear();

                    for (ChatMessage message : currentMessages) {
                        if (!messagesToRemove.contains(message)) {
                            memory.add(message);
                        }
                    }

                    log.debug("Removed {} messages from project: {}",
                            messagesToRemove.size(), projectHash);
                }
            } else {
                throw new MemoryException("Chat memory not initialized for project: " + projectHash);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException("Failed to remove messages from memory", e);
            }
            throw e;
        }
    }

    /**
     * Creates and initializes chat memory for a project
     * @param projectHash The project hash
     * @param chatMemorySize The maximum number of messages to retain
     */
    private void createChatMemory(@NotNull String projectHash, int chatMemorySize) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id("devoxxgenie-" + projectHash)
                .chatMemoryStore(inMemoryChatMemoryStore)
                .maxMessages(chatMemorySize)
                .build();
        projectConversations.put(projectHash, chatMemory);
        log.debug("Created new chat memory for project: {}", projectHash);
    }

    @Override
    public ChatMemory get(Object projectHash) {
        try {
            return projectConversations.get((String) projectHash);
        } catch (Exception e) {
            throw new MemoryException("Failed to get chat memory for project hash: " + projectHash, e);
        }
    }
}