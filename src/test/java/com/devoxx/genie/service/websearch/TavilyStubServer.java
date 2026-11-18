package com.devoxx.genie.service.websearch;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A local Tavily stand-in for end-to-end tests: the real {@code TavilyWebSearchEngine} (via
 * {@link WebSearchService}) talks HTTP to this server, so no real network is used.
 */
public final class TavilyStubServer implements AutoCloseable {

    private final MockWebServer server = new MockWebServer();

    public TavilyStubServer() throws IOException {
        server.start();
        WebSearchService.useTavilyBaseUrlForTesting(server.url("/").toString());
    }

    /** Queues one successful search response with a single hit. */
    public TavilyStubServer respondWithHit(String title, String url, String content) {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"query":"q","response_time":0.1,"results":[
                          {"title":"%s","url":"%s","content":"%s","score":0.9}
                        ]}""".formatted(title, url, content)));
        return this;
    }

    public TavilyStubServer respondWithNoResults() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"query\":\"q\",\"response_time\":0.1,\"results\":[]}"));
        return this;
    }

    public TavilyStubServer respondWithStatus(int status, String body) {
        server.enqueue(new MockResponse().setResponseCode(status).setBody(body));
        return this;
    }

    public int requestCount() {
        return server.getRequestCount();
    }

    /** The next recorded request, failing fast instead of hanging when none arrived. */
    public RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        if (request == null) {
            throw new AssertionError("Tavily stub received no request");
        }
        return request;
    }

    @Override
    public void close() throws IOException {
        WebSearchService.resetDefinitionsForTesting();
        server.shutdown();
    }
}
