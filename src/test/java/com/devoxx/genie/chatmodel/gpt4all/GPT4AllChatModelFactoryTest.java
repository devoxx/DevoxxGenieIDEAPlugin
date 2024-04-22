package com.devoxx.genie.chatmodel.gpt4all;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.ui.SettingsState;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GPT4AllChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<SettingsState> mockedSettings = Mockito.mockStatic(SettingsState.class)) {
            // Setup the mock for SettingsState
            SettingsState mockSettingsState = mock(SettingsState.class);
            when(SettingsState.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getGpt4allModelUrl()).thenReturn("http://localhost:8080");

            // Instance of the class containing the method to be tested
            GPT4AllChatModelFactory factory = new GPT4AllChatModelFactory();

            // Create a dummy ChatModel
            ChatModel chatModel = new ChatModel();
            chatModel.setModelName("gtp4all");

            // Call the method
            ChatLanguageModel result = factory.createChatModel(chatModel);
            assertThat(result).isNotNull();
        }
    }
}
