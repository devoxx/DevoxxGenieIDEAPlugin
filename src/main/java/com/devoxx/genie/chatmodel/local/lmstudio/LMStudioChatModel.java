package com.devoxx.genie.chatmodel.local.lmstudio;

import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.shared.Usage;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.model.openai.InternalOpenAiHelper.*;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;

/**
 * Based on <a href="https://localai.io/features/text-generation/">LocalAI documentation</a>.
 * But the token usage from the response is now used.
 */
public class LMStudioChatModel extends AbstractChatLanguageModel {

    private final OpenAiClient client;
    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final Integer maxTokens;
    private final Integer maxRetries;

    @Builder
    public LMStudioChatModel(String baseUrl,
                             String modelName,
                             Double temperature,
                             Double topP,
                             Integer maxTokens,
                             Duration timeout,
                             Integer maxRetries,
                             Boolean logRequests,
                             Boolean logResponses) {

        temperature = temperature == null ? 0.7 : temperature;
        timeout = timeout == null ? ofSeconds(60) : timeout;
        maxRetries = maxRetries == null ? 3 : maxRetries;

        this.client = OpenAiClient.builder()
            .openAiApiKey("ignored")
            .baseUrl(ensureNotBlank(baseUrl, "baseUrl"))
            .callTimeout(timeout)
            .connectTimeout(timeout)
            .readTimeout(timeout)
            .writeTimeout(timeout)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .build();
        this.modelName = ensureNotBlank(modelName, "modelName");
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.maxRetries = maxRetries;
    }

    public ChatResponse chat(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return chat(messages, toolSpecifications, null);
    }

    public ChatResponse chat(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        return chat(messages, singletonList(toolSpecification), toolSpecification);
    }

    private @NotNull ChatResponse chat(List<ChatMessage> messages,
                                       List<ToolSpecification> toolSpecifications,
                                       ToolSpecification toolThatMustBeExecuted
    ) {
        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
            .model(modelName)
            .messages(toOpenAiMessages(messages))
            .temperature(temperature)
            .topP(topP)
            .maxCompletionTokens(maxTokens);

        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.tools(toTools(toolSpecifications, false));
        }
        if (toolThatMustBeExecuted != null) {
            requestBuilder.toolChoice(toolThatMustBeExecuted.name());
        }

        ChatCompletionRequest request = requestBuilder.build();

        ChatCompletionResponse response = withRetry(() -> client.chatCompletion(request).execute(), maxRetries);

        Usage usage = response.usage();

        return ChatResponse.builder()
                .aiMessage(aiMessageFrom(response))
                .tokenUsage(new TokenUsage(usage.promptTokens(), usage.completionTokens()))
                .finishReason(finishReasonFrom(response.choices().get(0).finishReason()))
                .build();
    }
}
