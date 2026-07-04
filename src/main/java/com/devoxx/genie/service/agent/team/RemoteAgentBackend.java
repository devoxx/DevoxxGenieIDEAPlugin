package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for a DockerAgents {@code orchestrator-api} (TASK-248): the remote
 * execution backend behind {@code delegate_task}. The tool contract was deliberately
 * kept identical to the DockerAgents `/sessions` API contract, so a delegation maps
 * 1:1 onto: {@code POST /sessions} → {@code GET /sessions/{id}/wait} →
 * {@code result.json}'s {@code {status, exit_code, summary}} — and cancellation onto
 * {@code DELETE /sessions/{id}}.
 * <p>
 * Children then run in isolated Docker containers with the POC's own per-agent
 * provider bindings, repo cache and lifetime controls, while the IDE UX (progress
 * blocks, summary-only reports) stays identical.
 */
@Slf4j
public class RemoteAgentBackend {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient client;

    public RemoteAgentBackend(@NotNull String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    /** Names from the remote {@code GET /agents} directory. Throws on connectivity failure. */
    public @NotNull List<String> listAgentNames() throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/agents"))
                        .timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GET /agents returned HTTP " + response.statusCode());
        }
        List<String> names = new ArrayList<>();
        JsonElement parsed = JsonParser.parseString(response.body());
        JsonArray agents = parsed.isJsonArray()
                ? parsed.getAsJsonArray()
                : parsed.getAsJsonObject().getAsJsonArray("agents");
        if (agents != null) {
            for (JsonElement agent : agents) {
                if (agent.isJsonObject() && agent.getAsJsonObject().has("name")) {
                    names.add(agent.getAsJsonObject().get("name").getAsString());
                } else if (agent.isJsonPrimitive()) {
                    names.add(agent.getAsString());
                }
            }
        }
        return names;
    }

    /** Spawns a one-shot session; returns its session id. */
    public @NotNull String spawn(@NotNull String agent, @NotNull String task, @Nullable String intent)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("agent", agent);
        body.addProperty("task", task);
        if (intent != null && !intent.isBlank()) {
            body.addProperty("intent", intent);
        }
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/sessions"))
                        .timeout(Duration.ofSeconds(30))
                        .header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new IOException("Unknown remote agent '" + agent + "': " + response.body());
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("POST /sessions returned HTTP " + response.statusCode()
                    + ": " + response.body());
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonElement sessionId = json.get("session_id");
        if (sessionId == null) {
            throw new IOException("POST /sessions response has no session_id: " + response.body());
        }
        return sessionId.getAsString();
    }

    /**
     * Blocks on the remote session's completion and maps its result.json into an
     * {@link AgentResult}. HTTP 504 (the api's wait timeout) maps to TIMEOUT.
     */
    public @NotNull AgentResult waitFor(@NotNull String sessionId, @NotNull String agent,
                                        @Nullable String intent, int timeoutSeconds)
            throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        String url = baseUrl + "/sessions/" + URLEncoder.encode(sessionId, StandardCharsets.UTF_8)
                + "/wait?timeout=" + timeoutSeconds;
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(url))
                        // HTTP timeout must outlast the api-side wait
                        .timeout(Duration.ofSeconds(timeoutSeconds + 30L)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 504) {
            return AgentResult.timeout(agent, intent, timeoutSeconds);
        }
        if (response.statusCode() != 200) {
            throw new IOException("GET /wait returned HTTP " + response.statusCode()
                    + ": " + response.body());
        }
        return parseWaitResponse(response.body(), agent, intent, System.currentTimeMillis() - start);
    }

    /** Stops and removes the remote session (cancellation). Best-effort. */
    public void delete(@NotNull String sessionId) {
        try {
            client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/sessions/"
                                    + URLEncoder.encode(sessionId, StandardCharsets.UTF_8)))
                            .timeout(Duration.ofSeconds(15)).DELETE().build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            log.debug("Failed to delete remote session {}: {}", sessionId, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Maps the api's wait payload — {@code {session_id, status, exit_code?, result:
     * {status, exit_code, summary, ...}}} — into an AgentResult. Tolerant of missing
     * fields: a readable summary is always produced.
     */
    static @NotNull AgentResult parseWaitResponse(@NotNull String body, @NotNull String agent,
                                                  @Nullable String intent, long durationMs) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonObject result = json.has("result") && json.get("result").isJsonObject()
                    ? json.getAsJsonObject("result")
                    : json;
            String status = stringOf(result, "status");
            String summary = stringOf(result, "summary");
            int exitCode = result.has("exit_code") && !result.get("exit_code").isJsonNull()
                    ? result.get("exit_code").getAsInt() : 0;
            if (summary == null || summary.isBlank()) {
                summary = "(remote session " + stringOf(json, "session_id") + " returned no summary)";
            }
            boolean ok = "ok".equalsIgnoreCase(status) && exitCode == 0;
            return ok
                    ? AgentResult.ok(agent, intent, summary, 0, durationMs, "remote", null)
                    : AgentResult.error(agent, intent, summary, 0, durationMs, "remote", null);
        } catch (Exception e) {
            return AgentResult.error(agent, intent,
                    "Unreadable remote wait response: " + e.getMessage(), 0, durationMs, "remote", null);
        }
    }

    private static @Nullable String stringOf(@NotNull JsonObject json, @NotNull String key) {
        JsonElement value = json.get(key);
        return value != null && !value.isJsonNull() ? value.getAsString() : null;
    }
}
