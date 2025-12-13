package com.devoxx.genie.chatmodel.local.lmstudio;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LMStudioChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Setup the mock for SettingsState
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getLmstudioModelUrl()).thenReturn("http://localhost:8080");

            // Instance of the class containing the method to be tested
            LMStudioChatModelFactory factory = new LMStudioChatModelFactory();

            // Create a dummy ChatModel
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("lmstudio");

            // Call the method
            ChatModel result = factory.createChatModel(customChatModel);
            assertThat(result).isNotNull();
        }
    }
}
