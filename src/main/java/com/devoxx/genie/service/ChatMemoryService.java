package com.devoxx.genie.service;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.listener.ChatMemorySizeListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatMemoryService implements ChatMemorySizeListener {

    private final Map<Project, MessageWindowChatMemory> projectMemories = new ConcurrentHashMap<>();
    private final InMemoryChatMemoryStore inMemoryChatMemoryStore = new InMemoryChatMemoryStore();

    public static ChatMemoryService getInstance() {
        return ApplicationManager.getApplication().getService(ChatMemoryService.class);
    }

    public void init(Project project) {
        createChatMemory(project, DevoxxGenieSettingsServiceProvider.getInstance().getChatMemorySize());
        createChangeListener();
    }

    private void createChangeListener() {
        ApplicationManager.getApplication().getMessageBus()
            .connect()
            .subscribe(AppTopics.CHAT_MEMORY_SIZE_TOPIC, this);
    }

    public void clear(Project project) {
        projectMemories.get(project).clear();
    }

    public void add(Project project, ChatMessage chatMessage) {
        projectMemories.get(project).add(chatMessage);
    }

    public void remove(@NotNull ChatMessageContext chatMessageContext) {
        Project project = chatMessageContext.getProject();
        List<ChatMessage> messages = projectMemories.get(project).messages();
        messages.remove(chatMessageContext.getAiMessage());
        messages.remove(chatMessageContext.getUserMessage());
        projectMemories.get(project).clear();
        messages.forEach(message -> add(project, message));
    }

    public void removeLast(Project project) {
        List<ChatMessage> messages = projectMemories.get(project).messages();
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
            projectMemories.get(project).clear();
            messages.forEach(message -> add(project, message));
        }
    }

    public List<ChatMessage> messages(Project project) {
        return projectMemories.get(project).messages();
    }

    public boolean isEmpty(Project project) {
        return projectMemories.get(project).messages().isEmpty();
    }

    @Override
    public void onChatMemorySizeChanged(int chatMemorySize) {
        projectMemories.forEach((project, memory) -> createChatMemory(project, chatMemorySize));
    }

    private void createChatMemory(Project project, int chatMemorySize) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .id("devoxxgenie-" + project.getLocationHash())
            .chatMemoryStore(inMemoryChatMemoryStore)
            .maxMessages(chatMemorySize)
            .build();
        projectMemories.put(project, chatMemory);
    }
}
