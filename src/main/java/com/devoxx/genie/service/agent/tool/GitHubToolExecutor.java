package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** GitHub issue and pull request tools, sharing authentication and HTTP handling. */
public class GitHubToolExecutor implements ToolExecutor {
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private final HttpClient httpClient;

    public GitHubToolExecutor() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    GitHubToolExecutor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String repo = ToolArgumentParser.getString(request.arguments(), "repo");
            if (repo == null || !repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                    || repo.startsWith("./") || repo.startsWith("../")
                    || repo.endsWith("/.") || repo.endsWith("/..")) {
                return "Error: 'repo' must be in owner/name format.";
            }
            String path = "/repos/" + repo;
            if ("github_list_issues".equals(request.name())) {
                String label = ToolArgumentParser.getString(request.arguments(), "label");
                String query = "?state=open";
                if (label != null && !label.isBlank()) {
                    query += "&labels=" + URLEncoder.encode(label, StandardCharsets.UTF_8);
                }
                JsonArray issues = list(path + "/issues" + query);
                JsonArray result = new JsonArray();
                for (JsonElement item : issues) {
                    if (!item.getAsJsonObject().has("pull_request")) {
                        result.add(summary(item.getAsJsonObject()));
                    }
                }
                return result.toString();
            }
            String number = ToolArgumentParser.getString(request.arguments(), "number");
            if (number == null || !number.matches("[1-9][0-9]*")) {
                return "Error: 'number' must be a positive integer.";
            }
            return switch (request.name()) {
                case "github_issue" -> summary(call(path + "/issues/" + number, null).getAsJsonObject()).toString();
                case "github_pull_request" -> {
                    JsonObject result = summary(call(path + "/pulls/" + number, null).getAsJsonObject());
                    JsonArray files = new JsonArray();
                    for (JsonElement item : list(path + "/pulls/" + number + "/files")) {
                        JsonObject file = item.getAsJsonObject();
                        JsonObject details = new JsonObject();
                        for (String key : new String[]{"filename", "previous_filename", "status", "additions", "deletions", "changes"}) {
                            if (file.has(key)) details.add(key, file.get(key));
                        }
                        files.add(details);
                    }
                    result.add("changed_files", files);
                    yield result.toString();
                }
                case "github_comment" -> {
                    String body = ToolArgumentParser.getString(request.arguments(), "body");
                    if (body == null || body.isBlank()) yield "Error: 'body' parameter is required.";
                    JsonObject payload = new JsonObject();
                    payload.addProperty("body", body);
                    JsonObject comment = call(path + "/issues/" + number + "/comments", payload).getAsJsonObject();
                    yield "Comment posted: " + comment.get("html_url").getAsString();
                }
                default -> "Error: Unknown GitHub tool.";
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Request interrupted";
        } catch (Exception e) {
            // Never include request headers, credentials, or response bodies in errors.
            return "Error: GitHub request failed (" + e.getClass().getSimpleName() + "). "
                    + (e instanceof IOException ? e.getMessage() : "Check the arguments and GitHub response.");
        }
    }

    private JsonArray list(String path) throws IOException, InterruptedException {
        JsonArray result = new JsonArray();
        for (int page = 1; ; page++) {
            JsonArray items = call(path + (path.contains("?") ? "&" : "?")
                    + "per_page=100&page=" + page, null).getAsJsonArray();
            result.addAll(items);
            if (items.size() < 100) return result;
        }
    }

    private JsonElement call(String path, JsonObject payload) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://api.github.com" + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DevoxxGenie");
        String token = CredentialService.getInstance().getCredential(CredentialKey.GITHUB_TOKEN);
        if (token.isBlank()) token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) builder.header("Authorization", "Bearer " + token.trim());
        if (payload == null) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " from GitHub.");
        }
        if (response.body() == null || response.body().isBlank()) throw new IOException("Empty GitHub response.");
        return JsonParser.parseString(response.body());
    }

    private JsonObject summary(JsonObject source) {
        JsonObject result = new JsonObject();
        for (String key : new String[]{"number", "title", "state", "html_url"}) {
            result.add(key, source.get(key));
        }
        result.add("description", source.get("body"));
        JsonArray labels = new JsonArray();
        if (source.has("labels") && source.get("labels").isJsonArray()) {
            for (JsonElement label : source.getAsJsonArray("labels")) {
                labels.add(label.isJsonObject() ? label.getAsJsonObject().get("name") : label);
            }
        }
        result.add("labels", labels);
        return result;
    }
}
