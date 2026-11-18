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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * In-repo {@link WebSearchEngine} for Keenable (https://keenable.ai). Works without an API key against
 * {@code POST /v1/search/public}; a key switches to {@code POST /v1/search} with {@code X-API-Key}.
 *
 * <p>Originally contributed in #1430. The API key is never sent to a different origin (scheme, host or
 * port) after a redirect. Failures are reported as {@link WebSearchProviderException} with messages that
 * are safe to show to the user.
 */
public class KeenableWebSearchEngine implements WebSearchEngine {

    static final String DEFAULT_BASE_URL = "https://api.keenable.ai";
    static final String TITLE_HEADER = "DevoxxGenie";
    static final String USER_AGENT = "DevoxxGenie";
    private static final int MAX_RESULTS = 50;
    private static final int MAX_REDIRECTS = 5;
    private static final String SETTINGS = WebSearchReadiness.SETTINGS_PATH;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public KeenableWebSearchEngine(@Nullable String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, defaultHttpClient());
    }

    KeenableWebSearchEngine(@Nullable String apiKey, @NotNull String baseUrl, @NotNull HttpClient httpClient) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = httpClient;
    }

    /** Redirects are followed by hand (to control the key); the JVM proxy selector is the IDE's proxy setup. */
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

    boolean usesApiKey() {
        return !apiKey.isEmpty();
    }

    @Override
    public WebSearchResults search(WebSearchRequest webSearchRequest) {
        int limit = webSearchRequest.maxResults() == null ? 10 : webSearchRequest.maxResults();
        limit = Math.max(1, Math.min(MAX_RESULTS, limit));
        boolean keyed = usesApiKey();
        URI start = URI.create(baseUrl + (keyed ? "/v1/search" : "/v1/search/public"));

        JsonObject body = new JsonObject();
        body.addProperty("query", webSearchRequest.searchTerms());
        body.addProperty("max_results", limit);

        HttpResponse<String> response;
        try {
            response = send(start, body.toString(), keyed);
        } catch (IOException e) {
            throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    "Could not reach Keenable (" + e.getClass().getSimpleName()
                            + "). Check your network and the IDE's proxy settings.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    "Keenable search was interrupted.", e);
        }
        checkStatus(response.statusCode(), keyed);
        return parse(response.body(), limit);
    }

    private static void checkStatus(int status, boolean keyed) {
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 429) {
            throw new WebSearchProviderException(WebSearchProviderException.Kind.RATE_LIMITED, keyed
                    ? "Keenable's rate limit for your API key was reached. Try again in a moment."
                    : "Keenable's free search limit for your network was reached. Try again later, or add a "
                      + "Keenable API key in " + SETTINGS + " for a higher limit.", null);
        }
        if (status == 401 || status == 403) {
            throw new WebSearchProviderException(WebSearchProviderException.Kind.UNAUTHORIZED, keyed
                    ? "Keenable rejected the API key (HTTP " + status + "). Check it in " + SETTINGS
                      + ", or clear it to use free keyless search."
                    : "Keenable refused the keyless search (HTTP " + status + "). Add a Keenable API key in "
                      + SETTINGS + ".", null);
        }
        throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                "Keenable search failed (HTTP " + status + "). Try again later.", null);
    }

    private HttpResponse<String> send(URI uri, String body, boolean sendKey)
            throws IOException, InterruptedException {
        URI current = uri;
        boolean includeKey = sendKey;
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", USER_AGENT)
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
            if (includeKey && !sameOrigin(current, next)) {
                includeKey = false;
            }
            current = next;
        }
        throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                "Keenable search was redirected too many times.", null);
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

    /** Same scheme, host and (effective) port: the only case where the API key may follow a redirect. */
    static boolean sameOrigin(@NotNull URI left, @NotNull URI right) {
        return Objects.equals(lower(left.getScheme()), lower(right.getScheme()))
                && Objects.equals(lower(left.getHost()), lower(right.getHost()))
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443;
    }

    private static @Nullable String lower(@Nullable String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    static WebSearchResults parse(String json, int limit) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new WebSearchProviderException(WebSearchProviderException.Kind.FAILED,
                    "Keenable returned a response DevoxxGenie could not read.", e);
        }
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
                // WebSearchOrganicResult requires a title; the host reads better in the /search Sources list.
                title = uri.getHost() != null ? uri.getHost() : url;
            }
            hits.add(new WebSearchOrganicResult(title, uri, text(item, "snippet"), null));
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
}
