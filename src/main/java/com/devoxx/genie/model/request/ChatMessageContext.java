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
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChatMessageContext {
    private final LocalDateTime createdOn = LocalDateTime.now();
    private String id;
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
    private String commandName;     // Custom command name for the prompt, for example /test, /review etc.
    private double cost;
    private boolean semanticSearchActivated;
    private boolean gitDiffActivated;
    private boolean webSearchActivated;

    @Builder.Default
    private boolean webSearchRequested = false;

    @Getter
    @Setter
    private List<SemanticFile> semanticReferences;

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
