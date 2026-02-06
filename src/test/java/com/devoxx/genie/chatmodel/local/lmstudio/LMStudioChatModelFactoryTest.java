package com.devoxx.genie.chatmodel.local.lmstudio;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import dev.langchain4j.model.chat.ChatModel;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LMStudioChatModelFactoryTest {
    private final Gson gson = new Gson();

    @Test
    void testCreateChatModel() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            // Setup the mock for SettingsState
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getLmstudioModelUrl()).thenReturn("http://localhost:1234/v1/");

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

    @Test
    void buildLanguageModel_usesConfiguredFallbackContextLengthWhenModelContextIsMissing() throws Exception {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getLmStudioFallbackContextLength()).thenReturn(131072);

            LMStudioChatModelFactory factory = new LMStudioChatModelFactory();
            LMStudioModelEntryDTO modelEntryDTO = new LMStudioModelEntryDTO();
            modelEntryDTO.setId("test-model");

            Method buildLanguageModel = LMStudioChatModelFactory.class.getDeclaredMethod("buildLanguageModel", Object.class);
            buildLanguageModel.setAccessible(true);
            LanguageModel languageModel = (LanguageModel) buildLanguageModel.invoke(factory, modelEntryDTO);

            assertThat(languageModel.getInputMaxTokens()).isEqualTo(131072);
        }
    }

    @Test
    void buildLanguageModel_supportsApiV1ModelsResponseShape() throws Exception {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getLmStudioFallbackContextLength()).thenReturn(null);

            LMStudioChatModelFactory factory = new LMStudioChatModelFactory();
            LMStudioModelEntryDTO modelEntryDTO = gson.fromJson("""
                    {
                      "key": "text-embedding-nomic-embed-text-v1.5",
                      "display_name": "Nomic Embed Text v1.5",
                      "max_context_length": 2048
                    }
                    """, LMStudioModelEntryDTO.class);

            Method buildLanguageModel = LMStudioChatModelFactory.class.getDeclaredMethod("buildLanguageModel", Object.class);
            buildLanguageModel.setAccessible(true);
            LanguageModel languageModel = (LanguageModel) buildLanguageModel.invoke(factory, modelEntryDTO);

            assertThat(languageModel.getModelName()).isEqualTo("text-embedding-nomic-embed-text-v1.5");
            assertThat(languageModel.getDisplayName()).isEqualTo("Nomic Embed Text v1.5");
            assertThat(languageModel.getInputMaxTokens()).isEqualTo(2048);
        }
    }
}
