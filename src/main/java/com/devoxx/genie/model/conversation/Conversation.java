package com.devoxx.genie.model.conversation;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Setter
@Getter
public class Conversation {
    private String id;
    private String timestamp;
    private String title;
    private String llmProvider;
    private String modelName;
    private Boolean apiKeyUsed;
    private Long inputCost;
    private Long outputCost;
    private Integer contextWindow;
    private long executionTimeMs;
    private List<ChatMessage> messages = new ArrayList<>();
}
