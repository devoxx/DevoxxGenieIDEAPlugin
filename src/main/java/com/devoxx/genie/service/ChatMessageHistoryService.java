package com.devoxx.genie.service;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageHistoryService {

    private final List<String> chatHistory = new ArrayList<>();
    private int chatIndex = -1;

    private final JButton nextButton;
    private final JButton prevButton;
    private final JButton clearButton;
    private final JLabel infoLabel;

    public ChatMessageHistoryService(JButton prevButton,
                                     JButton nextButton,
                                     JLabel infoLabel,
                                     JButton clearButton) {
        this.nextButton = nextButton;
        this.prevButton = prevButton;
        this.infoLabel = infoLabel;
        this.clearButton = clearButton;
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

    public void addMessage(String message) {
        chatHistory.add(message);
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
            chatHistoryPane.setText(chatHistory.get(chatIndex));
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
            chatHistoryPane.setText(chatHistory.get(chatIndex));
            updateButtons();
        }
    }

    public void clearHistory() {
        chatHistory.clear();
        chatIndex = -1;
        updateButtons();
    }
}
