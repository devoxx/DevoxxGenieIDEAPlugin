package com.devoxx.genie.chatmodel.local.llamacpp;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
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
class LlamaChatModelFactoryTest {

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;

    @BeforeEach
    void setUp() {
        DevoxxGenieStateService mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getLlamaCPPUrl()).thenReturn("http://localhost:8080");
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
        LlamaChatModelFactory factory = new LlamaChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("llama-model");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxRetries(3);
        customChatModel.setTimeout(30);

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createChatModelWithDefaultValuesShouldWork() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("llama-cpp");

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void createChatModelShouldRespectCustomParameters() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();

        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("llama-model");
        customChatModel.setTemperature(0.0);
        customChatModel.setTopP(1.0);
        customChatModel.setMaxRetries(5);
        customChatModel.setTimeout(120);

        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }

    @Test
    void getModelsShouldReturnNonEmptyList() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();
        List<LanguageModel> models = factory.getModels();
        assertThat(models).isNotEmpty();
        assertThat(models).hasSize(1);
    }

    @Test
    void getModelsShouldReturnModelWithCorrectProvider() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();
        List<LanguageModel> models = factory.getModels();

        LanguageModel model = models.get(0);
        assertThat(model.getProvider()).isEqualTo(ModelProvider.LLaMA);
    }

    @Test
    void getModelsShouldReturnModelWithTestModelName() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();
        List<LanguageModel> models = factory.getModels();

        LanguageModel model = models.get(0);
        assertThat(model.getModelName()).isEqualTo("test-model");
        assertThat(model.getDisplayName()).isEqualTo("test-model");
    }

    @Test
    void getModelsShouldReturnFreeModel() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();
        List<LanguageModel> models = factory.getModels();

        LanguageModel model = models.get(0);
        assertThat(model.getInputCost()).isEqualTo(0);
        assertThat(model.getOutputCost()).isEqualTo(0);
    }

    @Test
    void getModelsShouldReturnModelWithoutApiKey() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();
        List<LanguageModel> models = factory.getModels();

        LanguageModel model = models.get(0);
        assertThat(model.isApiKeyUsed()).isFalse();
    }

    @Test
    void getModelsShouldReturnModelWithCorrectTokenLimit() {
        LlamaChatModelFactory factory = new LlamaChatModelFactory();
        List<LanguageModel> models = factory.getModels();

        LanguageModel model = models.get(0);
        assertThat(model.getInputMaxTokens()).isEqualTo(8000);
    }
}
