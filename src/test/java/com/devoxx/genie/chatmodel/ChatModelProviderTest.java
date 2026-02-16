package com.devoxx.genie.chatmodel;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatModelProviderTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private ChatModelFactory chatModelFactory;

    @Mock
    private ChatModel chatModel;

    @Mock
    private StreamingChatModel streamingChatModel;

    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;
    private MockedStatic<ChatModelFactoryProvider> factoryProviderMockedStatic;

    private ChatModelProvider chatModelProvider;

    @BeforeEach
    void setUp() {
        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        factoryProviderMockedStatic = Mockito.mockStatic(ChatModelFactoryProvider.class);

        // Default stubs for state service
        when(stateService.getTemperature()).thenReturn(0.7);
        when(stateService.getMaxRetries()).thenReturn(3);
        when(stateService.getTopP()).thenReturn(0.9);
        when(stateService.getTimeout()).thenReturn(60);
        when(stateService.getMaxOutputTokens()).thenReturn(4000);

        chatModelProvider = new ChatModelProvider();
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        factoryProviderMockedStatic.close();
    }

    @Test
    void testGetChatLanguageModel_ReturnsModel() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        factoryProviderMockedStatic
                .when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.of(chatModelFactory));

        when(chatModelFactory.createChatModel(any(CustomChatModel.class))).thenReturn(chatModel);

        ChatModel result = chatModelProvider.getChatLanguageModel(context);

        assertThat(result).isSameAs(chatModel);
        verify(chatModelFactory).createChatModel(any(CustomChatModel.class));
    }

    @Test
    void testGetStreamingChatLanguageModel_ReturnsModel() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.Anthropic)
                .modelName("claude-3-opus")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        factoryProviderMockedStatic
                .when(() -> ChatModelFactoryProvider.getFactoryByProvider("Anthropic"))
                .thenReturn(Optional.of(chatModelFactory));

        when(chatModelFactory.createStreamingChatModel(any(CustomChatModel.class))).thenReturn(streamingChatModel);

        StreamingChatModel result = chatModelProvider.getStreamingChatLanguageModel(context);

        assertThat(result).isSameAs(streamingChatModel);
        verify(chatModelFactory).createStreamingChatModel(any(CustomChatModel.class));
    }

    @Test
    void testGetFactory_ThrowsWhenNoFactoryFound() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        factoryProviderMockedStatic
                .when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenAI"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatModelProvider.getChatLanguageModel(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No factory for provider");
    }

    @Test
    void testGetChatLanguageModel_WithDifferentProvider() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.Anthropic)
                .modelName("claude-3-sonnet")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        factoryProviderMockedStatic
                .when(() -> ChatModelFactoryProvider.getFactoryByProvider("Anthropic"))
                .thenReturn(Optional.of(chatModelFactory));

        when(chatModelFactory.createChatModel(any(CustomChatModel.class))).thenReturn(chatModel);

        ChatModel result = chatModelProvider.getChatLanguageModel(context);

        assertThat(result).isSameAs(chatModel);
        // Verify it looked up Anthropic, not OpenAI
        factoryProviderMockedStatic.verify(() -> ChatModelFactoryProvider.getFactoryByProvider("Anthropic"));
    }

    @Test
    void testInitChatModel_SetsAllFields() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .inputMaxTokens(128000)
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        when(stateService.getTemperature()).thenReturn(0.5);
        when(stateService.getMaxRetries()).thenReturn(2);
        when(stateService.getTopP()).thenReturn(0.8);
        when(stateService.getTimeout()).thenReturn(120);
        when(stateService.getMaxOutputTokens()).thenReturn(8000);

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getModelName()).isEqualTo("gpt-4");
        assertThat(result.getTemperature()).isEqualTo(0.5);
        assertThat(result.getMaxRetries()).isEqualTo(2);
        assertThat(result.getTopP()).isEqualTo(0.8);
        assertThat(result.getTimeout()).isEqualTo(120);
        assertThat(result.getMaxTokens()).isEqualTo(8000);
        assertThat(result.getContextWindow()).isEqualTo(128000);
    }

    @Test
    void testInitChatModel_UsesTestModelWhenModelNameIsNull() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName(null)
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getModelName()).isEqualTo(ChatModelFactory.TEST_MODEL);
    }

    @Test
    void testInitChatModel_DoesNotSetContextWindowWhenZero() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .inputMaxTokens(0)
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getContextWindow()).isNull();
    }

    @Test
    void testInitChatModel_UsesDefaultMaxOutputTokensWhenNull() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        when(stateService.getMaxOutputTokens()).thenReturn(null);

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getMaxTokens()).isEqualTo(Constant.MAX_OUTPUT_TOKENS);
    }

    @Test
    void testInitChatModel_SetsBaseUrlForOllama() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.Ollama)
                .modelName("llama3")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        when(stateService.getOllamaModelUrl()).thenReturn("http://localhost:11434/");

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getBaseUrl()).isEqualTo("http://localhost:11434/");
    }

    @Test
    void testInitChatModel_SetsBaseUrlForLMStudio() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.LMStudio)
                .modelName("local-model")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        when(stateService.getLmstudioModelUrl()).thenReturn("http://localhost:1234/v1/");

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getBaseUrl()).isEqualTo("http://localhost:1234/v1/");
    }

    @Test
    void testInitChatModel_SetsBaseUrlForGPT4All() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.GPT4All)
                .modelName("gpt4all-model")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        when(stateService.getGpt4allModelUrl()).thenReturn("http://localhost:4891/v1/");

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getBaseUrl()).isEqualTo("http://localhost:4891/v1/");
    }

    @Test
    void testInitChatModel_SetsBaseUrlForLlama() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.LLaMA)
                .modelName("llama-model")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        when(stateService.getLlamaCPPUrl()).thenReturn("http://localhost:8080");

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getBaseUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void testInitChatModel_SetsBaseUrlForCustomOpenAI() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.CustomOpenAI)
                .modelName("custom-model")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        when(stateService.getCustomOpenAIUrl()).thenReturn("http://custom-api.example.com/v1/");

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getBaseUrl()).isEqualTo("http://custom-api.example.com/v1/");
    }

    @Test
    void testInitChatModel_DoesNotSetBaseUrlForCloudProvider() {
        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .build();

        ChatMessageContext context = ChatMessageContext.builder()
                .languageModel(languageModel)
                .build();

        CustomChatModel result = chatModelProvider.initChatModel(context);

        assertThat(result.getBaseUrl()).isNull();
    }
}
