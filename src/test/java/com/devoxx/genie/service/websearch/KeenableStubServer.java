package com.devoxx.genie.service.websearch;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A local Keenable stand-in for end-to-end tests: the real {@link KeenableWebSearchEngine} (via
 * {@link WebSearchService}) talks HTTP to this server, so no real network is used.
 */
public final class KeenableStubServer implements AutoCloseable {

    private final MockWebServer server = new MockWebServer();

    public KeenableStubServer() throws IOException {
        server.start();
        String url = server.url("/").toString();
        WebSearchService.useKeenableBaseUrlForTesting(url.substring(0, url.length() - 1));
    }

    public KeenableStubServer respondWithHit(String title, String url, String snippet) {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"results":[{"title":"%s","url":"%s","snippet":"%s"}]}""".formatted(title, url, snippet)));
        return this;
    }

    public KeenableStubServer respondWithNoResults() {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{\"results\":[]}"));
        return this;
    }

    public KeenableStubServer respondWithStatus(int status) {
        server.enqueue(new MockResponse().setResponseCode(status).setBody("{\"detail\":\"stub " + status + "\"}"));
        return this;
    }

    public int requestCount() {
        return server.getRequestCount();
    }

    public RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        if (request == null) {
            throw new AssertionError("Keenable stub received no request");
        }
        return request;
    }

    /** Leaves other test overrides alone; callers reset with {@link WebSearchServiceTestSupport#reset()}. */
    @Override
    public void close() throws IOException {
        server.shutdown();
    }
}
