package com.devoxx.genie.service.prompt.websearch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-repo {@link WebSearchEngine} for Keenable. Works without an API key against
 * {@code POST /v1/search/public}; a key switches to {@code POST /v1/search}.
 * {@code X-API-Key} is not forwarded to a different host on redirect.
 */
public class KeenableWebSearchEngine implements WebSearchEngine {

    static final String DEFAULT_BASE_URL = "https://api.keenable.ai";
    static final String TITLE_HEADER = "DevoxxGenie";
    private static final int MAX_RESULTS = 50;
    private static final int MAX_REDIRECTS = 5;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public KeenableWebSearchEngine(@Nullable String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    KeenableWebSearchEngine(@Nullable String apiKey, @NotNull String baseUrl, @NotNull HttpClient httpClient) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = httpClient;
    }

    @Override
    public WebSearchResults search(WebSearchRequest webSearchRequest) {
        int limit = webSearchRequest.maxResults() == null ? 10 : webSearchRequest.maxResults();
        limit = Math.max(1, Math.min(MAX_RESULTS, limit));
        boolean keyed = !apiKey.isEmpty();
        URI start = URI.create(baseUrl + (keyed ? "/v1/search" : "/v1/search/public"));
        String body = "{\"query\":" + jsonString(webSearchRequest.searchTerms())
                + ",\"max_results\":" + limit + "}";
        try {
            HttpResponse<String> response = send(start, body, keyed);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Keenable search failed with HTTP " + response.statusCode());
            }
            return parse(response.body(), limit);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Keenable search request failed", e);
        }
    }

    private HttpResponse<String> send(URI uri, String body, boolean sendKey)
            throws IOException, InterruptedException {
        URI current = uri;
        boolean includeKey = sendKey;
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-Keenable-Title", TITLE_HEADER)
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (includeKey) {
                builder.header("X-API-Key", apiKey);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Optional<URI> redirected = redirectUri(current, response);
            if (redirected.isEmpty()) {
                return response;
            }
            URI next = redirected.get();
            if (includeKey && !sameHost(current, next)) {
                includeKey = false;
            }
            current = next;
        }
        throw new IllegalStateException("Keenable search exceeded redirect limit");
    }

    private static Optional<URI> redirectUri(URI current, HttpResponse<String> response) {
        int status = response.statusCode();
        if (status < 300 || status >= 400) {
            return Optional.empty();
        }
        String location = response.headers().firstValue("Location").orElse("");
        if (location.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(current.resolve(location));
    }

    private static boolean sameHost(URI left, URI right) {
        String a = left.getHost() == null ? "" : left.getHost();
        String b = right.getHost() == null ? "" : right.getHost();
        return a.equalsIgnoreCase(b);
    }

    static WebSearchResults parse(String json, int limit) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray results = root.has("results") && root.get("results").isJsonArray()
                ? root.getAsJsonArray("results") : new JsonArray();
        List<WebSearchOrganicResult> hits = new ArrayList<>();
        for (JsonElement element : results) {
            if (hits.size() >= limit || !element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String url = text(item, "url");
            if (url.isBlank()) {
                continue;
            }
            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            String title = text(item, "title");
            if (title.isBlank()) {
                title = "(no title)";
            }
            String snippet = text(item, "snippet");
            hits.add(new WebSearchOrganicResult(title, uri, snippet, null));
        }
        return WebSearchResults.from(WebSearchInformationResult.from((long) hits.size()), hits);
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        JsonElement value = object.get(key);
        return value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String jsonString(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
