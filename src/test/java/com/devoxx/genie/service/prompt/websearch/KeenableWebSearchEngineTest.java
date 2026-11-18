package com.devoxx.genie.service.prompt.websearch;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class KeenableWebSearchEngineTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void keylessSearchUsesPublicEndpointAndTitleHeader() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> title = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        start(exchange -> {
            path.set(exchange.getRequestURI().getPath());
            title.set(exchange.getRequestHeaders().getFirst("X-Keenable-Title"));
            apiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            write(exchange, 200, """
                    {"results":[
                      {"title":"Example","url":"https://example.test","snippet":"A result"},
                      {"title":"Dropped","url":"","snippet":"no url"}
                    ]}
                    """);
        });

        WebSearchResults results = engine(null).search(WebSearchRequest.builder()
                .searchTerms("open web search")
                .maxResults(51)
                .build());

        assertThat(path.get()).isEqualTo("/v1/search/public");
        assertThat(title.get()).isEqualTo("DevoxxGenie");
        assertThat(apiKey.get()).isNull();
        assertThat(body.get()).isEqualTo("{\"query\":\"open web search\",\"max_results\":50}");
        List<WebSearchOrganicResult> hits = results.results();
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).title()).isEqualTo("Example");
        assertThat(hits.get(0).url().toString()).isEqualTo("https://example.test");
        assertThat(hits.get(0).snippet()).isEqualTo("A result");
    }

    @Test
    void keyedSearchUsesAuthenticatedEndpoint() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        start(exchange -> {
            path.set(exchange.getRequestURI().getPath());
            apiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            write(exchange, 200, "{\"results\":[{\"title\":\"Keyed\",\"url\":\"https://example.test/keyed\",\"snippet\":\"ok\"}]}");
        });

        WebSearchResults results = engine("secret-key").search(WebSearchRequest.from("query"));
        assertThat(path.get()).isEqualTo("/v1/search");
        assertThat(apiKey.get()).isEqualTo("secret-key");
        assertThat(results.results().get(0).url().toString()).isEqualTo("https://example.test/keyed");
    }

    @Test
    void doesNotForwardApiKeyOnCrossHostRedirect() throws Exception {
        HttpServer other = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> forwardedKey = new AtomicReference<>("unset");
        other.createContext("/v1/search", exchange -> {
            forwardedKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            write(exchange, 200, "{\"results\":[]}");
        });
        other.start();
        try {
            start(exchange -> {
                String location = "http://127.0.0.1:" + other.getAddress().getPort() + "/v1/search";
                exchange.getResponseHeaders().add("Location", location);
                write(exchange, 302, "");
            });
            engine("secret-key").search(WebSearchRequest.from("query"));
            assertThat(forwardedKey.get()).isNull();
        } finally {
            other.stop(0);
        }
    }

    private KeenableWebSearchEngine engine(String apiKey) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new KeenableWebSearchEngine(apiKey, "http://127.0.0.1:" + server.getAddress().getPort(), client);
    }

    private void start(Handler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private static void write(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}
