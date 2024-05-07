package com.devoxx.genie.model;

import com.devoxx.genie.model.request.ChatMessageContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ChatInteraction {
    private ChatMessageContext chatMessageContext;
    private String response;
}
