package com.devoxx.genie.model.gemini;

import com.devoxx.genie.model.gemini.model.Content;
import com.devoxx.genie.model.gemini.model.Part;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static java.time.Duration.ofSeconds;

public class GeminiChatModel implements ChatLanguageModel {

    private final GeminiClient client;

    private final Integer maxRetries;

    private final GeminiMessageRequest messageRequest = GeminiMessageRequest.builder().build();

    @Builder
    public GeminiChatModel(String apiKey,
                           String modelName,
                           Duration timeout,
                           Double temperature,  // unused for now
                           int maxTokens,       // unused for now
                           int maxRetries) {
        this.maxRetries = maxRetries;

        this.client = GeminiClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .apiKey(apiKey)
            .modelName(modelName)
            .timeout(getOrDefault(timeout, ofSeconds(60)))
            .build();

//        messageRequest.setGenerationConfig(GenerationConfig.builder()
//                        .maxOutputTokens(maxTokens)
//                        .temperature(temperature)
//                        .build());

//        messageRequest.setSystemInstruction(SystemInstruction.builder()
//                .parts(List.of(Part.builder().text("Always return response in markdown").build()))
//                .build());
    }

    @Override
    public Response<AiMessage> generate(@NotNull List<ChatMessage> messages) {
        ensureNotEmpty(messages, "messages");

        List<Content> userMessages = new ArrayList<>();

        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage systemMessage) {
                userMessages.add(createMessage("model", systemMessage.text()));
            } else if (message instanceof UserMessage userMessage) {
                userMessages.add(createMessage("user", userMessage.singleText()));
            }
        }

        messageRequest.setContents(userMessages);

        GeminiCompletionResponse completionResponse = withRetry(() -> client.completion(messageRequest), maxRetries);

        String response = completionResponse.getCandidates().get(0).getContent().getParts().get(0).getText();

        return new Response<>(new AiMessage(response));
    }

    /**
     * Create a message for the Gemini API
     *
     * @param role    the user role
     * @param message a chat message
     */
    private static Content createMessage(String role, String message) {
        return Content.builder()
            .role(role)
            .parts(List.of(Part.builder()
                .text(message)
                .build()))
            .build();
    }
}
