package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class FetchPageToolExecutor implements ToolExecutor {

    private static final int MAX_OUTPUT_CHARS = 100_000;

    private static final int MAX_DOWNLOAD_BYTES = 10 * 1024 * 1024;

    private static final int MAX_REDIRECTS = 10;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);

    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(60);

    private static final String ACCEPT = "text/markdown, text/html;q=0.9, */*;q=0.8";

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; DevoxxGenie/1.0)";

    private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);

    private final HttpClient httpClient;

    private final PageUrlPolicy urlPolicy;

    private final FetchedPageCache cache;

    public FetchPageToolExecutor() {
        this(HttpClient.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                PageUrlPolicy.publicWeb(),
                FetchedPageCache.shared());
    }

    FetchPageToolExecutor(@NotNull HttpClient httpClient, @NotNull PageUrlPolicy urlPolicy,
                          @NotNull FetchedPageCache cache) {
        this.httpClient = httpClient;
        this.urlPolicy = urlPolicy;
        this.cache = cache;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        String url = ToolArgumentParser.getString(request.arguments(), "url");
        if (url == null || url.isBlank()) {
            return "Error: 'url' parameter is required.";
        }
        try {
            var uri = this.urlPolicy.fetchableUri(url.strip());
            var cached = this.cache.get(uri);
            return cached.isPresent() ? cached.get() : fetchFollowingSameHostRedirects(uri);
        } catch (IllegalArgumentException rejected) {
            return "Error: " + rejected.getMessage();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return "Error: Request interrupted";
        } catch (TimeoutException timedOut) {
            return "Error: Timed out after " + FETCH_TIMEOUT.toSeconds() + "s fetching " + url;
        } catch (Exception e) {
            log.error("Error fetching page", e);
            var cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
            return "Error: Failed to fetch page - " + cause.getMessage();
        }
    }

    private @NotNull String fetchFollowingSameHostRedirects(@NotNull URI requested)
            throws InterruptedException, ExecutionException, TimeoutException {
        var current = requested;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            var response = send(current);
            var status = response.statusCode();
            if (REDIRECT_STATUSES.contains(status)) {
                var location = response.headers().firstValue("Location");
                if (location.isEmpty()) {
                    return "Error: HTTP " + status + " without a Location header from " + current;
                }
                var target = this.urlPolicy.fetchableUri(current.resolve(location.get().strip()).toString());
                if (!PageUrlPolicy.isSameHost(current, target)) {
                    return crossHostRedirectNotice(current, target, status);
                }
                current = target;
                continue;
            }
            if (status >= 400) {
                return "Error: HTTP " + status + " fetching " + current;
            }
            var content = readableContent(response, current);
            if (!content.startsWith("Error:")) {
                this.cache.put(requested, content);
            }
            return content;
        }
        return "Error: More than " + MAX_REDIRECTS + " redirects fetching " + requested;
    }

    private @NotNull HttpResponse<byte[]> send(@NotNull URI uri)
            throws InterruptedException, ExecutionException, TimeoutException {
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(FETCH_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", ACCEPT)
                .GET()
                .build();
        var response = this.httpClient.sendAsync(request, info -> new CappedBodySubscriber(MAX_DOWNLOAD_BYTES));
        try {
            return response.get(FETCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException stopped) {
            response.cancel(true);
            throw stopped;
        }
    }

    private static @NotNull String readableContent(@NotNull HttpResponse<byte[]> response, @NotNull URI uri) {
        var contentType = response.headers().firstValue("Content-Type").orElse("text/html");
        var mediaType = mediaTypeOf(contentType);
        var body = response.body();
        if (isBinary(mediaType)) {
            return "[Binary content (" + mediaType + ", " + kilobytes(body.length)
                    + ") not shown: fetch_page only returns text.]";
        }
        var text = new String(body, charsetOf(contentType));
        var readable = isHtml(mediaType) ? HtmlToMarkdown.convert(text, uri.toString()) : text;
        if (readable.isBlank()) {
            return "Error: Empty response from " + uri;
        }
        return truncated(readable);
    }

    private static @NotNull String crossHostRedirectNotice(@NotNull URI from, @NotNull URI to, int status) {
        return "Redirected (HTTP " + status + ") from " + from + " to " + to
                + ", which is on a different host, so it was not fetched. "
                + "Call fetch_page with " + to + " to read it.";
    }

    private static @NotNull String mediaTypeOf(@NotNull String contentType) {
        var separator = contentType.indexOf(';');
        var mediaType = separator < 0 ? contentType : contentType.substring(0, separator);
        return mediaType.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean isHtml(@NotNull String mediaType) {
        return mediaType.isEmpty() || mediaType.equals("text/html") || mediaType.equals("application/xhtml+xml");
    }

    private static boolean isBinary(@NotNull String mediaType) {
        return !(isHtml(mediaType)
                || mediaType.startsWith("text/")
                || mediaType.equals("application/json") || mediaType.endsWith("+json")
                || mediaType.equals("application/xml") || mediaType.endsWith("+xml")
                || mediaType.startsWith("application/javascript"));
    }

    private static @NotNull Charset charsetOf(@NotNull String contentType) {
        for (String parameter : contentType.split(";")) {
            var nameAndValue = parameter.strip().split("=", 2);
            if (nameAndValue.length == 2 && nameAndValue[0].strip().equalsIgnoreCase("charset")) {
                try {
                    return Charset.forName(nameAndValue[1].strip().replace("\"", ""));
                } catch (IllegalArgumentException unsupported) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static @NotNull String kilobytes(int bytes) {
        return Math.max(1, Math.round(bytes / 1024.0)) + " KB";
    }

    private static @NotNull String truncated(@NotNull String text) {
        if (text.length() <= MAX_OUTPUT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_OUTPUT_CHARS)
                + "\n\n[Content truncated at " + MAX_OUTPUT_CHARS + " characters]";
    }
}
