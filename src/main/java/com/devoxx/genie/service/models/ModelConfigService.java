package com.devoxx.genie.service.models;

import com.devoxx.genie.model.models.ModelConfig;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public final class ModelConfigService {

    private static final String MODELS_JSON_URL = "https://genie.devoxx.com/api/models.json";
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final OkHttpClient client = HttpClientProvider.getClient();
    private final Gson gson = new GsonBuilder().create();

    private volatile ModelConfig cachedConfig;

    @NotNull
    public static ModelConfigService getInstance() {
        return ApplicationManager.getApplication().getService(ModelConfigService.class);
    }

    /**
     * Returns cached model config immediately and triggers a background refresh if stale.
     * On first call with no cache, triggers a background fetch and returns null (hardcoded fallback should be used).
     *
     * @return cached ModelConfig, or null if no cache is available yet
     */
    @Nullable
    public ModelConfig getModelConfig() {
        if (cachedConfig != null) {
            triggerBackgroundRefreshIfStale();
            return cachedConfig;
        }

        // Try to restore from persistent cache
        ModelConfig restored = restoreFromPersistentCache();
        if (restored != null) {
            cachedConfig = restored;
            triggerBackgroundRefreshIfStale();
            return cachedConfig;
        }

        // No cache at all — trigger background fetch for next time, return null for hardcoded fallback
        triggerBackgroundFetch();
        return null;
    }

    private void triggerBackgroundRefreshIfStale() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        long lastFetch = state.getModelConfigLastFetchTimestamp();
        String cachedVersion = state.getModelConfigPluginVersion();
        String currentVersion = getPluginVersion();

        boolean isStale = (System.currentTimeMillis() - lastFetch) > CACHE_TTL_MS;
        boolean isVersionChanged = !currentVersion.equals(cachedVersion);

        if (isStale || isVersionChanged) {
            triggerBackgroundFetch();
        }
    }

    private void triggerBackgroundFetch() {
        ApplicationManager.getApplication().executeOnPooledThread(this::fetchAndCache);
    }

    private void fetchAndCache() {
        try {
            String pluginVersion = getPluginVersion();
            String os = System.getProperty("os.name", "unknown");

            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(MODELS_JSON_URL)).newBuilder();
            urlBuilder.addQueryParameter("v", pluginVersion);
            urlBuilder.addQueryParameter("os", os);

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Failed to fetch model config: HTTP {}", response.code());
                    return;
                }

                String json = response.body().string();
                ModelConfig config = gson.fromJson(json, ModelConfig.class);

                if (config == null) {
                    log.warn("Failed to parse model config JSON");
                    return;
                }

                // Schema version check — ignore incompatible content
                if (config.getSchemaVersion() > SUPPORTED_SCHEMA_VERSION) {
                    log.info("Remote model config has unsupported schema version {}, using hardcoded fallback",
                            config.getSchemaVersion());
                    return;
                }

                // Update in-memory cache
                cachedConfig = config;

                // Persist to state service
                DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
                state.setModelConfigCachedJson(json);
                state.setModelConfigLastFetchTimestamp(System.currentTimeMillis());
                state.setModelConfigPluginVersion(pluginVersion);

                // Notify registry to refresh models from remote config
                LLMModelRegistryService.getInstance().refreshFromRemoteConfig(config);

                log.info("Successfully fetched and cached model config (schema v{}, updated {})",
                        config.getSchemaVersion(), config.getLastUpdated());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch model config: {}", e.getMessage());
        }
    }

    @Nullable
    private ModelConfig restoreFromPersistentCache() {
        try {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            String cachedJson = state.getModelConfigCachedJson();

            if (cachedJson == null || cachedJson.isEmpty()) {
                return null;
            }

            ModelConfig config = gson.fromJson(cachedJson, ModelConfig.class);
            if (config != null && config.getSchemaVersion() <= SUPPORTED_SCHEMA_VERSION) {
                return config;
            }
        } catch (Exception e) {
            log.warn("Failed to restore model config from persistent cache: {}", e.getMessage());
        }
        return null;
    }

    @NotNull
    private String getPluginVersion() {
        try {
            return PropertiesService.getInstance().getVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
