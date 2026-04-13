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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
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
 * <p>Calls are fire-and-forget through {@link HttpClient#sendAsync(HttpRequest, HttpResponse.BodyHandler)}
 * and never block the EDT. Failures are logged at debug level and never surfaced to the user.
 */
@Slf4j
@Service(Service.Level.APP)
public final class AnalyticsService {

    public static final String APP_NAME = "devoxxgenie-intellij";
    public static final String EVENT_PROMPT_EXECUTED = "prompt_executed";
    public static final String EVENT_MODEL_SELECTED = "model_selected";
    public static final String EVENT_FEATURE_ENABLED = "feature_enabled";
    public static final String EVENT_FEATURE_USED = "feature_used";
    public static final String EVENT_FEATURE_COUNTS = "feature_counts";

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

    public static void trackPromptExecutedSafely(@Nullable String providerId, @Nullable String modelName) {
        try {
            getInstance().trackPromptExecuted(providerId, modelName);
        } catch (Exception e) {
            logAnalyticsFailure("Analytics tracking skipped", e);
        }
    }

    public static void trackModelSelectedSafely(@Nullable String providerId, @Nullable String modelName) {
        try {
            getInstance().trackModelSelected(providerId, modelName);
        } catch (Exception e) {
            logAnalyticsFailure("Analytics tracking skipped", e);
        }
    }

    public void trackPromptExecuted(@Nullable String providerId, @Nullable String modelName) {
        sendPromptOrModelEventSafely(EVENT_PROMPT_EXECUTED, providerId, modelName);
    }

    public void trackModelSelected(@Nullable String providerId, @Nullable String modelName) {
        sendPromptOrModelEventSafely(EVENT_MODEL_SELECTED, providerId, modelName);
    }

    public void trackFeatureEnabled(@NotNull FeatureId featureId) {
        if (featureId.isUsageOnly()) {
            log.debug("Rejected trackFeatureEnabled for usage-only feature '{}'", featureId.wireValue());
            return;
        }
        Map<String, String> params = AnalyticsEventBuilder.params();
        params.put("feature_id", featureId.wireValue());
        sendGenericSafely(EVENT_FEATURE_ENABLED, params);
    }

    public void trackFeatureUsed(@NotNull FeatureId featureId,
                                 @NotNull ProviderType providerType,
                                 @NotNull String toolCallCountBucket) {
        Map<String, String> params = AnalyticsEventBuilder.params();
        params.put("feature_id", featureId.wireValue());
        params.put("provider_type", providerType.wireValue());
        params.put("tool_call_count", toolCallCountBucket);
        sendGenericSafely(EVENT_FEATURE_USED, params);
    }

    public void trackFeatureCounts(@NotNull String mcpServerCountBucket,
                                   @NotNull String customPromptCountBucket,
                                   @NotNull String chatMemoryBucket) {
        Map<String, String> params = AnalyticsEventBuilder.params();
        params.put("mcp_server_count", mcpServerCountBucket);
        params.put("custom_prompt_count", customPromptCountBucket);
        params.put("chat_memory_bucket", chatMemoryBucket);
        sendGenericSafely(EVENT_FEATURE_COUNTS, params);
    }

    private void sendPromptOrModelEventSafely(@NotNull String eventName,
                                              @Nullable String providerId,
                                              @Nullable String modelName) {
        // Provider/model are required for both events to be useful.
        if (providerId == null || providerId.isEmpty() || modelName == null || modelName.isEmpty()) {
            return;
        }
        Map<String, String> params = AnalyticsEventBuilder.params();
        params.put("provider_id", providerId);
        params.put("model_name", modelName);
        sendGenericSafely(eventName, params);
    }

    private void sendGenericSafely(@NotNull String eventName, @NotNull Map<String, String> eventParams) {
        try {
            sendGeneric(eventName, eventParams);
        } catch (Exception e) {
            logAnalyticsFailure("Analytics tracking skipped", e);
        }
    }

    private void sendGeneric(@NotNull String eventName, @NotNull Map<String, String> eventParams) {
        DevoxxGenieStateService state;
        try {
            state = DevoxxGenieStateService.getInstance();
        } catch (Exception e) {
            logAnalyticsFailure("Analytics tracking skipped", e);
            return;
        }

        // Hard precondition gates — never emit before consent or when disabled.
        if (!Boolean.TRUE.equals(state.getAnalyticsNoticeAcknowledged())) {
            return;
        }
        if (!Boolean.TRUE.equals(state.getAnalyticsEnabled())) {
            return;
        }

        String endpoint = state.getAnalyticsEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            return;
        }

        String clientId = state.getAnalyticsClientId();
        Map<String, String> commonParams = buildCommonParams();

        String payload = AnalyticsEventBuilder.build(clientId, eventName, eventParams, commonParams);
        if (payload == null) {
            // Allowlist rejection — logged inside the builder.
            return;
        }

        if (synchronousForTest) {
            postBlockingSilently(endpoint, payload);
        } else {
            postAsyncSilently(endpoint, payload);
        }
    }

    @NotNull
    private Map<String, String> buildCommonParams() {
        Map<String, String> common = new LinkedHashMap<>();
        common.put("app_name", APP_NAME);
        common.put("app_version", pluginVersion());
        common.put("ide_version", ideVersion());
        common.put("session_id", sessionId);
        return common;
    }

    private void postAsyncSilently(@NotNull String endpoint, @NotNull String payload) {
        try {
            HttpRequest request = buildRequest(endpoint, payload);
            client().sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(this::logUnexpectedStatus)
                    .exceptionally(e -> {
                        logAnalyticsFailure("Analytics post failed", unwrapCompletionException(e));
                        return null;
                    });
        } catch (Exception e) {
            logAnalyticsFailure("Analytics post failed", e);
        }
    }

    private void postBlockingSilently(@NotNull String endpoint, @NotNull String payload) {
        try {
            HttpRequest request = buildRequest(endpoint, payload);
            HttpResponse<Void> response = client().send(request, HttpResponse.BodyHandlers.discarding());
            logUnexpectedStatus(response);
        } catch (Exception e) {
            logAnalyticsFailure("Analytics post failed", e);
        }
    }

    @NotNull
    private HttpRequest buildRequest(@NotNull String endpoint, @NotNull String payload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
    }

    private void logUnexpectedStatus(@NotNull HttpResponse<Void> response) {
        if (response.statusCode() / 100 != 2) {
            log.debug("Analytics endpoint returned {}", response.statusCode());
        }
    }

    private static void logAnalyticsFailure(@NotNull String prefix, @NotNull Throwable throwable) {
        String message = throwable.getMessage();
        log.debug("{}: {}", prefix, message != null ? message : throwable.getClass().getSimpleName());
    }

    @NotNull
    private static Throwable unwrapCompletionException(@NotNull Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
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
        setHttpClientForTest(client, true);
    }

    @TestOnly
    synchronized void setHttpClientForTest(@Nullable HttpClient client, boolean synchronousForTest) {
        this.httpClient = client;
        this.synchronousForTest = synchronousForTest;
    }
}
