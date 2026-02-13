package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public class FetchPageToolExecutor implements ToolExecutor {

    private static final int MAX_OUTPUT_CHARS = 100_000;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public FetchPageToolExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Constructor for testing with a custom HttpClient. */
    FetchPageToolExecutor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String url = ToolArgumentParser.getString(request.arguments(), "url");
            if (url == null || url.isBlank()) {
                return "Error: 'url' parameter is required.";
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return "Error: URL must start with http:// or https://";
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (compatible; DevoxxGenie/1.0)")
                    .header("Accept", "text/html,application/xhtml+xml,*/*")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                return "Error: HTTP " + response.statusCode() + " fetching " + url;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return "Error: Empty response from " + url;
            }

            Document doc = Jsoup.parse(body, url);
            doc.select("script, style, noscript").remove();
            String text = doc.text();

            if (text.length() > MAX_OUTPUT_CHARS) {
                text = text.substring(0, MAX_OUTPUT_CHARS) + "\n\n[Content truncated at " + MAX_OUTPUT_CHARS + " characters]";
            }

            return text;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Request interrupted";
        } catch (Exception e) {
            log.error("Error fetching page", e);
            return "Error: Failed to fetch page - " + e.getMessage();
        }
    }
}
