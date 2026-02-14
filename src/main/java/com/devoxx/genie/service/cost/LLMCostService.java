package com.devoxx.genie.service.cost;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.llmcost.LLMCostData;
import com.devoxx.genie.model.llmcost.LLMCostModelEntry;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public final class LLMCostService {

    private static final String LLM_COST_JSON_URL = "https://genie.devoxx.com/api/llm-api-cost.json";
    private static final String BUNDLED_RESOURCE_PATH = "/llm-api-cost.json";
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final OkHttpClient client = HttpClientProvider.getClient();
    private final Gson gson = new GsonBuilder().create();

    private volatile Map<String, LanguageModel> cachedModels;

    @NotNull
    public static LLMCostService getInstance() {
        return ApplicationManager.getApplication().getService(LLMCostService.class);
    }

    /**
     * Returns the model cost map. Loads from:
     * 1. In-memory cache
     * 2. Persistent cache (DevoxxGenieStateService)
     * 3. Bundled resource fallback (llm-api-cost.json in resources)
     *
     * Also triggers a background refresh from the remote URL if stale.
     *
     * @return map of provider:modelName -> LanguageModel (never null)
     */
    @NotNull
    public Map<String, LanguageModel> getModelCosts() {
        if (cachedModels != null) {
            triggerBackgroundRefreshIfStale();
            return cachedModels;
        }

        // Try to restore from persistent cache
        Map<String, LanguageModel> restored = restoreFromPersistentCache();
        if (restored != null) {
            cachedModels = restored;
            triggerBackgroundRefreshIfStale();
            return cachedModels;
        }

        // Fall back to bundled resource
        Map<String, LanguageModel> bundled = loadFromBundledResource();
        if (bundled != null) {
            cachedModels = bundled;
            triggerBackgroundFetch();
            return cachedModels;
        }

        // Should not happen — bundled resource is always available
        log.error("Failed to load LLM cost data from any source");
        cachedModels = new HashMap<>();
        return cachedModels;
    }

    private void triggerBackgroundRefreshIfStale() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        long lastFetch = state.getLlmCostLastFetchTimestamp();
        String cachedVersion = state.getLlmCostPluginVersion();
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
            Request request = new Request.Builder()
                    .url(LLM_COST_JSON_URL)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Failed to fetch LLM cost data: HTTP {}", response.code());
                    return;
                }

                String json = response.body().string();
                LLMCostData costData = gson.fromJson(json, LLMCostData.class);

                if (costData == null || costData.getModels() == null) {
                    log.warn("Failed to parse LLM cost data JSON");
                    return;
                }

                if (costData.getSchemaVersion() > SUPPORTED_SCHEMA_VERSION) {
                    log.info("Remote LLM cost data has unsupported schema version {}, using local fallback",
                            costData.getSchemaVersion());
                    return;
                }

                Map<String, LanguageModel> models = convertToLanguageModels(costData);
                cachedModels = models;

                // Persist to state service
                DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
                state.setLlmCostCachedJson(json);
                state.setLlmCostLastFetchTimestamp(System.currentTimeMillis());
                state.setLlmCostPluginVersion(getPluginVersion());

                log.info("Successfully fetched and cached LLM cost data ({} models, schema v{})",
                        models.size(), costData.getSchemaVersion());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LLM cost data: {}", e.getMessage());
        }
    }

    @Nullable
    private Map<String, LanguageModel> restoreFromPersistentCache() {
        try {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            String cachedJson = state.getLlmCostCachedJson();

            if (cachedJson == null || cachedJson.isEmpty()) {
                return null;
            }

            LLMCostData costData = gson.fromJson(cachedJson, LLMCostData.class);
            if (costData != null && costData.getModels() != null
                    && costData.getSchemaVersion() <= SUPPORTED_SCHEMA_VERSION) {
                return convertToLanguageModels(costData);
            }
        } catch (Exception e) {
            log.warn("Failed to restore LLM cost data from persistent cache: {}", e.getMessage());
        }
        return null;
    }

    @Nullable
    private Map<String, LanguageModel> loadFromBundledResource() {
        try (InputStream is = getClass().getResourceAsStream(BUNDLED_RESOURCE_PATH)) {
            if (is == null) {
                log.error("Bundled LLM cost resource not found: {}", BUNDLED_RESOURCE_PATH);
                return null;
            }
            LLMCostData costData = gson.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8), LLMCostData.class);
            if (costData != null && costData.getModels() != null) {
                log.info("Loaded LLM cost data from bundled resource ({} models)", costData.getModels().size());
                return convertToLanguageModels(costData);
            }
        } catch (Exception e) {
            log.warn("Failed to load bundled LLM cost resource: {}", e.getMessage());
        }
        return null;
    }

    @NotNull
    private Map<String, LanguageModel> convertToLanguageModels(@NotNull LLMCostData costData) {
        Map<String, LanguageModel> result = new HashMap<>();
        for (LLMCostModelEntry entry : costData.getModels()) {
            ModelProvider provider = ModelProvider.fromName(entry.getProvider());
            if (provider == null) {
                log.warn("Unknown provider '{}' in LLM cost data, skipping model '{}'",
                        entry.getProvider(), entry.getModelName());
                continue;
            }
            String key = provider.getName() + ":" + entry.getModelName();
            result.put(key, LanguageModel.builder()
                    .provider(provider)
                    .modelName(entry.getModelName())
                    .displayName(entry.getDisplayName())
                    .inputCost(entry.getInputCost())
                    .outputCost(entry.getOutputCost())
                    .inputMaxTokens(entry.getInputMaxTokens())
                    .outputMaxTokens(entry.getOutputMaxTokens())
                    .apiKeyUsed(entry.isApiKeyUsed())
                    .build());
        }
        return result;
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
