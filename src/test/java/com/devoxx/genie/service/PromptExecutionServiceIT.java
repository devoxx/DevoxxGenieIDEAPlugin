package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PromptExecutionServiceIT extends AbstractLightPlatformTestCase {

    private static Dotenv dotenv;
    private PromptExecutionService promptExecutionService;

    @BeforeAll
    static void loadEnvironment() {
        dotenv = Dotenv.load();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockSettingsState();
        promptExecutionService = new PromptExecutionService();
    }

    private void mockSettingsState() {
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getOpenAIKey()).thenReturn(dotenv.get("OPENAI_API_KEY"));
        when(settingsStateMock.getAnthropicKey()).thenReturn(dotenv.get("ANTHROPIC_API_KEY"));
        when(settingsStateMock.getGeminiKey()).thenReturn(dotenv.get("GEMINI_API_KEY"));
        when(settingsStateMock.getMistralKey()).thenReturn(dotenv.get("MISTRAL_API_KEY"));
        when(settingsStateMock.getDeepInfraKey()).thenReturn(dotenv.get("DEEPINFRA_API_KEY"));

        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    public void testExecuteQueryOpenAI() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.OPENAI)
            .modelName("gpt-3.5-turbo")
            .displayName("GPT-3.5 Turbo")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .contextWindow(4096)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    public void testExecuteQueryAnthropic() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.ANTHROPIC)
            .modelName("claude-3-5-sonnet-20240620")
            .displayName("claude-3-5-sonnet-20240620")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .contextWindow(100000)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    public void testExecuteQueryGemini() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.GOOGLE)
            .modelName("gemini-1.5-flash")
            .displayName("Gemini 1.5 Flash")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .contextWindow(32768)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    public void testExecuteQueryMistral() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.MISTRAL)
            .modelName("mistral-medium")
            .displayName("Mistral Medium")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .contextWindow(32768)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    @Test
    public void testExecuteQueryDeepInfra() {
        LanguageModel model = LanguageModel.builder()
            .provider(ModelProvider.DEEP_INFRA)
            .modelName("mistralai/Mixtral-8x7B-Instruct-v0.1")
            .displayName("Mixtral 8x7B")
            .apiKeyUsed(true)
            .inputCost(0.0)
            .outputCost(0.0)
            .contextWindow(32768)
            .build();
        verifyResponse(createChatModel(model), model);
    }

    private ChatLanguageModel createChatModel(LanguageModel languageModel) {
        return switch (languageModel.getProvider()) {
            case OPENAI -> OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case ANTHROPIC -> AnthropicChatModel.builder()
                .apiKey(dotenv.get("ANTHROPIC_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case GOOGLE -> GoogleAiGeminiChatModel.builder()
                .apiKey(dotenv.get("GEMINI_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case MISTRAL -> MistralAiChatModel.builder()
                .apiKey(dotenv.get("MISTRAL_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case DEEP_INFRA -> OpenAiChatModel.builder()
                .baseUrl("https://api.deepinfra.com/v1/openai")
                .apiKey(dotenv.get("DEEPINFRA_API_KEY"))
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
            .totalFileCount(1)
            .executionTimeMs(0)
            .cost(0).build();

        CompletableFuture<Response<AiMessage>> response = promptExecutionService.executeQuery(context);
        assertNotNull(response);

        var aiMessage = response.join().content();
        assertNotNull(aiMessage);
        assertNotNull(aiMessage.text());
        assertFalse(aiMessage.text().isEmpty());
        assertTrue(aiMessage.text().toLowerCase().contains("brussels"));
    }
}
