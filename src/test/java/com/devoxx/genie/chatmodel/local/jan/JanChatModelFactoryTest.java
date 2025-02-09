package com.devoxx.genie.chatmodel.local.jan;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.Socket;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JanChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Setup the mock for SettingsState
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getJanModelUrl()).thenReturn("http://localhost:8080");

            // Instance of the class containing the method to be tested
            JanChatModelFactory factory = new JanChatModelFactory();

            // Create a dummy ChatModel
            ChatModel chatModel = new ChatModel();
            chatModel.setModelName("jan");
            chatModel.setBaseUrl("http://localhost:8080");

            // Call the method
            ChatLanguageModel result = factory.createChatModel(chatModel);
            assertThat(result).isNotNull();
        }
    }

    @Test
    @EnabledIf("isJanRunning")
    void testHelloChat() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Setup the mock for SettingsState
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getJanModelUrl()).thenReturn("http://localhost:1337/v1/");

            // Instance of the class containing the method to be tested
            JanChatModelFactory factory = new JanChatModelFactory();

            ChatModel chatModel = new ChatModel();
            chatModel.setModelName("mistral-ins-7b-q4");
            ChatLanguageModel chatLanguageModel = factory.createChatModel(chatModel);
            String hello = chatLanguageModel.chat("Hello");
            assertThat(hello).isNotNull();
        }
    }

    private boolean isJanRunning() {
        try (Socket socket = new Socket("localhost", 1337)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
