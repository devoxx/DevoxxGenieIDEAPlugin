package com.devoxx.genie.chatmodel.jan;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.service.SettingsStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JanChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<SettingsStateService> mockedSettings = Mockito.mockStatic(SettingsStateService.class)) {
            // Setup the mock for SettingsState
            SettingsStateService mockSettingsState = mock(SettingsStateService.class);
            when(SettingsStateService.getInstance()).thenReturn(mockSettingsState);
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
}
