package com.devoxx.genie.chatmodel.cloud.azureopenai;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureOpenAiChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getAzureOpenAIKey()).thenReturn("dummy-azure-openai-api-key");
        when(settingsStateMock.getAzureOpenAIEndpoint()).thenReturn("https://example.openai.azure.com/");
        when(settingsStateMock.getAzureOpenAIDeployment()).thenReturn("gpt-4o-deployment");
        when(settingsStateMock.getOpenRouterKey()).thenReturn("dummy-openrouter-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    void createChatModel() {
        AzureOpenAIChatModelFactory factory = new AzureOpenAIChatModelFactory();
        CustomChatModel customChatModel = new CustomChatModel();
        customChatModel.setModelName("gpt-3.5-turbo");
        customChatModel.setTemperature(0.6);
        customChatModel.setMaxTokens(100);

        ChatModel result = factory.createChatModel(customChatModel);

        // cannot verify more because model does not offer more access to data inside
        assertThat(result).isInstanceOf(AzureOpenAiChatModel.class);
    }

    @Test
    void getModels() {
        AzureOpenAIChatModelFactory factory = new AzureOpenAIChatModelFactory();
        assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> models = factory.getModels();
        assertThat(models).size().isEqualTo(1);
    }
}
