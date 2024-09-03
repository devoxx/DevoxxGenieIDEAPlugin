package com.devoxx.genie.service;

import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

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

    public void addConversation(Conversation conversation) {
        myState.conversations.add(conversation);
        saveState();
    }

    public @NotNull List<Conversation> getConversations() {
        return new ArrayList<>(myState.conversations);
    }

    public void removeConversation(Conversation conversation) {
        myState.conversations.remove(conversation);
        saveState();
    }

    public void clearAllConversations() {
        myState.conversations.clear();
        saveState();
    }

    private void saveState() {
        ApplicationManager.getApplication().invokeLater(() ->
            ApplicationManager.getApplication().saveSettings()
        );
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class State {
        public List<Conversation> conversations = new ArrayList<>();
    }
}
