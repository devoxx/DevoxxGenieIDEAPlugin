package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaModelServicePullTest {

    /** Recorded from a live Ollama: a failed pull is HTTP 200 with an "error" line in the stream. */
    @Test
    void pullOfAModelThatDoesNotExistFails() throws Exception {
        String body = """
                {"status":"pulling manifest"}
                {"error":"pull model manifest: file does not exist"}
                """;

        assertThatThrownBy(() -> pull(body))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("file does not exist");
    }

    @Test
    void successfulPullReportsItsProgress() throws Exception {
        List<String> statuses = pull("""
                {"status":"pulling manifest"}
                {"status":"pulling abc","completed":50,"total":100}
                {"status":"success"}
                """);

        assertThat(statuses).contains("pulling manifest", "Downloading: 50.0%", "success");
    }

    private static List<String> pull(String body) throws IOException {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody(body));
            server.start();

            try (MockedStatic<DevoxxGenieStateService> settings = Mockito.mockStatic(DevoxxGenieStateService.class)) {
                DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
                settings.when(DevoxxGenieStateService::getInstance).thenReturn(state);
                when(state.getOllamaModelUrl()).thenReturn(server.url("/").toString());

                List<String> statuses = new ArrayList<>();
                new OllamaModelService().pullModel("some-model", statuses::add);
                return statuses;
            }
        }
    }
}
