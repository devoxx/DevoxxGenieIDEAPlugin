package com.devoxx.genie.chatmodel.local.jan;

import com.google.gson.JsonParser;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Regression tests for issue #1051.
 *
 * <p>langchain4j's OpenAI client serializes request bodies as pretty-printed (multi-line)
 * JSON. Jan v0.8.0's bundled llama.cpp HTTP server hangs forever on a request body that
 * contains newlines, so chat requests never get a response. {@link CompactJsonHttpClient}
 * rewrites the body to single-line JSON before it reaches the server.
 */
class CompactJsonHttpClientTest {

    private static final String PRETTY_BODY = """
            {
              "model" : "gemma4-e2b-it.gguf",
              "messages" : [ {
                "role" : "user",
                "content" : "hey"
              } ],
              "stream" : true
            }""";

    @Test
    void compactsPrettyPrintedJsonBodyForNonStreaming() {
        HttpClient delegate = mock(HttpClient.class);
        HttpRequest request = jsonRequest(PRETTY_BODY);

        new CompactJsonHttpClient(delegate).execute(request);

        HttpRequest forwarded = captureNonStreaming(delegate);
        assertThat(forwarded.body()).doesNotContain("\n");
        assertThat(JsonParser.parseString(forwarded.body()))
                .isEqualTo(JsonParser.parseString(PRETTY_BODY));
        // Method, url and headers are preserved.
        assertThat(forwarded.url()).isEqualTo(request.url());
        assertThat(forwarded.method()).isEqualTo(request.method());
        assertThat(forwarded.headers()).isEqualTo(request.headers());
    }

    @Test
    void compactsPrettyPrintedJsonBodyForStreaming() {
        HttpClient delegate = mock(HttpClient.class);
        ServerSentEventParser parser = mock(ServerSentEventParser.class);
        ServerSentEventListener listener = mock(ServerSentEventListener.class);

        new CompactJsonHttpClient(delegate).execute(jsonRequest(PRETTY_BODY), parser, listener);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(delegate).execute(captor.capture(), org.mockito.ArgumentMatchers.eq(parser),
                org.mockito.ArgumentMatchers.eq(listener));
        assertThat(captor.getValue().body()).doesNotContain("\n");
    }

    @Test
    void preservesNewlinesInsideStringValuesAsEscapes() {
        HttpClient delegate = mock(HttpClient.class);
        String body = "{\n  \"content\" : \"line1\\nline2\"\n}";

        new CompactJsonHttpClient(delegate).execute(jsonRequest(body));

        HttpRequest forwarded = captureNonStreaming(delegate);
        // No structural newlines, but the string value's newline survives as an escape.
        assertThat(forwarded.body()).doesNotContain("\n");
        assertThat(forwarded.body()).contains("line1\\nline2");
        assertThat(JsonParser.parseString(forwarded.body()))
                .isEqualTo(JsonParser.parseString(body));
    }

    @Test
    void leavesNonJsonBodyUnchanged() {
        HttpClient delegate = mock(HttpClient.class);
        HttpRequest request = jsonRequest("not json at all");

        new CompactJsonHttpClient(delegate).execute(request);

        assertThat(captureNonStreaming(delegate)).isSameAs(request);
    }

    @Test
    void leavesNullBodyUnchanged() {
        HttpClient delegate = mock(HttpClient.class);
        HttpRequest request = HttpRequest.builder()
                .method(HttpMethod.GET)
                .url("http://localhost:1337/v1/models")
                .build();

        new CompactJsonHttpClient(delegate).execute(request);

        assertThat(captureNonStreaming(delegate)).isSameAs(request);
    }

    private static HttpRequest jsonRequest(String body) {
        return HttpRequest.builder()
                .method(HttpMethod.POST)
                .url("http://localhost:1337/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build();
    }

    private static HttpRequest captureNonStreaming(HttpClient delegate) {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(delegate).execute(captor.capture());
        return captor.getValue();
    }
}
