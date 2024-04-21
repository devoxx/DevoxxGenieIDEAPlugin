package com.devoxx.genie.service;

public interface ChatHistoryObserver {
    void onHistoryUpdated(int currentIndex, int totalMessages);
}
