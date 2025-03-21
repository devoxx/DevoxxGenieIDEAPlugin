package com.devoxx.genie.model.request;

import com.devoxx.genie.model.LanguageModel;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the context of a chat message with all the necessary information.
 */
@ToString
@Data
@Builder
public class ChatMessageContext {
    private final LocalDateTime createdOn = LocalDateTime.now();
    private String id;
    private Project project;
    private Integer timeout;
    private String userPrompt;          // The user prompt
    private UserMessage userMessage;    // The user message
    private AiMessage aiMessage;        // The LLM response message
    private String filesContext;             // The context of the prompt
    private EditorInfo editorInfo;      // The editor info
    private LanguageModel languageModel;
    private ChatLanguageModel chatLanguageModel;
    private StreamingChatLanguageModel streamingChatLanguageModel;
    private long executionTimeMs;
    private TokenUsage tokenUsage;
    private String commandName;     // Custom command name for the prompt, for example /test, /review etc.
    private double cost;
    private boolean mcpActivated;
    private boolean ragActivated;
    private boolean gitDiffActivated;
    private boolean webSearchActivated;

    @Builder.Default
    private boolean webSearchRequested = false;

    @Getter
    @Setter
    private List<SemanticFile> semanticReferences;

    public void setTokenUsageAndCost(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
        if (this.tokenUsage != null) {
            this.cost = (tokenUsage.inputTokenCount() * languageModel.getInputCost() +
                tokenUsage.outputTokenCount() * languageModel.getOutputCost()) / 1_000_000.0;
        }
    }
}
