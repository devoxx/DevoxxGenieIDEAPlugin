package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomOpenAIChatModelFactoryTest {

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;
    private DevoxxGenieStateService mockState;

    @BeforeEach
    void setUp() {
        mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");
        when(mockState.isCustomOpenAIApiKeyEnabled()).thenReturn(false);
        when(mockState.getCustomOpenAIApiKey()).thenReturn("");
        when(mockState.isCustomOpenAIModelNameEnabled()).thenReturn(false);
        when(mockState.getCustomOpenAIModelName()).thenReturn("");
        when(mockState.isCustomOpenAIForceHttp11()).thenReturn(false);
        when(mockState.getAgentModeEnabled()).thenReturn(false);

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(MCPService::isMCPEnabled).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedMCPService != null) mockedMCPService.close();
    }

    @Test
    void createChatModelShouldReturnNonNull() {
        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(256);
        customChatModel.setMaxRetries(3);
        customChatModel.setTimeout(30);

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createChatModelWithApiKeyEnabledShouldUseApiKey() {
        when(mockState.isCustomOpenAIApiKeyEnabled()).thenReturn(true);
        when(mockState.getCustomOpenAIApiKey()).thenReturn("my-custom-key");

        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createChatModelWithModelNameEnabledShouldUseCustomModelName() {
        when(mockState.isCustomOpenAIModelNameEnabled()).thenReturn(true);
        when(mockState.getCustomOpenAIModelName()).thenReturn("my-custom-model");

        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createChatModelWithBlankModelNameShouldFallbackToDefault() {
        when(mockState.isCustomOpenAIModelNameEnabled()).thenReturn(true);
        when(mockState.getCustomOpenAIModelName()).thenReturn("   ");

        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelShouldReturnNonNull() {
        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setTimeout(30);

        StreamingChatModel result = factory.createStreamingChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelWithApiKeyEnabledShouldWork() {
        when(mockState.isCustomOpenAIApiKeyEnabled()).thenReturn(true);
        when(mockState.getCustomOpenAIApiKey()).thenReturn("my-streaming-key");

        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");

        StreamingChatModel result = factory.createStreamingChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createChatModelWithForceHttp11ShouldWork() {
        when(mockState.isCustomOpenAIForceHttp11()).thenReturn(true);

        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelWithForceHttp11ShouldWork() {
        when(mockState.isCustomOpenAIForceHttp11()).thenReturn(true);

        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("custom-model");

        StreamingChatModel result = factory.createStreamingChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void getModelsWithBlankUrlReturnsEmptyList() {
        when(mockState.getCustomOpenAIUrl()).thenReturn("");
        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
        assertThat(factory.getModels()).isEmpty();
    }

    @Test
    void getModelsWhenServerUnreachableReturnsEmptyList() {
        // A missing/empty/failing /models endpoint must degrade gracefully
        // (return empty) rather than throw and break model loading.
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://127.0.0.1:1/v1/");
        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
        assertThat(factory.getModels()).isEmpty();
    }

    @Test
    void getModelsCachesResultAndProbesEndpointOnlyOnce() {
        // Regression for the freeze: repeated provider selections must not re-probe the
        // /models endpoint every time. The network call is cached after the first fetch.
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");

        ResponseDTO response = buildResponse("model-a", "model-b");

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)))
                    .thenReturn(response);

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            List<LanguageModel> first = factory.getModels();
            List<LanguageModel> second = factory.getModels();

            assertThat(first).extracting(LanguageModel::getModelName).containsExactly("model-a", "model-b");
            assertThat(second).extracting(LanguageModel::getModelName).containsExactly("model-a", "model-b");

            // The endpoint is probed exactly once despite two getModels() calls.
            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)), times(1));
        }
    }

    @Test
    void resetModelsClearsCacheSoNextGetModelsReprobes() {
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)))
                    .thenReturn(buildResponse("model-a"));

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            factory.getModels();
            factory.resetModels();
            factory.getModels();

            // After reset the endpoint is probed again: two probes for two uncached calls.
            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)), times(2));
        }
    }

    @Test
    void getModelsReflectsContextWindowChangedAfterFirstProbe() {
        // Regression: the factory used to cache fully-built LanguageModel objects, so the
        // context window configured in Settings after the first probe never reached the model.
        // The conversation footer then kept showing the 4096 default as the denominator.
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");
        when(mockState.getCustomOpenAIContextWindow()).thenReturn(null);

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)))
                    .thenReturn(buildResponse("model-a"));

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            assertThat(factory.getModels())
                    .extracting(LanguageModel::getInputMaxTokens)
                    .containsExactly(CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW);

            // User sets the real context window in Settings -> Tools -> DevoxxGenie -> LLM Providers.
            when(mockState.getCustomOpenAIContextWindow()).thenReturn(262_000);

            assertThat(factory.getModels())
                    .extracting(LanguageModel::getInputMaxTokens)
                    .containsExactly(262_000);

            // Still a single network probe: the settings-derived fields are rebuilt, not re-fetched.
            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)), times(1));
        }
    }

    @Test
    void getModelsReflectsCostsChangedAfterFirstProbe() {
        // Same staleness applied to the cost fields, which feed the cost estimate in the footer.
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");
        when(mockState.getCustomOpenAIInputCost()).thenReturn(null);
        when(mockState.getCustomOpenAIOutputCost()).thenReturn(null);

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)))
                    .thenReturn(buildResponse("model-a"));

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
            factory.getModels();

            when(mockState.getCustomOpenAIInputCost()).thenReturn(1.5);
            when(mockState.getCustomOpenAIOutputCost()).thenReturn(2.5);

            assertThat(factory.getModels())
                    .singleElement()
                    .satisfies(model -> {
                        assertThat(model.getInputCost()).isEqualTo(1.5);
                        assertThat(model.getOutputCost()).isEqualTo(2.5);
                    });
        }
    }

    @Test
    void getModelsWithModelNameOverrideSkipsEndpointProbe() {
        // Issue #1210: when the user has explicitly set a model name via the override field,
        // getModels() must NOT probe the /models endpoint. Probing an already-specified model
        // wastes a network round-trip and, for endpoints whose base URL is the full chat path,
        // produced a scary 401 on the nonsensical '.../chat/completions/models' URL.
        when(mockState.getCustomOpenAIUrl())
                .thenReturn("https://gateway.ai.cloudflare.com/v1/id/free-gateway/compat/chat/completions");
        when(mockState.isCustomOpenAIModelNameEnabled()).thenReturn(true);
        when(mockState.getCustomOpenAIModelName()).thenReturn("gpt-4o-mini");

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            assertThat(factory.getModels())
                    .extracting(LanguageModel::getModelName)
                    .containsExactly("gpt-4o-mini");

            // The endpoint is never probed when an explicit model name is configured.
            util.verifyNoInteractions();
        }
    }

    @Test
    void getModelsWithBlankModelNameOverrideStillProbesEndpoint() {
        // The skip only applies when the override is enabled AND non-blank. A blank override
        // must fall back to the normal /models probe so the picker can still be populated.
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");
        when(mockState.isCustomOpenAIModelNameEnabled()).thenReturn(true);
        when(mockState.getCustomOpenAIModelName()).thenReturn("   ");

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)))
                    .thenReturn(buildResponse("model-a"));

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            assertThat(factory.getModels())
                    .extracting(LanguageModel::getModelName)
                    .containsExactly("model-a");

            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)), times(1));
        }
    }

    @Test
    void getModelsPassesApiKeyToProbeWhenAuthEnabled() {
        // Issue #1210: authenticated gateways (e.g. Cloudflare AI Gateway) reject the /models probe
        // with 401 unless it carries the API key. The probe must forward it as a bearer token.
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");
        when(mockState.isCustomOpenAIApiKeyEnabled()).thenReturn(true);
        when(mockState.getCustomOpenAIApiKey()).thenReturn("secret-token");

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class),
                            any(OkHttpClient.class), eq("secret-token")))
                    .thenReturn(buildResponse("model-a"));

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            assertThat(factory.getModels())
                    .extracting(LanguageModel::getModelName)
                    .containsExactly("model-a");

            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class),
                    any(OkHttpClient.class), eq("secret-token")), times(1));
        }
    }

    private static ResponseDTO buildResponse(String... modelIds) {
        ResponseDTO dto = new ResponseDTO();
        dto.setData(java.util.Arrays.stream(modelIds).map(id -> {
            com.devoxx.genie.model.gpt4all.Model m = new com.devoxx.genie.model.gpt4all.Model();
            m.setId(id);
            return m;
        }).toList());
        return dto;
    }
}
