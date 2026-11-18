package com.devoxx.genie.service.websearch;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A local stand-in for Parallel's REST Search API: the real {@link ParallelWebSearchEngine} (via
 * {@link WebSearchService}) talks HTTP to this server, so no real network is used.
 */
public final class ParallelStubServer implements AutoCloseable {

    private final MockWebServer server = new MockWebServer();

    public ParallelStubServer() throws IOException {
        server.start();
        String url = server.url("/").toString();
        WebSearchService.useParallelBaseUrlForTesting(url.substring(0, url.length() - 1));
    }

    public ParallelStubServer respondWithHit(String title, String url, String excerpt) {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"search_id":"s1","session_id":"x","results":[
                  {"url":"%s","title":"%s","publish_date":null,"excerpts":["%s"]}
                ]}""".formatted(url, title, excerpt)));
        return this;
    }

    public ParallelStubServer respondWithNoResults() {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"search_id\":\"s1\",\"session_id\":\"x\",\"results\":[]}"));
        return this;
    }

    public ParallelStubServer respondWithStatus(int status) {
        server.enqueue(new MockResponse().setResponseCode(status)
                .setBody("{\"type\":\"error\",\"error\":{\"ref_id\":\"r\",\"message\":\"stub " + status + "\"}}"));
        return this;
    }

    public int requestCount() {
        return server.getRequestCount();
    }

    public RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        if (request == null) {
            throw new AssertionError("Parallel stub received no request");
        }
        return request;
    }

    /** Leaves other test overrides alone; callers reset with {@link WebSearchServiceTestSupport#reset()}. */
    @Override
    public void close() throws IOException {
        server.shutdown();
    }
}
