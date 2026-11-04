package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingProviderValidatorTest {

    private MockedStatic<DevoxxGenieStateService> settings;
    private DevoxxGenieStateService state;
    private final EmbeddingProviderValidator validator = new EmbeddingProviderValidator();

    @BeforeEach
    void setUp() {
        settings = Mockito.mockStatic(DevoxxGenieStateService.class);
        state = mock(DevoxxGenieStateService.class);
        settings.when(DevoxxGenieStateService::getInstance).thenReturn(state);
        when(state.getRagEmbeddingProvider()).thenReturn(EmbeddingProvider.CUSTOM_OPENAI.id());
        when(state.getRagEmbeddingModelName()).thenReturn("my-model");
        when(state.isCustomOpenAIUrlEnabled()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        settings.close();
    }

    @Test
    void anyHttpResponseCountsAsReachable() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            // A server without /models, or one that wants the key, is still running.
            server.enqueue(new MockResponse().setResponseCode(401));
            server.start();
            when(state.getCustomOpenAIUrl()).thenReturn(server.url("/v1").toString());

            assertThat(validator.isValid()).isTrue();
            assertThat(server.takeRequest().getPath()).isEqualTo("/v1/models");
        }
    }

    @Test
    void serverThatIsDownIsReported() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        String url = server.url("/v1").toString();
        server.close();
        when(state.getCustomOpenAIUrl()).thenReturn(url);

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).contains("not reachable").contains(url);
    }

    @Test
    void providerDisabledInTheLlmSettingsIsReported() {
        when(state.isCustomOpenAIUrlEnabled()).thenReturn(false);

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).contains("Custom OpenAI").contains("disabled");
    }

    @Test
    void missingUrlAndModelAreReported() {
        when(state.getCustomOpenAIUrl()).thenReturn("");
        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).contains("URL");

        when(state.getCustomOpenAIUrl()).thenReturn("http://localhost:1/v1");
        when(state.getRagEmbeddingModelName()).thenReturn(" ");
        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getErrorMessage()).contains("embedding model");
    }
}
