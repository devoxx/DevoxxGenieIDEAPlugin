package com.devoxx.genie.service.websearch;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // --- TASK-277.3 hardening ---------------------------------------------------------------------

    @Test
    void requestsIdentifyDevoxxGenie_withUserAgentAndTitleHeader() throws Exception {
        AtomicReference<String> userAgent = new AtomicReference<>();
        AtomicReference<String> title = new AtomicReference<>();
        start(exchange -> {
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            title.set(exchange.getRequestHeaders().getFirst("X-Keenable-Title"));
            write(exchange, 200, "{\"results\":[]}");
        });

        engine(null).search(WebSearchRequest.from("q"));

        assertThat(userAgent.get()).isEqualTo("DevoxxGenie");
        assertThat(title.get()).isEqualTo("DevoxxGenie");
    }

    @Test
    void queryWithQuotesBackslashesAndControlCharacters_isSentAsValidJson() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        start(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            write(exchange, 200, "{\"results\":[]}");
        });
        String query = "say \"hi\" C:\\path\ttab\u0001ctrl\u2028sep";

        engine(null).search(WebSearchRequest.builder().searchTerms(query).maxResults(3).build());

        com.google.gson.JsonObject sent = com.google.gson.JsonParser.parseString(body.get()).getAsJsonObject();
        assertThat(sent.get("query").getAsString()).isEqualTo(query);
        assertThat(sent.get("max_results").getAsInt()).isEqualTo(3);
    }

    @Test
    void keyIsKeptOnSameOriginRedirect() throws Exception {
        AtomicReference<String> keyOnTarget = new AtomicReference<>("unset");
        start(exchange -> {
            if (exchange.getRequestURI().getPath().equals("/v1/search")) {
                exchange.getResponseHeaders().add("Location", "/v2/search");
                write(exchange, 307, "");
            } else {
                keyOnTarget.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
                write(exchange, 200, "{\"results\":[]}");
            }
        });

        engine("secret-key").search(WebSearchRequest.from("q"));

        assertThat(keyOnTarget.get()).isEqualTo("secret-key");
    }

    @Test
    void sameOrigin_comparesSchemeHostAndEffectivePort() {
        assertThat(KeenableWebSearchEngine.sameOrigin(URI.create("https://api.keenable.ai/a"),
                URI.create("https://API.keenable.ai:443/b"))).isTrue();
        assertThat(KeenableWebSearchEngine.sameOrigin(URI.create("https://api.keenable.ai/a"),
                URI.create("http://api.keenable.ai/a"))).isFalse();
        assertThat(KeenableWebSearchEngine.sameOrigin(URI.create("http://127.0.0.1:8080/a"),
                URI.create("http://127.0.0.1:9090/a"))).isFalse();
        assertThat(KeenableWebSearchEngine.sameOrigin(URI.create("https://api.keenable.ai/a"),
                URI.create("https://evil.example/a"))).isFalse();
    }

    @Test
    void keylessRateLimit_explainsTheFreeLimitAndSuggestsAKey() throws Exception {
        start(exchange -> write(exchange, 429, "{\"detail\":\"too many requests\"}"));

        assertThatThrownBy(() -> engine(null).search(WebSearchRequest.from("q")))
                .isInstanceOfSatisfying(WebSearchProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(WebSearchProviderException.Kind.RATE_LIMITED);
                    assertThat(e.getMessage()).contains("free search limit").contains("API key")
                            .contains("Settings → DevoxxGenie → Web search");
                });
    }

    @Test
    void keyedRateLimit_saysTheKeysLimitWasReached() throws Exception {
        start(exchange -> write(exchange, 429, ""));

        assertThatThrownBy(() -> engine("k").search(WebSearchRequest.from("q")))
                .isInstanceOfSatisfying(WebSearchProviderException.class, e ->
                        assertThat(e.getMessage()).contains("rate limit for your API key"));
    }

    @Test
    void rejectedKey_401and403_sayTheKeyWasRejected_withoutEchoingIt() throws Exception {
        for (int status : new int[]{401, 403}) {
            start(exchange -> write(exchange, status, "{\"detail\":\"bad key secret-key\"}"));

            assertThatThrownBy(() -> engine("secret-key").search(WebSearchRequest.from("q")))
                    .isInstanceOfSatisfying(WebSearchProviderException.class, e -> {
                        assertThat(e.kind()).isEqualTo(WebSearchProviderException.Kind.UNAUTHORIZED);
                        assertThat(e.getMessage()).contains("rejected the API key").contains("HTTP " + status)
                                .doesNotContain("secret-key");
                    });
            server.stop(0);
        }
    }

    @Test
    void serverError_isAGenericFailureWithTheStatus() throws Exception {
        start(exchange -> write(exchange, 503, "upstream down"));

        assertThatThrownBy(() -> engine(null).search(WebSearchRequest.from("q")))
                .isInstanceOfSatisfying(WebSearchProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(WebSearchProviderException.Kind.FAILED);
                    assertThat(e.getMessage()).contains("HTTP 503").doesNotContain("upstream down");
                });
    }

    @Test
    void unreadableResponse_isReportedClearly() throws Exception {
        start(exchange -> write(exchange, 200, "<html>not json</html>"));

        assertThatThrownBy(() -> engine(null).search(WebSearchRequest.from("q")))
                .isInstanceOf(WebSearchProviderException.class)
                .hasMessageContaining("could not read");
    }

    @Test
    void unreachableServer_isReportedWithoutStackTraceDetails() {
        KeenableWebSearchEngine unreachable = new KeenableWebSearchEngine("k", "http://127.0.0.1:1",
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());

        assertThatThrownBy(() -> unreachable.search(WebSearchRequest.from("q")))
                .isInstanceOf(WebSearchProviderException.class)
                .hasMessageStartingWith("Could not reach Keenable");
    }

    @Test
    void parse_mapsResults_skipsBadUrls_respectsLimit_andFallsBackToHostForMissingTitles() {
        WebSearchResults results = KeenableWebSearchEngine.parse("""
                {"results":[
                  {"title":"","url":"https://docs.example.com/a","snippet":"no title"},
                  {"title":"Bad","url":"http://bad host/x","snippet":"bad url"},
                  {"title":"Null url","url":null},
                  "not an object",
                  {"title":"Second","url":"https://example.com/b","snippet":"ok"},
                  {"title":"Over limit","url":"https://example.com/c"}
                ]}""", 2);

        assertThat(results.results()).extracting(WebSearchOrganicResult::title)
                .containsExactly("docs.example.com", "Second");
        assertThat(results.searchInformation().totalResults()).isEqualTo(2L);
    }

    @Test
    void parse_emptyOrMissingResults_isEmpty() {
        assertThat(KeenableWebSearchEngine.parse("{\"results\":[]}", 5).results()).isEmpty();
        assertThat(KeenableWebSearchEngine.parse("{}", 5).results()).isEmpty();
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
