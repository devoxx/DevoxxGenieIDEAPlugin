package com.devoxx.genie.chatmodel.cloud.anthropic;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnthropicChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        DevoxxGenieStateService settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getAnthropicKey()).thenReturn("dummy-api-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    void testCreateChatModel() {
        // Instance of the class containing the method to be tested
        var factory = new AnthropicChatModelFactory();

        // Create a dummy ChatModel
        ChatModel chatModel = new ChatModel();
        chatModel.setModelName("dummy-model");
        chatModel.setTemperature(0.7);
        chatModel.setTopP(0.9);
        chatModel.setMaxTokens(256);
        chatModel.setMaxRetries(3);

        // Call the method
        ChatLanguageModel result = factory.createChatModel(chatModel);
        assertThat(result).isNotNull();
    }


    @Test
    void testModelNames() {
        AnthropicChatModelFactory factory = new AnthropicChatModelFactory();
        Assertions.assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> modelNames = factory.getModels();
        Assertions.assertThat(modelNames).size().isGreaterThan(3);
    }
}
