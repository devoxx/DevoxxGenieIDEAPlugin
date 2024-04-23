package com.devoxx.genie.service;

import com.devoxx.genie.model.ChatInteraction;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageHistoryService {

    private final List<ChatHistoryObserver> observers = new ArrayList<>();
    private final List<ChatInteraction> chatHistory = new ArrayList<>();
    @Getter
    private int chatIndex = -1;

    public void addObserver(ChatHistoryObserver observer) {
        observers.add(observer);
    }

    private void notifyHistoryUpdated() {
        for (ChatHistoryObserver observer : observers) {
            observer.onChatHistoryUpdated(chatIndex, chatHistory.size());
        }
    }

    public void setPreviousMessage() {
        if (chatIndex > 0) {
            chatIndex--;
            notifyHistoryUpdated();  // Observer pattern implementation
        }
    }

    public void setNextMessage() {
        if (chatIndex < chatHistory.size() - 1) {
            chatIndex++;
            notifyHistoryUpdated();
        }
    }

    public int getChatHistorySize() {
        return chatHistory.size();
    }

    public ChatInteraction getCurrentChatInteraction() {
        return chatHistory.get(chatIndex);
    }

    public void addMessage(String llmProvider,
                           String modelName,
                           String question,
                           String response) {
        chatHistory.add(new ChatInteraction(llmProvider, modelName, question, response));
        chatIndex++;
        notifyHistoryUpdated();
    }
}
