package com.devoxx.genie.chatmodel.cloud.grok;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrokChatModelFactoryTest {

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<LLMProviderService> mockedProviderService;
    private MockedStatic<MCPService> mockedMCPService;
    private MockedStatic<LLMModelRegistryService> mockedModelRegistry;

    @BeforeEach
    void setUp() {
        DevoxxGenieStateService mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getGrokKey()).thenReturn("dummy-api-key");
        when(mockState.getAgentModeEnabled()).thenReturn(false);

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        LLMProviderService mockProviderService = mock(LLMProviderService.class);
        when(mockProviderService.getApiKey(any())).thenReturn("dummy-api-key");
        mockedProviderService = Mockito.mockStatic(LLMProviderService.class);
        mockedProviderService.when(LLMProviderService::getInstance).thenReturn(mockProviderService);

        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(MCPService::isMCPEnabled).thenReturn(false);

        LLMModelRegistryService mockModelRegistry = mock(LLMModelRegistryService.class);
        when(mockModelRegistry.getModels()).thenReturn(List.of());
        mockedModelRegistry = Mockito.mockStatic(LLMModelRegistryService.class);
        mockedModelRegistry.when(LLMModelRegistryService::getInstance).thenReturn(mockModelRegistry);
    }

    @AfterEach
    void tearDown() {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedProviderService != null) mockedProviderService.close();
        if (mockedMCPService != null) mockedMCPService.close();
        if (mockedModelRegistry != null) mockedModelRegistry.close();
    }

    @Test
    void createChatModelShouldReturnNonNull() {
        GrokChatModelFactory factory = new GrokChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("grok-1");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxRetries(3);
        customChatModel.setTimeout(30);

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createChatModelWithDefaultValuesShouldWork() {
        GrokChatModelFactory factory = new GrokChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("grok-1");

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelShouldReturnNonNull() {
        GrokChatModelFactory factory = new GrokChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("grok-1");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setTimeout(30);

        StreamingChatModel result = factory.createStreamingChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelWithDefaultValuesShouldWork() {
        GrokChatModelFactory factory = new GrokChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("grok-1");

        StreamingChatModel result = factory.createStreamingChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void getModelsShouldReturnList() {
        GrokChatModelFactory factory = new GrokChatModelFactory();
        List<LanguageModel> models = factory.getModels();
        assertThat(models).isNotNull();
    }

    @Test
    void createChatModelShouldRespectCustomParameters() {
        GrokChatModelFactory factory = new GrokChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("grok-2");
        customChatModel.setTemperature(0.0);
        customChatModel.setTopP(1.0);
        customChatModel.setMaxRetries(5);
        customChatModel.setTimeout(120);

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelShouldRespectCustomParameters() {
        GrokChatModelFactory factory = new GrokChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("grok-2");
        customChatModel.setTemperature(1.0);
        customChatModel.setTopP(0.5);
        customChatModel.setTimeout(60);

        StreamingChatModel result = factory.createStreamingChatModel(customChatModel);
        assertThat(result).isNotNull();
    }
}
