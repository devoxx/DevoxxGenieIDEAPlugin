package com.devoxx.genie.chatmodel.gemini;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.service.settings.SettingsStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeminiChatModelFactoryTest extends AbstractLightPlatformTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        SettingsStateService settingsStateMock = mock(SettingsStateService.class);
        when(settingsStateMock.getGeminiKey()).thenReturn("dummy-key");

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(
            ApplicationManager.getApplication(),
            SettingsStateService.class,
            settingsStateMock,
            getTestRootDisposable()
        );
    }

    @Test
    public void createChatModel() {
        // Instance of the class containing the method to be tested
        var factory = new GeminiChatModelFactory();

        // Create a dummy ChatModel
        ChatModel chatModel = new ChatModel();
        chatModel.setModelName("gemini-pro");
        chatModel.setTemperature(0.7);
        chatModel.setTopP(0.9);
        chatModel.setMaxTokens(256);
        chatModel.setMaxRetries(3);

        // Call the method
        ChatLanguageModel result = factory.createChatModel(chatModel);
        Assertions.assertThat(result).isNotNull();
    }
}
