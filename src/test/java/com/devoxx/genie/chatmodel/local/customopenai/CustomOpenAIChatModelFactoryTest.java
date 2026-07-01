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
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class)))
                    .thenReturn(response);

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            List<LanguageModel> first = factory.getModels();
            List<LanguageModel> second = factory.getModels();

            assertThat(first).extracting(LanguageModel::getModelName).containsExactly("model-a", "model-b");
            assertThat(second).extracting(LanguageModel::getModelName).containsExactly("model-a", "model-b");

            // The endpoint is probed exactly once despite two getModels() calls.
            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class)), times(1));
        }
    }

    @Test
    void resetModelsClearsCacheSoNextGetModelsReprobes() {
        when(mockState.getCustomOpenAIUrl()).thenReturn("http://localhost:8080/v1/");

        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class)))
                    .thenReturn(buildResponse("model-a"));

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();

            factory.getModels();
            factory.resetModels();
            factory.getModels();

            // After reset the endpoint is probed again: two probes for two uncached calls.
            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq("http://localhost:8080/v1/models"), eq(ResponseDTO.class), any(OkHttpClient.class)), times(2));
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
