package com.devoxx.genie.service;

import com.devoxx.genie.model.ChatInteraction;
import com.devoxx.genie.ui.component.PlaceholderTextArea;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageHistoryService {

    private final List<ChatInteraction> chatHistory = new ArrayList<>();
    private int chatIndex = -1;

    private final JButton nextButton;
    private final JButton prevButton;
    private final JButton clearButton;
    private final JLabel infoLabel;
    private final PlaceholderTextArea promptInputField;

    public ChatMessageHistoryService(JButton prevButton,
                                     JButton nextButton,
                                     JLabel infoLabel,
                                     JButton clearButton,
                                     PlaceholderTextArea promptInputField) {
        this.nextButton = nextButton;
        this.prevButton = prevButton;
        this.infoLabel = infoLabel;
        this.clearButton = clearButton;
        this.promptInputField = promptInputField;
        this.clearButton.addActionListener(e -> clearHistory());
        clearButton.setEnabled(false);

        updateButtons();
    }

    private void updateButtons() {
        nextButton.setEnabled(chatIndex < chatHistory.size() - 1);
        prevButton.setEnabled(chatIndex > 0);
        clearButton.setEnabled(!chatHistory.isEmpty());
        if (chatHistory.size() > 1) {
            infoLabel.setText(String.format("%d/%d", (chatIndex + 1), chatHistory.size()));
        } else {
            infoLabel.setText("");
        }
    }

    /**
     * Add a chat message to the history.
     * @param question the chat question
     * @param response the chat response
     */
    public void addMessage(String question, String response) {
        chatHistory.add(new ChatInteraction(question, response));
        chatIndex++;
        updateButtons();
    }

    /**
     * Set the previous message in the chat history.
     * @param chatHistoryPane The chat history pane.
     */
    public void setPreviousMessage(JEditorPane chatHistoryPane) {
        if (chatIndex > 0) {
            chatIndex--;
            ChatInteraction chatInteraction = chatHistory.get(chatIndex);
            promptInputField.setText(chatInteraction.getQuestion());
            chatHistoryPane.setText(chatInteraction.getResponse());
            updateButtons();
        }
    }

    /**
     * Set the next message in the chat history.
     * @param chatHistoryPane The chat history pane.
     */
    public void setNextMessage(JEditorPane chatHistoryPane) {
        if (chatIndex < chatHistory.size() - 1) {
            chatIndex++;
            ChatInteraction chatInteraction = chatHistory.get(chatIndex);
            promptInputField.setText(chatInteraction.getQuestion());
            chatHistoryPane.setText(chatInteraction.getResponse());
            updateButtons();
        }
    }

    /**
     * Clear the chat history, reset chat index and disable related buttons.
     */
    public void clearHistory() {
        chatHistory.clear();
        chatIndex = -1;
        promptInputField.setText("");
        updateButtons();
    }
}
