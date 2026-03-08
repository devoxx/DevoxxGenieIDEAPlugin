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

    public static final String CHAT_MEMORY_NOT_INITIALIZED_FOR_PROJECT = "Chat memory not initialized for project: ";
    public static final String FAILED_TO_REMOVE_MESSAGES_FROM_MEMORY = "Failed to remove messages from memory";
    public static final String FAILED_TO_GET_CHAT_MEMORY_FOR_PROJECT_HASH = "Failed to get chat memory for project hash: ";
    public static final String FAILED_TO_INITIALIZE_CHAT_MEMORY_FOR_PROJECT = "Failed to initialize chat memory for project: ";
    public static final String FAILED_TO_CLEAR_MEMORY = "Failed to clear memory";
    public static final String FAILED_TO_ADD_MESSAGE_TO_MEMORY = "Failed to add message to memory";
    public static final String FAILED_TO_GET_MESSAGES_FROM_MEMORY = "Failed to get messages from memory";
    public static final String FAILED_TO_CHECK_IF_MEMORY_IS_EMPTY = "Failed to check if memory is empty";
    public static final String FAILED_TO_REMOVE_LAST_MESSAGE_FROM_MEMORY = "Failed to remove last message from memory";

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
        initializeByKey(project.getLocationHash(), chatMemorySize);
    }

    /**
     * Initializes chat memory for a composite key (e.g., projectHash-tabId) with the specified size
     * @param memoryKey The composite key to initialize
     * @param chatMemorySize The maximum number of messages to retain
     */
    public void initializeByKey(@NotNull String memoryKey, int chatMemorySize) {
        try {
            log.debug("Initializing chat memory for key: {} with size: {}", memoryKey, chatMemorySize);

            // If memory already exists for this key, clear it first
            if (projectConversations.containsKey(memoryKey)) {
                projectConversations.get(memoryKey).clear();
                log.debug("Cleared existing memory for key: {}", memoryKey);
            }

            createChatMemory(memoryKey, chatMemorySize);
        } catch (Exception e) {
            throw new MemoryException(FAILED_TO_INITIALIZE_CHAT_MEMORY_FOR_PROJECT + memoryKey, e);
        }
    }

    /**
     * Clears all messages for a project
     * @param project The project to clear memory for
     */
    public void clearMemory(@NotNull Project project) {
        clearMemoryByKey(project.getLocationHash());
    }

    /**
     * Clears all messages for a given memory key
     * @param memoryKey The memory key to clear
     */
    public void clearMemoryByKey(@NotNull String memoryKey) {
        try {
            MessageWindowChatMemory memory = projectConversations.get(memoryKey);
            if (memory != null) {
                memory.clear();
                log.debug("Cleared memory for key: {}", memoryKey);
            } else {
                log.warn("Attempted to clear memory for non-existent key: {}", memoryKey);
            }
        } catch (Exception e) {
            throw new MemoryException(FAILED_TO_CLEAR_MEMORY, e);
        }
    }

    /**
     * Adds a chat message to the project's memory
     * @param project The project to add the message to
     * @param chatMessage The message to add
     */
    public void addMessage(@NotNull Project project, ChatMessage chatMessage) {
        addMessageByKey(project.getLocationHash(), chatMessage);
    }

    /**
     * Adds a chat message to the memory identified by key
     * @param memoryKey The memory key
     * @param chatMessage The message to add
     */
    public void addMessageByKey(@NotNull String memoryKey, ChatMessage chatMessage) {
        try {
            MessageWindowChatMemory memory = projectConversations.get(memoryKey);
            if (memory != null) {
                // Check for duplicate messages to prevent adding the same message multiple times
                List<ChatMessage> currentMessages = memory.messages();
                if (!currentMessages.isEmpty()) {
                    ChatMessage lastMessage = currentMessages.get(currentMessages.size() - 1);

                    // Check if the last message is of the same type and has the same content
                    if (lastMessage.getClass().equals(chatMessage.getClass()) &&
                        lastMessage.toString().equals(chatMessage.toString())) {
                        log.warn("Prevented duplicate message addition for key: {}", memoryKey);
                        return; // Skip adding duplicate message
                    }
                }

                // Log the message content for debugging XML issues
                log.debug("Adding message to memory - Type: {}, Content: {}",
                        chatMessage.getClass().getSimpleName(), chatMessage);

                memory.add(chatMessage);
                log.debug("Successfully added message to key: {}, message type: {}", memoryKey, chatMessage.getClass().getSimpleName());
            } else {
                throw new MemoryException(CHAT_MEMORY_NOT_INITIALIZED_FOR_PROJECT + memoryKey);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException(FAILED_TO_ADD_MESSAGE_TO_MEMORY, e);
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
        return getMessagesByKey(project.getLocationHash());
    }

    /**
     * Gets all messages for a memory key
     * @param memoryKey The memory key
     * @return List of chat messages
     */
    public List<ChatMessage> getMessagesByKey(@NotNull String memoryKey) {
        try {
            MessageWindowChatMemory memory = projectConversations.get(memoryKey);
            if (memory != null) {
                return memory.messages();
            } else {
                throw new MemoryException(CHAT_MEMORY_NOT_INITIALIZED_FOR_PROJECT + memoryKey);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException(FAILED_TO_GET_MESSAGES_FROM_MEMORY, e);
            }
            throw e;
        }
    }

    /**
     * Checks if a memory entry exists for the given key
     * @param memoryKey The memory key to check
     * @return true if memory exists, false otherwise
     */
    public boolean hasMemory(@NotNull String memoryKey) {
        return projectConversations.containsKey(memoryKey);
    }

    /**
     * Checks if memory is empty for a project
     * @param project The project to check
     * @return true if empty, false otherwise
     */
    public boolean isEmpty(@NotNull Project project) {
        return isEmptyByKey(project.getLocationHash());
    }

    /**
     * Checks if memory is empty for a given key
     * @param memoryKey The memory key to check
     * @return true if empty, false otherwise
     */
    public boolean isEmptyByKey(@NotNull String memoryKey) {
        try {
            MessageWindowChatMemory memory = projectConversations.get(memoryKey);
            if (memory != null) {
                return memory.messages().isEmpty();
            } else {
                return true;
            }
        } catch (Exception e) {
            throw new MemoryException(FAILED_TO_CHECK_IF_MEMORY_IS_EMPTY, e);
        }
    }

    /**
     * Removes the last message from a project's memory
     * @param project The project to remove from
     */
    public void removeLastMessage(@NotNull Project project) {
        removeLastMessageByKey(project.getLocationHash());
    }

    /**
     * Removes the last message from memory identified by key
     * @param memoryKey The memory key
     */
    public void removeLastMessageByKey(@NotNull String memoryKey) {
        try {
            MessageWindowChatMemory memory = projectConversations.get(memoryKey);
            if (memory != null) {
                List<ChatMessage> messages = memory.messages();
                if (!messages.isEmpty()) {
                    ChatMessage lastMessage = messages.get(messages.size() - 1);
                    memory.clear();

                    for (int i = 0; i < messages.size() - 1; i++) {
                        memory.add(messages.get(i));
                    }

                    log.debug("Removed last message ({}) from key: {}",
                            lastMessage.getClass().getSimpleName(), memoryKey);
                }
            } else {
                throw new MemoryException(CHAT_MEMORY_NOT_INITIALIZED_FOR_PROJECT + memoryKey);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException(FAILED_TO_REMOVE_LAST_MESSAGE_FROM_MEMORY, e);
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
        removeMessagesByKey(project.getLocationHash(), messagesToRemove);
    }

    /**
     * Removes specific messages from memory identified by key
     * @param memoryKey The memory key
     * @param messagesToRemove The messages to remove
     */
    public void removeMessagesByKey(@NotNull String memoryKey, List<ChatMessage> messagesToRemove) {
        try {
            MessageWindowChatMemory memory = projectConversations.get(memoryKey);
            if (memory != null) {
                List<ChatMessage> currentMessages = memory.messages();

                if (!currentMessages.isEmpty() && !messagesToRemove.isEmpty()) {
                    memory.clear();

                    for (ChatMessage message : currentMessages) {
                        if (!messagesToRemove.contains(message)) {
                            memory.add(message);
                        }
                    }

                    log.debug("Removed {} messages from key: {}",
                            messagesToRemove.size(), memoryKey);
                }
            } else {
                throw new MemoryException(CHAT_MEMORY_NOT_INITIALIZED_FOR_PROJECT + memoryKey);
            }
        } catch (Exception e) {
            if (!(e instanceof MemoryException)) {
                throw new MemoryException(FAILED_TO_REMOVE_MESSAGES_FROM_MEMORY, e);
            }
            throw e;
        }
    }

    /**
     * Completely removes a memory entry by key (used for tab cleanup)
     * @param memoryKey The memory key to remove
     */
    public void removeByKey(@NotNull String memoryKey) {
        MessageWindowChatMemory memory = projectConversations.remove(memoryKey);
        if (memory != null) {
            memory.clear();
            log.debug("Removed memory for key: {}", memoryKey);
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
            return projectConversations.get(projectHash.toString());
        } catch (Exception e) {
            throw new MemoryException(FAILED_TO_GET_CHAT_MEMORY_FOR_PROJECT_HASH + projectHash, e);
        }
    }
}