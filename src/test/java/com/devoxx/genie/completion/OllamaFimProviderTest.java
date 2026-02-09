package com.devoxx.genie.completion;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaFimProviderTest {

    private MockWebServer server;
    private OllamaFimProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        provider = new OllamaFimProvider();
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            server.shutdown();
        } catch (IOException e) {
            // Ignore shutdown errors from pending delayed responses
        }
    }

    @Test
    void shouldReturnCompletionOnSuccess() throws InterruptedException {
        String responseJson = "{\"response\":\"System.out.println()\",\"done\":true}";
        server.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        FimRequest request = FimRequest.builder()
                .prefix("public void main() {\n    ")
                .suffix("\n}")
                .modelName("starcoder2:3b")
                .baseUrl(server.url("/").toString())
                .maxTokens(64)
                .temperature(0.0)
                .timeoutMs(5000)
                .build();

        FimResponse response = provider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.getCompletionText()).isEqualTo("System.out.println()");
        assertThat(response.getDurationMs()).isGreaterThanOrEqualTo(0);

        // Verify the request body
        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/api/generate");
        String body = recordedRequest.getBody().readUtf8();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        assertThat(json.get("model").getAsString()).isEqualTo("starcoder2:3b");
        assertThat(json.get("prompt").getAsString()).isEqualTo("public void main() {\n    ");
        assertThat(json.get("suffix").getAsString()).isEqualTo("\n}");
        assertThat(json.get("stream").getAsBoolean()).isFalse();
        assertThat(json.getAsJsonObject("options").get("num_predict").getAsInt()).isEqualTo(64);
        assertThat(json.getAsJsonObject("options").get("temperature").getAsDouble()).isEqualTo(0.0);
    }

    @Test
    void shouldReturnNullOnEmptyResponse() {
        String responseJson = "{\"response\":\"\",\"done\":true}";
        server.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        FimRequest request = createRequest();

        FimResponse response = provider.generate(request);
        assertThat(response).isNull();
    }

    @Test
    void shouldReturnNullOnHttpError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        FimRequest request = createRequest();

        FimResponse response = provider.generate(request);
        assertThat(response).isNull();
    }

    @Test
    void shouldReturnNullOnTimeout() {
        // No response enqueued — will time out
        server.enqueue(new MockResponse()
                .setBody("{\"response\":\"late\",\"done\":true}")
                .setHeadersDelay(6, java.util.concurrent.TimeUnit.SECONDS));

        FimRequest request = FimRequest.builder()
                .prefix("test")
                .suffix("")
                .modelName("test-model")
                .baseUrl(server.url("/").toString())
                .timeoutMs(500)
                .build();

        FimResponse response = provider.generate(request);
        assertThat(response).isNull();
    }

    @Test
    void shouldTrimTrailingWhitespace() {
        String responseJson = "{\"response\":\"hello()  \\n  \",\"done\":true}";
        server.enqueue(new MockResponse()
                .setBody(responseJson)
                .setHeader("Content-Type", "application/json"));

        FimRequest request = createRequest();

        FimResponse response = provider.generate(request);
        assertThat(response).isNotNull();
        assertThat(response.getCompletionText()).isEqualTo("hello()");
    }

    @Test
    void shouldCancelPreviousRequest() throws InterruptedException {
        // First request will be slow
        server.enqueue(new MockResponse()
                .setBody("{\"response\":\"slow\",\"done\":true}")
                .setBodyDelay(5, java.util.concurrent.TimeUnit.SECONDS));

        // Second request will be fast
        server.enqueue(new MockResponse()
                .setBody("{\"response\":\"fast\",\"done\":true}")
                .setHeader("Content-Type", "application/json"));

        FimRequest request = createRequest();

        // Start first request in background
        Thread firstThread = new Thread(() -> provider.generate(request));
        firstThread.start();

        // Wait a bit then cancel
        Thread.sleep(100);
        provider.cancelActiveCall();

        firstThread.join(2000);
        assertThat(firstThread.isAlive()).isFalse();
    }

    private FimRequest createRequest() {
        return FimRequest.builder()
                .prefix("test prefix")
                .suffix("test suffix")
                .modelName("test-model")
                .baseUrl(server.url("/").toString())
                .maxTokens(64)
                .temperature(0.0)
                .timeoutMs(5000)
                .build();
    }
}
