package com.devoxx.genie.service;

import org.junit.Test;

import java.io.IOException;

public class ChatMessageHistoryServiceTest {

    @Test
    public void testAddingChatMessage() throws IOException {
        ChatMessageHistoryService chatMessageHistoryService = new ChatMessageHistoryService(null, null, null, null);
        chatMessageHistoryService.addMessage("test");

    }
}
