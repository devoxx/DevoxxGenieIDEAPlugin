package com.devoxx.genie.service.prompt.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

public class TaskPreservingMessageWindowChatMemory implements ChatMemory {

    private final Object id;

    private final int maxMessages;

    private final ChatMemoryStore store;

    private TaskPreservingMessageWindowChatMemory(Builder builder) {
        this.id = ensureNotNull(builder.id, "id");
        this.maxMessages = ensureGreaterThanZero(builder.maxMessages, "maxMessages");
        this.store = ensureNotNull(builder.store(), "store");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Object id() {
        return this.id;
    }

    @Override
    public void add(ChatMessage message) {
        List<ChatMessage> messages = messages();
        if (appendMessage(messages, message)) {
            this.store.updateMessages(this.id, messages);
        }
    }

    @Override
    public CompletableFuture<Void> addAsync(List<ChatMessage> messagesToAdd) {
        return this.store.getMessagesAsync(this.id).thenCompose(stored -> {
            List<ChatMessage> messages = windowed(stored);
            boolean changed = false;
            for (ChatMessage message : messagesToAdd) {
                changed |= appendMessage(messages, message);
            }
            if (changed) {
                return this.store.updateMessagesAsync(this.id, messages);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private boolean appendMessage(List<ChatMessage> messages, ChatMessage message) {
        if (message instanceof SystemMessage) {
            Optional<SystemMessage> systemMessage = SystemMessage.findFirst(messages);
            if (systemMessage.isPresent()) {
                if (systemMessage.get().equals(message)) {
                    return false;
                }
                messages.remove(systemMessage.get());
            }
        }
        messages.add(message);
        ensureCapacity(messages, this.maxMessages);
        return true;
    }

    @Override
    public void set(Iterable<ChatMessage> iterable) {
        List<ChatMessage> messages = new ArrayList<>();
        iterable.forEach(messages::add);
        ensureCapacity(messages, this.maxMessages);
        this.store.updateMessages(this.id, messages);
    }

    @Override
    public CompletableFuture<Void> setAsync(List<ChatMessage> messages) {
        try {
            List<ChatMessage> windowed = new ArrayList<>(messages);
            ensureCapacity(windowed, this.maxMessages);
            return this.store.updateMessagesAsync(this.id, windowed);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return windowed(this.store.getMessages(this.id));
    }

    @Override
    public CompletableFuture<List<ChatMessage>> messagesAsync() {
        return this.store.getMessagesAsync(this.id).thenApply(this::windowed);
    }

    @Override
    public void clear() {
        this.store.deleteMessages(this.id);
    }

    private List<ChatMessage> windowed(List<ChatMessage> stored) {
        List<ChatMessage> messages = new LinkedList<>(stored);
        ensureCapacity(messages, this.maxMessages);
        return messages;
    }

    private static void ensureCapacity(List<ChatMessage> messages, int maxMessages) {
        while (messages.size() > maxMessages) {
            int messageToEvictIndex = indexToEvict(messages);
            ChatMessage evictedMessage = messages.remove(messageToEvictIndex);
            if (evictedMessage instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                while (messages.size() > messageToEvictIndex
                        && messages.get(messageToEvictIndex) instanceof ToolExecutionResultMessage) {
                    messages.remove(messageToEvictIndex);
                }
            }
        }
    }

    private static int indexToEvict(List<ChatMessage> messages) {
        int oldest = messages.get(0) instanceof SystemMessage ? 1 : 0;
        if (!isTaskOfRunningToolLoop(messages, oldest)) {
            return oldest;
        }
        int oldestFinishedToolExchange = oldestFinishedToolExchangeAfter(messages, oldest);
        return oldestFinishedToolExchange >= 0 ? oldestFinishedToolExchange : oldest;
    }

    private static boolean isTaskOfRunningToolLoop(List<ChatMessage> messages, int index) {
        if (!(messages.get(index) instanceof UserMessage)) {
            return false;
        }
        boolean toolLoopStarted = false;
        for (int i = index + 1; i < messages.size(); i++) {
            if (messages.get(i) instanceof AiMessage aiMessage) {
                if (!aiMessage.hasToolExecutionRequests()) {
                    return false;
                }
                toolLoopStarted = true;
            }
        }
        return toolLoopStarted;
    }

    private static int oldestFinishedToolExchangeAfter(List<ChatMessage> messages, int index) {
        int latestAiMessage = indexOfLatestAiMessage(messages);
        for (int i = index + 1; i < latestAiMessage; i++) {
            if (messages.get(i) instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfLatestAiMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AiMessage) {
                return i;
            }
        }
        return -1;
    }

    public static class Builder {

        private Object id = dev.langchain4j.service.memory.ChatMemoryService.DEFAULT;

        private Integer maxMessages;

        private ChatMemoryStore store;

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder maxMessages(Integer maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public Builder chatMemoryStore(ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        private ChatMemoryStore store() {
            return this.store != null ? this.store : new InMemoryChatMemoryStore();
        }

        public TaskPreservingMessageWindowChatMemory build() {
            return new TaskPreservingMessageWindowChatMemory(this);
        }
    }

    public static TaskPreservingMessageWindowChatMemory withMaxMessages(int maxMessages) {
        return builder().maxMessages(maxMessages).build();
    }
}
