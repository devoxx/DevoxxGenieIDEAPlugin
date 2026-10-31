package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaApiServiceTest {

    /**
     * Ollama answers 200 with an EMPTY body for some models (observed live with
     * gemma3n:e4b). gson.fromJson("") returns null, which NPEs in findContextLength
     * and — because the NPE escapes the per-model task — hides the ENTIRE model list.
     * The probe must fall back to the default context length instead.
     */
    @Test
    void testGetModelContextReturnsDefaultWhenApiShowReturnsEmptyBody() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody(""));
            server.start();

            try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
                DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
                mockedSettings.when(DevoxxGenieStateService::getInstance).thenReturn(state);
                when(state.getOllamaModelUrl()).thenReturn(server.url("/").toString());

                assertThat(OllamaApiService.getModelContext("gemma3n:e4b"))
                        .isEqualTo(OllamaApiService.DEFAULT_CONTEXT_LENGTH);
            }
        }
    }

    @Test
    void testGetModelContextThrowsWhenApiShowReturnsError() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(404)
                    .setBody("{\"error\":\"model 'missing:1b' not found\"}"));
            server.start();

            try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
                DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
                mockedSettings.when(DevoxxGenieStateService::getInstance).thenReturn(state);
                when(state.getOllamaModelUrl()).thenReturn(server.url("/").toString());

                assertThatThrownBy(() -> OllamaApiService.getModelContext("missing:1b"))
                        .isInstanceOf(IOException.class);
            }
        }
    }

    @Test
    void testGetModelContextParsesContextLengthFromModelInfo() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"model_info\":{\"param.context_length\":131072}}"));
            server.start();

            try (MockedStatic<DevoxxGenieStateService> mockedSettings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
                DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
                mockedSettings.when(DevoxxGenieStateService::getInstance).thenReturn(state);
                when(state.getOllamaModelUrl()).thenReturn(server.url("/").toString());

                assertThat(OllamaApiService.getModelContext("mistral-small3.1:latest"))
                        .isEqualTo(131072);
            }
        }
    }
}
