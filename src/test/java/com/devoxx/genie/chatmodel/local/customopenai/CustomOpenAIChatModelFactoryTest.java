package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.service.mcp.MCPService;
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
import static org.mockito.Mockito.mock;
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
    void getModelsShouldReturnEmptyList() {
        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
        List<LanguageModel> models = factory.getModels();
        assertThat(models).isEmpty();
    }
}
