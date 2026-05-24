package com.devoxx.genie.chatmodel.local.jan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import org.jetbrains.annotations.NotNull;

/**
 * A langchain4j {@link HttpClient} decorator that rewrites JSON request bodies to
 * single-line (compact) form before delegating.
 *
 * <p>Workaround for issue #1051: langchain4j's OpenAI client serializes request bodies as
 * pretty-printed (multi-line) JSON, and Jan v0.8.0's bundled llama.cpp HTTP server hangs
 * indefinitely on a request body that contains newlines — it never sends a response, so the
 * chat request times out with no tokens. Compacting the body sidesteps the server bug while
 * keeping the payload semantically identical.
 */
public class CompactJsonHttpClient implements HttpClient {

    // disableHtmlEscaping keeps characters like '<' intact instead of emitting <.
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final HttpClient delegate;

    public CompactJsonHttpClient(@NotNull HttpClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException {
        return delegate.execute(compact(request));
    }

    @Override
    public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        delegate.execute(compact(request), parser, listener);
    }

    /**
     * Returns a copy of {@code request} with its JSON body compacted, or the original request
     * when the body is empty, already compact, or not valid JSON.
     */
    private static @NotNull HttpRequest compact(@NotNull HttpRequest request) {
        String body = request.body();
        if (body == null || body.isBlank()) {
            return request;
        }

        String compact;
        try {
            compact = GSON.toJson(JsonParser.parseString(body));
        } catch (RuntimeException notJson) {
            return request;
        }

        if (compact.equals(body)) {
            return request;
        }

        return HttpRequest.builder()
                .method(request.method())
                .url(request.url())
                .headers(request.headers())
                .body(compact)
                .build();
    }
}
