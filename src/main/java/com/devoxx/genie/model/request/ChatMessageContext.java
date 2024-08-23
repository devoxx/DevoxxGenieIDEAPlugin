package com.devoxx.genie.model.request;

import com.devoxx.genie.model.LanguageModel;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageContext {
    private final LocalDateTime createdOn = LocalDateTime.now();
    private String name;
    private Project project;
    private Integer timeout;
    private String userPrompt;
    private UserMessage userMessage;
    private AiMessage aiMessage;
    private String context;
    private EditorInfo editorInfo;
    private LanguageModel languageModel;
    private ChatLanguageModel chatLanguageModel;
    private StreamingChatLanguageModel streamingChatLanguageModel;
    private int totalFileCount;
    private long executionTimeMs;
    private TokenUsage tokenUsage;
    private double cost;

    @Builder.Default
    private boolean webSearchRequested = false;

    @Builder.Default
    private boolean fullProjectContextAdded = false;

    // Custom method
    public boolean hasFiles() {
        return totalFileCount > 0;
    }

    public void setTokenUsageAndCost(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
        if (this.tokenUsage != null) {
            this.cost = (tokenUsage.inputTokenCount() * languageModel.getInputCost() +
                         tokenUsage.outputTokenCount() * languageModel.getOutputCost()) / 1_000_000.0;
        }
    }
}
