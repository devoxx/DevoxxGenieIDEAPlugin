package com.devoxx.genie.chatmodel.cloud.anthropic;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;

import dev.langchain4j.model.chat.ChatModel;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnthropicChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getAnthropicKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());

        // Mock model registry to make getModels() deterministic and avoid network/service side effects
        LLMModelRegistryService modelRegistryServiceMock = mock(LLMModelRegistryService.class);
        when(modelRegistryServiceMock.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.Anthropic).modelName("claude-1").displayName("Claude 1").inputCost(1).outputCost(1).inputMaxTokens(200_000).apiKeyUsed(true).build(),
                LanguageModel.builder().provider(ModelProvider.Anthropic).modelName("claude-2").displayName("Claude 2").inputCost(1).outputCost(1).inputMaxTokens(200_000).apiKeyUsed(true).build(),
                LanguageModel.builder().provider(ModelProvider.Anthropic).modelName("claude-3").displayName("Claude 3").inputCost(1).outputCost(1).inputMaxTokens(200_000).apiKeyUsed(true).build(),
                LanguageModel.builder().provider(ModelProvider.Anthropic).modelName("claude-4").displayName("Claude 4").inputCost(1).outputCost(1).inputMaxTokens(200_000).apiKeyUsed(true).build()
        ));
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), LLMModelRegistryService.class, modelRegistryServiceMock, getTestRootDisposable());
    }

    @Test
    public void testCreateChatModel() {
        // Instance of the class containing the method to be tested
        var factory = new AnthropicChatModelFactory();

        // Create a dummy ChatModel
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("dummy-model");
        customChatModel.setTemperature(0.7);
        customChatModel.setTopP(0.9);
        customChatModel.setMaxTokens(256);
        customChatModel.setMaxRetries(3);

        // Call the method
        ChatModel result = factory.createChatModel(customChatModel);
        assertThat(result).isNotNull();
    }


    @Test
    public void testModelNames() {
        AnthropicChatModelFactory factory = new AnthropicChatModelFactory();
        Assertions.assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> modelNames = factory.getModels();
        Assertions.assertThat(modelNames).size().isGreaterThan(3);
    }
}
