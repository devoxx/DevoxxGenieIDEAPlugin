package com.devoxx.genie.ui.listener;

import com.devoxx.genie.model.request.ChatMessageContext;

public interface ChatChangeListener {

    void removeMessagePair(ChatMessageContext chatMessageContext);
}
