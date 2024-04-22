package com.devoxx.genie.service;

public interface ChatHistoryObserver {
    void onChatHistoryUpdated(int currentIndex, int totalMessages);
}
