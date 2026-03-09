package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaChatModelFactoryTest {

    @Test
    void testCreateChatModelDoesNotSendNumCtxWithoutExplicitOverride() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");
            customChatModel.setBaseUrl("http://localhost:8080");
            customChatModel.setContextWindow(8192);

            ChatModel result = factory.createChatModel(customChatModel);
            assertThat(result).isNotNull();
            assertThat(((OllamaChatModel) result).defaultRequestParameters().numCtx()).isNull();
        }
    }

    @Test
    void testCreateChatModelSendsNumCtxWhenExplicitOverrideIsSet() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");
            customChatModel.setBaseUrl("http://localhost:8080");
            customChatModel.setContextWindow(8192);
            customChatModel.setContextWindowOverride(8192);

            ChatModel result = factory.createChatModel(customChatModel);

            assertThat(result).isNotNull();
            assertThat(((OllamaChatModel) result).defaultRequestParameters().numCtx()).isEqualTo(8192);
        }
    }

    @Test
    void testCreateStreamingChatModelDoesNotSendNumCtxWithoutExplicitOverride() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");
            customChatModel.setBaseUrl("http://localhost:8080");
            customChatModel.setContextWindow(8192);

            StreamingChatModel result = factory.createStreamingChatModel(customChatModel);

            assertThat(result).isNotNull();
            assertThat(((OllamaStreamingChatModel) result).defaultRequestParameters().numCtx()).isNull();
        }
    }

    @Test
    void testCreateStreamingChatModelSendsNumCtxWhenExplicitOverrideIsSet() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");
            customChatModel.setBaseUrl("http://localhost:8080");
            customChatModel.setContextWindow(8192);
            customChatModel.setContextWindowOverride(8192);

            StreamingChatModel result = factory.createStreamingChatModel(customChatModel);

            assertThat(result).isNotNull();
            assertThat(((OllamaStreamingChatModel) result).defaultRequestParameters().numCtx()).isEqualTo(8192);
        }
    }
}
