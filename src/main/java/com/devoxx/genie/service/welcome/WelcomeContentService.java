package com.devoxx.genie.service.welcome;

import com.devoxx.genie.model.welcome.WelcomeContent;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
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
public class WelcomeContentService {

    private static final String WELCOME_JSON_URL = "https://genie.devoxx.com/api/welcome.json";
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final OkHttpClient client = HttpClientProvider.getClient();
    private final Gson gson = new GsonBuilder().create();

    private volatile WelcomeContent cachedContent;

    @NotNull
    public static WelcomeContentService getInstance() {
        return ApplicationManager.getApplication().getService(WelcomeContentService.class);
    }

    /**
     * Returns cached welcome content immediately and triggers a background refresh if stale.
     * On first call with no cache, returns null (local fallback should be used).
     *
     * @return cached WelcomeContent, or null if no cache is available yet
     */
    @Nullable
    public WelcomeContent getWelcomeContent() {
        if (cachedContent != null) {
            triggerBackgroundRefreshIfStale();
            return cachedContent;
        }

        // Try to restore from persistent cache
        WelcomeContent restored = restoreFromPersistentCache();
        if (restored != null) {
            cachedContent = restored;
            triggerBackgroundRefreshIfStale();
            return cachedContent;
        }

        // No cache at all — trigger background fetch for next time, return null for local fallback
        triggerBackgroundFetch();
        return null;
    }

    private void triggerBackgroundRefreshIfStale() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        long lastFetch = state.getWelcomeContentLastFetchTimestamp();
        String cachedVersion = state.getWelcomeContentPluginVersion();
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

            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(WELCOME_JSON_URL)).newBuilder();
            urlBuilder.addQueryParameter("v", pluginVersion);
            urlBuilder.addQueryParameter("os", os);

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Failed to fetch welcome content: HTTP {}", response.code());
                    return;
                }

                String json = response.body().string();
                WelcomeContent content = gson.fromJson(json, WelcomeContent.class);

                if (content == null) {
                    log.warn("Failed to parse welcome content JSON");
                    return;
                }

                // Schema version check — ignore incompatible content
                if (content.getSchemaVersion() > SUPPORTED_SCHEMA_VERSION) {
                    log.info("Remote welcome content has unsupported schema version {}, using local fallback",
                            content.getSchemaVersion());
                    return;
                }

                // Update in-memory cache
                cachedContent = content;

                // Persist to state service
                DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
                state.setWelcomeContentCachedJson(json);
                state.setWelcomeContentLastFetchTimestamp(System.currentTimeMillis());
                state.setWelcomeContentPluginVersion(pluginVersion);

                log.info("Successfully fetched and cached welcome content (schema v{})", content.getSchemaVersion());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch welcome content: {}", e.getMessage());
        }
    }

    @Nullable
    private WelcomeContent restoreFromPersistentCache() {
        try {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            String cachedJson = state.getWelcomeContentCachedJson();

            if (cachedJson == null || cachedJson.isEmpty()) {
                return null;
            }

            WelcomeContent content = gson.fromJson(cachedJson, WelcomeContent.class);
            if (content != null && content.getSchemaVersion() <= SUPPORTED_SCHEMA_VERSION) {
                return content;
            }
        } catch (Exception e) {
            log.warn("Failed to restore welcome content from persistent cache: {}", e.getMessage());
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
