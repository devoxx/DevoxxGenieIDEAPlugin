package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.debug.RawTrafficListenerService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaChatModelFactoryTest {

    private static boolean returnThinking(Object ollamaModel) {
        try {
            Field field = ollamaModel.getClass().getSuperclass().getDeclaredField("returnThinking");
            field.setAccessible(true);
            return (boolean) field.get(ollamaModel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read LangChain4j Ollama returnThinking flag", e);
        }
    }

    @Test
    void testCreateChatModelDoesNotEnableThinkingWhenSettingDisabled() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");
            when(mockSettingsState.getShowThinkingEnabled()).thenReturn(false);

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");

            ChatModel result = factory.createChatModel(customChatModel);

            assertThat(((OllamaChatModel) result).defaultRequestParameters().think()).isNull();
            assertThat(returnThinking(result)).isFalse();
        }
    }

    @Test
    void testCreateChatModelEnablesThinkingWhenSettingEnabled() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");
            when(mockSettingsState.getShowThinkingEnabled()).thenReturn(true);

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");

            ChatModel result = factory.createChatModel(customChatModel);

            assertThat(((OllamaChatModel) result).defaultRequestParameters().think()).isTrue();
            assertThat(returnThinking(result)).isTrue();
        }
    }

    @Test
    void testCreateStreamingChatModelEnablesThinkingWhenSettingEnabled() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);
            when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");
            when(mockSettingsState.getShowThinkingEnabled()).thenReturn(true);

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");

            StreamingChatModel result = factory.createStreamingChatModel(customChatModel);

            assertThat(((OllamaStreamingChatModel) result).defaultRequestParameters().think()).isTrue();
            assertThat(returnThinking(result)).isTrue();
        }
    }

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

    @Test
    void testCreateChatModelAttachesRawTrafficListenerWhenEnabled() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mockStateWithRawLoggingEnabled();
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");

            ChatModel result = factory.createChatModel(customChatModel);

            org.assertj.core.api.Assertions.assertThat(result.listeners())
                    .anyMatch(RawTrafficListenerService.class::isInstance);
        }
    }

    @Test
    void testCreateStreamingChatModelAttachesRawTrafficListenerWhenEnabled() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockSettingsState = mockStateWithRawLoggingEnabled();
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockSettingsState);

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("ollama");

            StreamingChatModel result = factory.createStreamingChatModel(customChatModel);

            org.assertj.core.api.Assertions.assertThat(result.listeners())
                    .anyMatch(RawTrafficListenerService.class::isInstance);
        }
    }

    @Test
    void testBuildLanguageModelFallsBackToDefaultContextWhenApiShowFails() throws Exception {
        // /api/tags succeeded (the model is listed by Ollama), but the per-model /api/show
        // context lookup fails (e.g. model not loaded, slow endpoint, non-2xx). The model
        // must still be listed with the default context window instead of being dropped.
        OllamaModelEntryDTO entry = new OllamaModelEntryDTO();
        entry.setName("llama3.2:3b");

        try (MockedStatic<OllamaApiService> mockedApi = Mockito.mockStatic(OllamaApiService.class)) {
            mockedApi.when(() -> OllamaApiService.getModelContext("llama3.2:3b"))
                    .thenThrow(new java.io.IOException("Unexpected code HTTP 500"));

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            LanguageModel model = factory.buildLanguageModel(entry);

            assertThat(model).isNotNull();
            assertThat(model.getModelName()).isEqualTo("llama3.2:3b");
            assertThat(model.getInputMaxTokens()).isEqualTo(OllamaApiService.DEFAULT_CONTEXT_LENGTH);
        }
    }

    @Test
    void testBuildLanguageModelUsesContextFromApiShowWhenAvailable() throws Exception {
        OllamaModelEntryDTO entry = new OllamaModelEntryDTO();
        entry.setName("llama3.2:3b");

        try (MockedStatic<OllamaApiService> mockedApi = Mockito.mockStatic(OllamaApiService.class)) {
            mockedApi.when(() -> OllamaApiService.getModelContext("llama3.2:3b")).thenReturn(16384);

            OllamaChatModelFactory factory = new OllamaChatModelFactory();
            LanguageModel model = factory.buildLanguageModel(entry);

            assertThat(model).isNotNull();
            assertThat(model.getInputMaxTokens()).isEqualTo(16384);
        }
    }

    private static DevoxxGenieStateService mockStateWithRawLoggingEnabled() {
        DevoxxGenieStateService mockSettingsState = mock(DevoxxGenieStateService.class);
        when(mockSettingsState.getOllamaModelUrl()).thenReturn("http://localhost:8080");
        when(mockSettingsState.getShowThinkingEnabled()).thenReturn(false);
        when(mockSettingsState.getMcpEnabled()).thenReturn(false);
        when(mockSettingsState.getAgentModeEnabled()).thenReturn(false);
        when(mockSettingsState.getRawRequestResponseLoggingEnabled()).thenReturn(true);
        return mockSettingsState;
    }
}
