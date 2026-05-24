package com.devoxx.genie.chatmodel.local.jan;

import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Wraps another {@link HttpClientBuilder} so the built client compacts JSON request bodies.
 * See {@link CompactJsonHttpClient} for the why (issue #1051).
 *
 * <p>Timeout configuration is delegated to the wrapped builder; the setters return
 * {@code this} so the caller keeps configuring (and ultimately builds) the wrapper rather
 * than the bare delegate.
 */
public class CompactJsonHttpClientBuilder implements HttpClientBuilder {

    private final HttpClientBuilder delegate;

    public CompactJsonHttpClientBuilder(@NotNull HttpClientBuilder delegate) {
        this.delegate = delegate;
    }

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
        return new CompactJsonHttpClient(delegate.build());
    }
}
