package com.devoxx.genie.service.websearch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * In-repo {@link WebSearchEngine} for Parallel's REST Search API (https://docs.parallel.ai):
 * {@code POST /v1/search} with an {@code x-api-key} header (TASK-277.5, #1423).
 *
 * <p>Uses the {@code fast} search mode: interactive IDE search, and a fifth of the default {@code advanced}
 * mode's price. Redirects are never followed, so the key only ever goes to the configured origin. Failures
 * are reported as {@link WebSearchProviderException} with messages that are safe to show to the user.
 */
public class ParallelWebSearchEngine implements WebSearchEngine {

    static final String DEFAULT_BASE_URL = "https://api.parallel.ai";
    static final String MODE = "fast";
    /** Caps the excerpt text Parallel returns across all results, keeping prompts small. */
    static final int MAX_CHARS_TOTAL = 6000;
    private static final int MAX_RESULTS = 20;
    private static final String SETTINGS = WebSearchReadiness.SETTINGS_PATH;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    ParallelWebSearchEngine(@NotNull String apiKey, @NotNull String baseUrl, @NotNull HttpClient httpClient) {
        this.apiKey = apiKey.trim();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
    }

    @Override
    public WebSearchResults search(WebSearchRequest webSearchRequest) {
        int limit = webSearchRequest.maxResults() == null ? 10 : webSearchRequest.maxResults();
        limit = Math.max(1, Math.min(MAX_RESULTS, limit));

        JsonObject body = new JsonObject();
        JsonArray queries = new JsonArray();
        queries.add(webSearchRequest.searchTerms());
        body.add("search_queries", queries);
        body.addProperty("objective", webSearchRequest.searchTerms());
        body.addProperty("mode", MODE);
        body.addProperty("max_chars_total", MAX_CHARS_TOTAL);
        JsonObject advanced = new JsonObject();
        advanced.addProperty("max_results", limit);
        body.add("advanced_settings", advanced);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/search"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("User-Agent", WebSearchHttp.USER_AGENT)
                .header("x-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    "Could not reach Parallel (" + e.getClass().getSimpleName()
                            + "). Check your network and the IDE's proxy settings.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    "Parallel search was interrupted.", e);
        }
        checkStatus(response.statusCode());
        return parse(response.body(), limit);
    }

    /** Error codes as documented at docs.parallel.ai/resources/warnings-and-errors. */
    private static void checkStatus(int status) {
        if (status >= 200 && status < 300) {
            return;
        }
        switch (status) {
            case 401, 403 -> throw new WebSearchProviderException(WebSearchProviderException.Kind.UNAUTHORIZED,
                    "Parallel rejected the API key (HTTP " + status + "). Check it in " + SETTINGS + ".", null);
            case 402 -> throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    "Your Parallel account is out of credit (HTTP 402). Top it up at platform.parallel.ai.", null);
            case 429 -> throw new WebSearchProviderException(WebSearchProviderException.Kind.RATE_LIMITED,
                    "Parallel's rate limit was reached. Try again in a minute.", null);
            default -> throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    status >= 500
                            ? "Parallel search failed (HTTP " + status + "). Try again later."
                            : "Parallel search failed (HTTP " + status + ").", null);
        }
    }

    static WebSearchResults parse(String json, int limit) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    "Parallel returned a response DevoxxGenie could not read.", e);
        }
        JsonArray results = root.has("results") && root.get("results").isJsonArray()
                ? root.getAsJsonArray("results") : new JsonArray();
        List<WebSearchOrganicResult> hits = new ArrayList<>();
        for (JsonElement element : results) {
            if (hits.size() >= limit || !element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            URI uri = uri(text(item, "url"));
            if (uri == null) {
                continue;
            }
            String title = text(item, "title");
            if (title.isBlank()) {
                title = uri.getHost();
            }
            String excerpts = excerpts(item);
            hits.add(new WebSearchOrganicResult(title, uri, excerpts, excerpts.isBlank() ? null : excerpts));
        }
        return WebSearchResults.from(WebSearchInformationResult.from((long) hits.size()), hits);
    }

    /** An absolute http(s) URL with a host, or null. */
    private static URI uri(String url) {
        if (url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? null : uri;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String excerpts(JsonObject item) {
        if (!item.has("excerpts") || !item.get("excerpts").isJsonArray()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (JsonElement excerpt : item.getAsJsonArray("excerpts")) {
            if (excerpt.isJsonPrimitive() && !excerpt.getAsString().isBlank()) {
                parts.add(excerpt.getAsString().strip());
            }
        }
        return String.join("\n\n", parts);
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        return object.get(key).getAsString();
    }
}
