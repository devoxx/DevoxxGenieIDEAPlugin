package com.devoxx.genie.chatmodel.lmstudio;

import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LMStudioChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<DevoxxGenieSettingsServiceProvider> mockedSettings = Mockito.mockStatic(DevoxxGenieSettingsServiceProvider.class)) {
            // Setup the mock for SettingsState
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieSettingsServiceProvider.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getLmstudioModelUrl()).thenReturn("http://localhost:8080");

            // Instance of the class containing the method to be tested
            LMStudioChatModelFactory factory = new LMStudioChatModelFactory();

            // Create a dummy ChatModel
            ChatModel chatModel = new ChatModel();
            chatModel.setModelName("lmstudio");

            // Call the method
            ChatLanguageModel result = factory.createChatModel(chatModel);
            assertThat(result).isNotNull();
        }
    }
}
