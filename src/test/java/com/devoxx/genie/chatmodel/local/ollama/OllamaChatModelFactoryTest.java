package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Setup the mock for SettingsState
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");

            // Instance of the class containing the method to be tested
            OllamaChatModelFactory factory = new OllamaChatModelFactory();

            // Create a dummy ChatModel
            ChatModel chatModel = new ChatModel();
            chatModel.setModelName("ollama");
            chatModel.setBaseUrl("http://localhost:8080");

            // Call the method
            ChatLanguageModel result = factory.createChatModel(chatModel);
            assertThat(result).isNotNull();
        }
    }
}
