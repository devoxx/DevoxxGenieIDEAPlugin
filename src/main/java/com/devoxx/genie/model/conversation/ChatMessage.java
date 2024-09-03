package com.devoxx.genie.model.conversation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessage {
    private boolean isUser;
    private String content;
    private String timestamp;
}
