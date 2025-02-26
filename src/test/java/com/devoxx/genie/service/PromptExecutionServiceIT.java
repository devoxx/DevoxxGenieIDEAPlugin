package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.concurrent.CompletableFuture;

/**
 * Integration tests for {@link PromptExecutionService}.
 *
 * <p>Note: These tests require specific environment variables to be set before execution.
 * If the required environment variable isn't set, the execution of related test will be skipped.</p>
 *
 * <ul>
 *     <li>{@link #testExecuteQueryOpenAI()} requires `OPENAI_API_KEY`</li>
 *     <li>{@link #testExecuteQueryAnthropic()} requires `ANTHROPIC_API_KEY`</li>
 *     <li>{@link #testExecuteQueryGemini()} requires `GEMINI_API_KEY`</li>
 *     <li>{@link #testExecuteQueryMistral()} requires `MISTRAL_API_KEY`</li>
 *     <li>{@link #testExecuteQueryDeepInfra()} requires `DEEPINFRA_API_KEY`</li>
 * </ul>
 */
class PromptExecutionServiceIT extends AbstractLightPlatformTestCase {

    private PromptExecutionService promptExecutionService;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        promptExecutionService = new PromptExecutionService();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testExecuteQueryOpenAI() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.OpenAI)
            .modelName("gpt-3.5-turbo")
            .displayName("GPT-3.5 Turbo")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .inputMaxTokens(4096)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void testExecuteQueryAnthropic() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.Anthropic)
            .modelName("claude-3-5-sonnet-20240620")
            .displayName("claude-3-5-sonnet-20240620")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .inputMaxTokens(100000)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testExecuteQueryGemini() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.Google)
            .modelName("gemini-1.5-flash")
            .displayName("Gemini 1.5 Flash")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .inputMaxTokens(32768)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MISTRAL_API_KEY", matches = ".+")
    void testExecuteQueryMistral() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.Mistral)
            .modelName("mistral-medium")
            .displayName("Mistral Medium")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .inputMaxTokens(32768)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPINFRA_API_KEY", matches = ".+")
    void testExecuteQueryDeepInfra() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.DeepInfra)
            .modelName("mistralai/Mixtral-8x7B-Instruct-v0.1")
            .displayName("Mixtral 8x7B")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .inputMaxTokens(32768)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    private ChatLanguageModel createChatModel(@NotNull LanguageModel languageModel) {
        return switch (languageModel.getProvider()) {
            case OpenAI -> OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case Anthropic -> AnthropicChatModel.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case Google -> GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case Mistral -> MistralAiChatModel.builder()
                .apiKey(System.getenv("MISTRAL_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case DeepInfra -> OpenAiChatModel.builder()
                .baseUrl("https://api.deepinfra.com/v1/openai")
                .apiKey(System.getenv("DEEPINFRA_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            default -> throw new IllegalArgumentException("Unsupported model provider");
        };
    }

    private void verifyResponse(ChatLanguageModel chatModel, LanguageModel languageModel) {
        ChatMessageContext context = ChatMessageContext.builder()
            .userPrompt("What is the capital of Belgium?")
            .chatLanguageModel(chatModel)
            .languageModel(languageModel)
            .project(getProject())
            .executionTimeMs(0)
            .cost(0).build();

        CompletableFuture<ChatResponse> chatResponse = promptExecutionService.executeQuery(context);
        assertNotNull(chatResponse);

        var response = chatResponse.join();
        assertNotNull(response);
        assertFalse(response.aiMessage().text().isEmpty());
        assertTrue(response.aiMessage().text().toLowerCase().contains("brussels"));
    }
}
