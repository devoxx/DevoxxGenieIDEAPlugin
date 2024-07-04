package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.gemini.GeminiChatModel;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PromptExecutionServiceIT extends AbstractLightPlatformTestCase {

    private static Dotenv dotenv;

    @BeforeAll
    static void loadEnvironment() {
        dotenv = Dotenv.load();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockSettingsState();
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

    @ParameterizedTest
    @MethodSource("provideModels")
    public void testExecuteQuery(LanguageModel languageModel) {
        PromptExecutionService promptExecutionService = new PromptExecutionService();

        ChatMessageContext context = ChatMessageContext.builder()
            .userPrompt("What is the capital of Belgium?")
            .chatLanguageModel(createChatModel(languageModel))
            .languageModel(languageModel)
            .project(getProject())
            .build();

        verifyResponse(promptExecutionService, context);
    }

    private static Stream<LanguageModel> provideModels() {
        return new LLMModelRegistryService().getModels().stream();
    }

    private ChatLanguageModel createChatModel(@NotNull LanguageModel languageModel) {
        return switch (languageModel.getProvider()) {
            case OpenAI -> OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case Anthropic -> AnthropicChatModel.builder()
                .apiKey(dotenv.get("ANTHROPIC_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case Gemini -> GeminiChatModel.builder()
                .apiKey(dotenv.get("GEMINI_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case Mistral -> MistralAiChatModel.builder()
                .apiKey(dotenv.get("MISTRAL_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            case DeepInfra -> OpenAiChatModel.builder()
                .baseUrl("https://api.deepinfra.com/v1/openai")
                .apiKey(dotenv.get("DEEPINFRA_API_KEY"))
                .modelName(languageModel.getModelName())
                .build();
            default -> throw new IllegalArgumentException("Unsupported model provider");
        };
    }

    private static void verifyResponse(@NotNull PromptExecutionService promptExecutionService,
                                       ChatMessageContext context) {
        CompletableFuture<Optional<AiMessage>> response = promptExecutionService.executeQuery(context);
        assertNotNull(response);

        Optional<AiMessage> aiMessageOptional = response.join();
        assertTrue(aiMessageOptional.isPresent());
        AiMessage aiMessage = aiMessageOptional.get();
        assertNotNull(aiMessage);
        assertNotNull(aiMessage.text());
        assertFalse(aiMessage.text().isEmpty());
        assertTrue(aiMessage.text().toLowerCase().contains("brussels"));
    }
}
