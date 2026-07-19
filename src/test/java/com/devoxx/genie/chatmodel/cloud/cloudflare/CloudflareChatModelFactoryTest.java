package com.devoxx.genie.chatmodel.cloud.cloudflare;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudflareChatModelFactoryTest {

    private static final String MODELS_URL =
            "https://gateway.ai.cloudflare.com/v1/acct123/default/compat/models";

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;
    private DevoxxGenieStateService mockState;

    @BeforeEach
    void setUp() {
        mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getCloudflareAccountId()).thenReturn("acct123");
        when(mockState.getCloudflareGatewayName()).thenReturn("default");
        when(mockState.getCloudflareKey()).thenReturn("cf-token");
        when(mockState.getCloudflareModelName()).thenReturn("");
        when(mockState.isCloudflareModelNameEnabled()).thenReturn(false);
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
    void createChatModelReturnsNonNull() {
        CustomChatModel model = new CustomChatModel();
        model.setModelName("openai/gpt-4o-mini");
        model.setTemperature(0.7);
        model.setTopP(0.9);
        model.setMaxTokens(256);
        model.setMaxRetries(3);
        model.setTimeout(30);
        ChatModel result = new CloudflareChatModelFactory().createChatModel(model);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelReturnsNonNull() {
        CustomChatModel model = new CustomChatModel();
        model.setModelName("openai/gpt-4o-mini");
        model.setTemperature(0.7);
        model.setTopP(0.9);
        model.setTimeout(30);
        StreamingChatModel result = new CloudflareChatModelFactory().createStreamingChatModel(model);
        assertThat(result).isNotNull();
    }

    @Test
    void getModelsProbesAssembledCompatUrlWithBearerToken() {
        ResponseDTO response = buildResponse("openai/gpt-4o-mini", "anthropic/claude-4-5-sonnet");
        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), eq("cf-token")))
                    .thenReturn(response);

            assertThat(new CloudflareChatModelFactory().getModels())
                    .extracting(LanguageModel::getModelName)
                    .containsExactly("openai/gpt-4o-mini", "anthropic/claude-4-5-sonnet");

            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), eq("cf-token")), times(1));
        }
    }

    @Test
    void getModelsWithModelNameOverrideSkipsProbe() {
        when(mockState.isCloudflareModelNameEnabled()).thenReturn(true);
        when(mockState.getCloudflareModelName()).thenReturn("openai/gpt-4o");
        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            assertThat(new CloudflareChatModelFactory().getModels())
                    .extracting(LanguageModel::getModelName)
                    .containsExactly("openai/gpt-4o");
            util.verifyNoInteractions();
        }
    }

    @Test
    void getModelsWithBlankAccountIdReturnsEmpty() {
        when(mockState.getCloudflareAccountId()).thenReturn("");
        assertThat(new CloudflareChatModelFactory().getModels()).isEmpty();
    }

    @Test
    void getModelsCachesIdsAndProbesOnlyOnce() {
        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)))
                    .thenReturn(buildResponse("openai/gpt-4o-mini"));

            CloudflareChatModelFactory factory = new CloudflareChatModelFactory();
            factory.getModels();
            factory.getModels();

            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)), times(1));
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
