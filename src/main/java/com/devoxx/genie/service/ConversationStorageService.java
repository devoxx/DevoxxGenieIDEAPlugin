package com.devoxx.genie.service;

import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@State(name = "DevoxxGenieConversationStorage", storages = @Storage("devoxxgenie-conversations.xml"))
public final class ConversationStorageService implements PersistentStateComponent<ConversationStorageService.State> {

    private final State myState = new State();

    public static ConversationStorageService getInstance() {
        return ApplicationManager.getApplication().getService(ConversationStorageService.class);
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public void addConversation(@NotNull Project project, Conversation conversation) {
        if (conversation != null &&
            (!conversation.getMessages().isEmpty() || (conversation.getTitle() != null && !conversation.getTitle().trim().isEmpty()))) {
            myState.conversations.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).add(conversation);
            saveState();
        }
    }

    public @NotNull List<Conversation> getConversations(@NotNull Project project) {
        return new ArrayList<>(myState.conversations.getOrDefault(project.getLocationHash(), new ArrayList<>()));
    }

    public void removeConversation(@NotNull Project project, Conversation conversation) {
        myState.conversations.getOrDefault(project.getLocationHash(), new ArrayList<>()).remove(conversation);
        saveState();
    }

    public void clearAllConversations(@NotNull Project project) {
        myState.conversations.remove(project.getLocationHash());
        saveState();
    }

    private void saveState() {
        ApplicationManager.getApplication().invokeLater(() ->
            ApplicationManager.getApplication().saveSettings()
        );
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class State {
        public Map<String, List<Conversation>> conversations = new HashMap<>();
    }
}
