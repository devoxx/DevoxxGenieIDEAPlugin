package com.devoxx.genie.service.analytics;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.extensions.PluginId;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Anonymous LLM provider/model usage analytics.
 *
 * <p>Sends a GA4 Measurement Protocol payload through the shared GenieBuilder Cloudflare worker.
 * DevoxxGenie traffic is segmented from GenieBuilder's Electron traffic via {@code app_name=devoxxgenie-intellij}.
 *
 * <p>The payload is intentionally minimal — see task-206 for the full disclosure list. No prompt
 * text, response text, conversation history, file content, file paths, project names, API keys,
 * or user identity is ever sent.
 *
 * <p>Calls are fire-and-forget on the application thread pool and never block the EDT. Failures
 * are logged at debug level and never surfaced to the user.
 */
@Slf4j
@Service(Service.Level.APP)
public final class AnalyticsService {

    public static final String APP_NAME = "devoxxgenie-intellij";
    public static final String EVENT_PROMPT_EXECUTED = "prompt_executed";
    public static final String EVENT_MODEL_SELECTED = "model_selected";

    private static final String PLUGIN_ID = "com.devoxx.genie";

    private final String sessionId;
    private HttpClient httpClient;
    private boolean synchronousForTest = false;

    public AnalyticsService() {
        this.sessionId = generateSessionId();
    }

    public static AnalyticsService getInstance() {
        return ApplicationManager.getApplication().getService(AnalyticsService.class);
    }

    public void trackPromptExecuted(@Nullable String providerId, @Nullable String modelName) {
        send(EVENT_PROMPT_EXECUTED, providerId, modelName);
    }

    public void trackModelSelected(@Nullable String providerId, @Nullable String modelName) {
        send(EVENT_MODEL_SELECTED, providerId, modelName);
    }

    private void send(@NotNull String eventName, @Nullable String providerId, @Nullable String modelName) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        // Hard precondition gates — never emit before consent or when disabled.
        if (!Boolean.TRUE.equals(state.getAnalyticsNoticeAcknowledged())) {
            return;
        }
        if (!Boolean.TRUE.equals(state.getAnalyticsEnabled())) {
            return;
        }

        // Provider/model are required for both events to be useful.
        if (providerId == null || providerId.isEmpty() || modelName == null || modelName.isEmpty()) {
            return;
        }

        String endpoint = state.getAnalyticsEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            return;
        }

        String clientId = state.getAnalyticsClientId();
        String payload = buildPayload(clientId, eventName, providerId, modelName);

        if (synchronousForTest) {
            postSilently(endpoint, payload);
        } else {
            ApplicationManager.getApplication().executeOnPooledThread(() -> postSilently(endpoint, payload));
        }
    }

    private void postSilently(@NotNull String endpoint, @NotNull String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<Void> response = client().send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                log.debug("Analytics endpoint returned {}", response.statusCode());
            }
        } catch (Exception e) {
            log.debug("Analytics post failed: {}", e.getMessage());
        }
    }

    String buildPayload(@NotNull String clientId,
                        @NotNull String eventName,
                        @NotNull String providerId,
                        @NotNull String modelName) {
        StringBuilder sb = new StringBuilder(384);
        sb.append('{')
          .append("\"client_id\":\"").append(escape(clientId)).append("\",")
          .append("\"events\":[{")
          .append("\"name\":\"").append(escape(eventName)).append("\",")
          .append("\"params\":{")
          .append("\"provider_id\":\"").append(escape(providerId)).append("\",")
          .append("\"model_name\":\"").append(escape(modelName)).append("\",")
          .append("\"app_name\":\"").append(APP_NAME).append("\",")
          .append("\"app_version\":\"").append(escape(pluginVersion())).append("\",")
          .append("\"ide_version\":\"").append(escape(ideVersion())).append("\",")
          .append("\"session_id\":\"").append(sessionId).append("\",")
          .append("\"engagement_time_msec\":1")
          .append("}}]}")
          ;
        return sb.toString();
    }

    @NotNull
    private static String escape(@NotNull String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    @NotNull
    private static String pluginVersion() {
        try {
            var descriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
            if (descriptor != null && descriptor.getVersion() != null) {
                return descriptor.getVersion();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "unknown";
    }

    @NotNull
    private static String ideVersion() {
        try {
            return ApplicationInfo.getInstance().getFullVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @NotNull
    private static String generateSessionId() {
        // 10-digit string, matching GenieBuilder analytics-service.ts format.
        long n = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        return Long.toString(n);
    }

    @NotNull
    String getSessionId() {
        return sessionId;
    }

    @NotNull
    private synchronized HttpClient client() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
        return httpClient;
    }

    @TestOnly
    synchronized void setHttpClientForTest(@Nullable HttpClient client) {
        this.httpClient = client;
        this.synchronousForTest = true;
    }
}
