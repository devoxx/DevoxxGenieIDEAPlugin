package com.devoxx.genie.chatmodel.azureopenai;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.chatmodel.cloud.azureopenai.AzureOpenAIChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureOpenAiChatModelFactoryTest extends AbstractLightPlatformTestCase {

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
    public void createChatModel() {
        AzureOpenAIChatModelFactory factory = new AzureOpenAIChatModelFactory();
        ChatModel chatModel = new ChatModel();
        chatModel.setModelName("gpt-3.5-turbo");
        chatModel.setTemperature(0.7);
        chatModel.setMaxTokens(100);

        ChatLanguageModel result = factory.createChatModel(chatModel);

        assertThat(result).isNotNull();
    }

    @Test
    public void getModels() {
        AzureOpenAIChatModelFactory factory = new AzureOpenAIChatModelFactory();
        assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> models = factory.getModels();
        assertThat(models).size().isEqualTo(1);
    }
}
