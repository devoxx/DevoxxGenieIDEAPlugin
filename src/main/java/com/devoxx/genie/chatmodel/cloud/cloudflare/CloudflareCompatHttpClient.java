package com.devoxx.genie.chatmodel.cloud.cloudflare;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * An {@link HttpClient} that runs every outgoing request body through
 * {@link CloudflareCompatRequestNormalizer} before handing it to the real client.
 *
 * <p>Issue #1256: langchain4j speaks correct OpenAI, but Cloudflare's {@code /compat} endpoint
 * passes the messages on to Workers AI, whose schema is stricter. This is the only seam
 * langchain4j offers for touching the serialised request, so the Cloudflare-specific reshaping
 * lives here instead of leaking into the shared OpenAI code path.</p>
 */
public class CloudflareCompatHttpClient implements HttpClient {

    private final HttpClient delegate;

    public CloudflareCompatHttpClient(@NotNull HttpClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException {
        return delegate.execute(normalize(request));
    }

    @Override
    public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        delegate.execute(normalize(request), parser, listener);
    }

    private static HttpRequest normalize(@NotNull HttpRequest request) {
        String body = request.body();
        String normalized = CloudflareCompatRequestNormalizer.normalize(body);
        if (normalized == null || normalized.equals(body)) {
            return request;
        }
        return HttpRequest.builder()
                .method(request.method())
                .url(request.url())
                .headers(request.headers())
                .body(normalized)
                .build();
    }

    /**
     * Wraps the JDK client builder so langchain4j's timeout settings still reach the real client.
     * The concrete {@link JdkHttpClientBuilder} is created directly rather than via
     * {@code HttpClientBuilderLoader}, which throws when several HTTP client SPI factories share
     * the classpath — a real risk inside an IDE plugin.
     */
    public static @NotNull HttpClientBuilder builder() {
        return new Builder(new JdkHttpClientBuilder());
    }

    private record Builder(HttpClientBuilder delegate) implements HttpClientBuilder {

        @Override
        public Duration connectTimeout() {
            return delegate.connectTimeout();
        }

        @Override
        public HttpClientBuilder connectTimeout(Duration timeout) {
            delegate.connectTimeout(timeout);
            return this;
        }

        @Override
        public Duration readTimeout() {
            return delegate.readTimeout();
        }

        @Override
        public HttpClientBuilder readTimeout(Duration timeout) {
            delegate.readTimeout(timeout);
            return this;
        }

        @Override
        public HttpClient build() {
            return new CloudflareCompatHttpClient(delegate.build());
        }
    }
}
