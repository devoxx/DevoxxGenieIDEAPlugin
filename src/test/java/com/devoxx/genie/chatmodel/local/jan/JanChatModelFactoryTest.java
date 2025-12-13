package com.devoxx.genie.chatmodel.local.jan;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.Socket;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JanChatModelFactory}.
 *
 * <p>Note: The {@link #testHelloChat()} test requires Jan to be running before execution.
 * Ensure that Jan is running on localhost:1337 before running this test, otherwise it
 * will be skipped. Ensure Jan is running with downloaded model "Mistral 7B Instruct Q4"
 * (mistral-ins-7b-q4).</p>
 */
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
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("jan");
            customChatModel.setBaseUrl("http://localhost:8080");

            // Call the method
            ChatModel result = factory.createChatModel(customChatModel);
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

            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("mistral-ins-7b-q4");
            ChatModel chatLanguageModel = factory.createChatModel(customChatModel);
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
