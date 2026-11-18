package com.devoxx.genie.service.websearch;

import org.jetbrains.annotations.NotNull;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

/** HTTP defaults shared by the in-repo web search engines (Keenable, Parallel). */
final class WebSearchHttp {

    /** Sent by every in-repo engine so providers can identify DevoxxGenie traffic. */
    static final String USER_AGENT = "DevoxxGenie";

    private WebSearchHttp() {
    }

    /**
     * A client that never follows redirects on its own (engines decide what may follow a redirect, so an API
     * key never leaks to another origin) and uses the JVM proxy selector, which is the IDE's proxy setup.
     * java.net.http.HttpClient uses no proxy unless told to.
     */
    static @NotNull HttpClient defaultHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER);
        ProxySelector proxySelector = ProxySelector.getDefault();
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        return builder.build();
    }
}
