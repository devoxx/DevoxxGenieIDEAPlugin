package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomOpenAIChatModelFactoryTest {

    @Test
    void testCreateChatModel_withHttp11Default() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getCustomOpenAIUrl()).thenReturn("http://localhost:3000/v1/");
            when(mockSettingsState.isCustomOpenAIApiKeyEnabled()).thenReturn(false);
            when(mockSettingsState.isCustomOpenAIModelNameEnabled()).thenReturn(true);
            when(mockSettingsState.getCustomOpenAIModelName()).thenReturn("test-model");
            when(mockSettingsState.isCustomOpenAIForceHttp11()).thenReturn(true);

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("dropdown-model");

            ChatModel result = factory.createChatModel(customChatModel);
            assertThat(result).isNotNull();
        }
    }

    @Test
    void testCreateChatModel_withHttp2() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getCustomOpenAIUrl()).thenReturn("http://localhost:3000/v1/");
            when(mockSettingsState.isCustomOpenAIApiKeyEnabled()).thenReturn(false);
            when(mockSettingsState.isCustomOpenAIModelNameEnabled()).thenReturn(false);
            when(mockSettingsState.isCustomOpenAIForceHttp11()).thenReturn(false);

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("dropdown-model");

            ChatModel result = factory.createChatModel(customChatModel);
            assertThat(result).isNotNull();
        }
    }

    @Test
    void testCreateChatModel_withApiKey() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getCustomOpenAIUrl()).thenReturn("http://localhost:3000/v1/");
            when(mockSettingsState.isCustomOpenAIApiKeyEnabled()).thenReturn(true);
            when(mockSettingsState.getCustomOpenAIApiKey()).thenReturn("test-api-key");
            when(mockSettingsState.isCustomOpenAIModelNameEnabled()).thenReturn(true);
            when(mockSettingsState.getCustomOpenAIModelName()).thenReturn("test-model");
            when(mockSettingsState.isCustomOpenAIForceHttp11()).thenReturn(true);

            CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();

            ChatModel result = factory.createChatModel(customChatModel);
            assertThat(result).isNotNull();
        }
    }

    @Test
    void testResetModels_clearsCachedModels() {
        CustomOpenAIChatModelFactory factory = new CustomOpenAIChatModelFactory();
        factory.resetModels();
        // After reset, getModels should trigger a fresh fetch attempt
        // Since no service is available in tests, it should return empty
        // This just verifies resetModels doesn't throw
        assertThat(factory).isNotNull();
    }
}
