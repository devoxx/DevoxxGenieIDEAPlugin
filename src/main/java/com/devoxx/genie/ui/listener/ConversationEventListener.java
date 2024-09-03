package com.devoxx.genie.ui.listener;

import com.devoxx.genie.model.request.ChatMessageContext;

public interface ConversationEventListener {
    void onNewConversation(ChatMessageContext chatMessageContext);
}
