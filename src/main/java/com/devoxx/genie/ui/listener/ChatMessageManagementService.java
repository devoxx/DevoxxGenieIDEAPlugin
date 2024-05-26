package com.devoxx.genie.ui.listener;

import com.devoxx.genie.model.request.ChatMessageContext;

public interface ChatMessageManagementService {

    void removeMessagePair(ChatMessageContext chatMessageContext);
}
