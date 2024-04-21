package com.devoxx.genie.service;

import com.devoxx.genie.model.ChatInteraction;
import com.devoxx.genie.ui.component.PlaceholderTextArea;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

//public class ChatMessageHistoryService {
//
//    private final List<ChatInteraction> chatHistory = new ArrayList<>();
//    private int chatIndex = -1;
//
//    private final JButton nextButton;
//    private final JButton prevButton;
//    private final JButton clearButton;
//    private final JLabel infoLabel;
//    private final PlaceholderTextArea promptInputField;
//    private final JEditorPane promptOutputArea;
//
//    public ChatMessageHistoryService(JButton prevButton,
//                                     JButton nextButton,
//                                     JLabel infoLabel,
//                                     JButton clearButton,
//                                     PlaceholderTextArea promptInputArea,
//                                     JEditorPane promptOutputArea) {
//        this.nextButton = nextButton;
//        this.prevButton = prevButton;
//        this.infoLabel = infoLabel;
//        this.clearButton = clearButton;
//        this.promptOutputArea = promptOutputArea;
//        this.promptInputField = promptInputArea;
//        this.clearButton.addActionListener(e -> clearHistory());
//        clearButton.setEnabled(false);
//
//        updateButtons();
//    }
//
//    private void updateButtons() {
//        nextButton.setEnabled(chatIndex < chatHistory.size() - 1);
//        prevButton.setEnabled(chatIndex > 0);
//        clearButton.setEnabled(!chatHistory.isEmpty());
//        if (chatHistory.size() > 1) {
//            infoLabel.setText(String.format("%d/%d", (chatIndex + 1), chatHistory.size()));
//        } else {
//            infoLabel.setText("");
//        }
//    }
//
//    /**
//     * Add a chat message to the history.
//     * @param llmProvider the LLM provider
//     * @param modelName the model name
//     * @param question the chat question
//     * @param response the chat response
//     */
//    public void addMessage(String llmProvider, String modelName, String question, String response) {
//        chatHistory.add(new ChatInteraction(llmProvider, modelName, question, response));
//        chatIndex++;
//        updateButtons();
//    }
//
//    /**
//     * Set the previous message in the chat history.
//     */
//    public void setPreviousMessage() {
//        if (chatIndex > 0) {
//            chatIndex--;
//            ChatInteraction chatInteraction = chatHistory.get(chatIndex);
//            promptInputField.setText(chatInteraction.getQuestion());
//            promptOutputArea.setText(chatInteraction.getResponse());
//            updateButtons();
//        }
//    }
//
//    /**
//     * Set the next message in the chat history.
//     */
//    public void setNextMessage() {
//        if (chatIndex < chatHistory.size() - 1) {
//            chatIndex++;
//            ChatInteraction chatInteraction = chatHistory.get(chatIndex);
//            promptInputField.setText(chatInteraction.getQuestion());
//            promptOutputArea.setText(chatInteraction.getResponse());
//            updateButtons();
//        }
//    }
//
//    /**
//     * Clear the chat history, reset chat index and disable related buttons.
//     */
//    public void clearHistory() {
//        chatHistory.clear();
//        chatIndex = -1;
//        promptInputField.setText("");
//        updateButtons();
//    }
//
//    public int getTotalChats() {
//        return chatHistory.size();
//    }
//
//    public int getCurrentIndex() {
//        return chatIndex;
//    }
//}

public class ChatMessageHistoryService {

    private final List<ChatHistoryObserver> observers = new ArrayList<>();
    private final List<ChatInteraction> chatHistory = new ArrayList<>();
    private int chatIndex = -1;
    private ChatInteraction currentChatInteraction;

    public void addObserver(ChatHistoryObserver observer) {
        observers.add(observer);
    }

    private void notifyHistoryUpdated() {
        for (ChatHistoryObserver observer : observers) {
            observer.onHistoryUpdated(chatIndex, chatHistory.size());
        }
    }

    public void setPreviousMessage() {
        if (chatIndex > 0) {
            chatIndex--;
            currentChatInteraction = chatHistory.get(chatIndex);
            notifyHistoryUpdated();  // Observer pattern implementation
        }
    }

    public void setNextMessage() {
        if (chatIndex < chatHistory.size() - 1) {
            chatIndex++;
            currentChatInteraction = chatHistory.get(chatIndex);
            notifyHistoryUpdated();
        }
    }

    public int getChatIndex() {
        return chatIndex;
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

    public void clearHistory() {
        chatHistory.clear();
        chatIndex = -1;
    }
}
