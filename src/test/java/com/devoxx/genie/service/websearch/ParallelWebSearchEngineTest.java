package com.devoxx.genie.service.websearch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TASK-277.5: Parallel's REST Search API (POST /v1/search, x-api-key). */
class ParallelWebSearchEngineTest {

    private MockWebServer server;

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stop() throws Exception {
        server.shutdown();
    }

    private ParallelWebSearchEngine engine(String apiKey) {
        String url = server.url("/").toString();
        return new ParallelWebSearchEngine(apiKey, url.substring(0, url.length() - 1),
                WebSearchHttp.defaultHttpClient());
    }

    private void respond(int status, String body) {
        server.enqueue(new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body));
    }

    private RecordedRequest request() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).as("request reached the stub").isNotNull();
        return request;
    }

    @Test
    void sendsTheDocumentedRequest_withKeyUserAgentAndFastMode() throws Exception {
        respond(200, "{\"search_id\":\"s1\",\"results\":[],\"session_id\":\"x\"}");

        engine("par-key").search(WebSearchRequest.builder()
                .searchTerms("intellij \"plugin\" since-build\n").maxResults(4).build());

        RecordedRequest request = request();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/search");
        assertThat(request.getHeader("x-api-key")).isEqualTo("par-key");
        assertThat(request.getHeader("User-Agent")).isEqualTo("DevoxxGenie");
        assertThat(request.getHeader("Content-Type")).startsWith("application/json");
        JsonObject body = JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
        assertThat(body.getAsJsonArray("search_queries").get(0).getAsString()).isEqualTo("intellij \"plugin\" since-build\n");
        assertThat(body.get("objective").getAsString()).isEqualTo("intellij \"plugin\" since-build\n");
        assertThat(body.get("mode").getAsString()).isEqualTo("fast");
        assertThat(body.get("max_chars_total").getAsInt()).isPositive();
        assertThat(body.getAsJsonObject("advanced_settings").get("max_results").getAsInt()).isEqualTo(4);
    }

    @Test
    void mapsResults_joinsExcerpts_fallsBackToHost_andSkipsBadUrls() throws Exception {
        respond(200, """
                {"search_id":"s1","session_id":"x","results":[
                  {"url":"https://plugins.jetbrains.com/docs/a","title":"Build ranges","excerpts":["First part.","Second part."]},
                  {"url":"https://docs.example.com/b","title":null,"excerpts":[]},
                  {"url":"not a valid url","title":"Bad","excerpts":["x"]},
                  {"title":"No url","excerpts":["x"]}
                ]}""");

        WebSearchResults results = engine("k").search(WebSearchRequest.builder().searchTerms("q").maxResults(10).build());

        assertThat(results.results()).extracting(WebSearchOrganicResult::title)
                .containsExactly("Build ranges", "docs.example.com");
        WebSearchOrganicResult first = results.results().get(0);
        assertThat(first.url().toString()).isEqualTo("https://plugins.jetbrains.com/docs/a");
        assertThat(first.snippet()).isEqualTo("First part.\n\nSecond part.");
        assertThat(results.searchInformation().totalResults()).isEqualTo(2L);
    }

    @Test
    void respectsMaxResults_evenIfTheServerReturnsMore() {
        respond(200, """
                {"search_id":"s","session_id":"x","results":[
                  {"url":"https://a.example/1","title":"1","excerpts":["a"]},
                  {"url":"https://a.example/2","title":"2","excerpts":["b"]},
                  {"url":"https://a.example/3","title":"3","excerpts":["c"]}
                ]}""");

        assertThat(engine("k").search(WebSearchRequest.builder().searchTerms("q").maxResults(2).build()).results())
                .hasSize(2);
    }

    @Test
    void emptyResults_areEmpty() {
        respond(200, "{\"search_id\":\"s\",\"session_id\":\"x\",\"results\":[]}");

        assertThat(engine("k").search(WebSearchRequest.from("q")).results()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void rejectedKey_saysSo_withoutEchoingKeyOrBody(int status) {
        respond(status, "{\"error\":{\"message\":\"bad key par-secret\"}}");

        assertThatThrownBy(() -> engine("par-secret").search(WebSearchRequest.from("q")))
                .isInstanceOfSatisfying(WebSearchProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(WebSearchProviderException.Kind.UNAUTHORIZED);
                    assertThat(e.getMessage()).contains("Parallel rejected the API key").contains("HTTP " + status)
                            .contains(WebSearchReadiness.SETTINGS_PATH).doesNotContain("par-secret");
                });
    }

    @Test
    void noCredit_402_pointsToTheBalance() {
        respond(402, "{\"error\":{\"message\":\"Insufficient credit\"}}");

        assertThatThrownBy(() -> engine("k").search(WebSearchRequest.from("q")))
                .isInstanceOfSatisfying(WebSearchProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(WebSearchProviderException.Kind.FAILED);
                    assertThat(e.getMessage()).contains("out of credit").contains("platform.parallel.ai");
                });
    }

    @Test
    void rateLimit_429_saysToRetryInAMinute() {
        respond(429, "{}");

        assertThatThrownBy(() -> engine("k").search(WebSearchRequest.from("q")))
                .isInstanceOfSatisfying(WebSearchProviderException.class, e -> {
                    assertThat(e.kind()).isEqualTo(WebSearchProviderException.Kind.RATE_LIMITED);
                    assertThat(e.getMessage()).contains("rate limit").contains("minute");
                });
    }

    @Test
    void validationError_422_isReported() {
        respond(422, "{\"type\":\"error\",\"error\":{\"ref_id\":\"r\",\"message\":\"bad request\"}}");

        assertThatThrownBy(() -> engine("k").search(WebSearchRequest.from("q")))
                .isInstanceOf(WebSearchProviderException.class)
                .hasMessageContaining("HTTP 422");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void serverErrors_sayTryAgainLater(int status) {
        respond(status, "upstream failure");

        assertThatThrownBy(() -> engine("k").search(WebSearchRequest.from("q")))
                .isInstanceOf(WebSearchProviderException.class)
                .hasMessageContaining("HTTP " + status).hasMessageContaining("Try again later")
                .hasMessageNotContaining("upstream failure");
    }

    @Test
    void redirect_isNotFollowed_soTheKeyNeverLeaves() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "https://elsewhere.example/v1/search"));

        assertThatThrownBy(() -> engine("par-secret").search(WebSearchRequest.from("q")))
                .isInstanceOf(WebSearchProviderException.class)
                .hasMessageContaining("HTTP 302");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void unreadableBody_isReportedClearly() {
        respond(200, "<html>oops</html>");

        assertThatThrownBy(() -> engine("k").search(WebSearchRequest.from("q")))
                .isInstanceOf(WebSearchProviderException.class)
                .hasMessageContaining("could not read");
    }

    @Test
    void unreachableHost_isReportedWithoutDetails() {
        ParallelWebSearchEngine unreachable = new ParallelWebSearchEngine("k", "http://127.0.0.1:1",
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());

        assertThatThrownBy(() -> unreachable.search(WebSearchRequest.from("q")))
                .isInstanceOf(WebSearchProviderException.class)
                .hasMessageStartingWith("Could not reach Parallel");
    }
}
