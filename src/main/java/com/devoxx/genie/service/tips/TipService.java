package com.devoxx.genie.service.tips;

import com.devoxx.genie.model.tips.Tip;
import com.devoxx.genie.model.tips.TipConfig;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public final class TipService {

    private static final List<Tip> FALLBACK_TIPS = List.of(
            new Tip("Type @ to add a file to the context.", 2),
            new Tip("Generate a DEVOXXGENIE.md with /init for project-aware answers.", 3),
            new Tip("Try /test, /explain or /review on selected code.", 2),
            new Tip("Discover and install MCP servers from the MCP Marketplace in Settings.", 1),
            new Tip("Enable Agent Mode to let DevoxxGenie read, search and edit files for you.", 1),
            new Tip("Turn on RAG in Settings to add semantic project context automatically.", 1),
            new Tip("Drag & drop images into the prompt when using a multimodal model.", 1),
            new Tip("Press Ctrl+Space to autocomplete a slash command.", 1),
            new Tip("Add the full project to context with large-context models like Gemini.", 1),
            new Tip("DevoxxGenie is open source — star it on GitHub and join us at Devoxx!", 1));

    private static final String TIPS_JSON_URL = "https://genie.devoxx.com/api/tips.json";
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final OkHttpClient client = HttpClientProvider.getClient();
    private final Gson gson = new GsonBuilder().create();

    private volatile TipConfig cachedConfig;

    @NotNull
    public static TipService getInstance() {
        return ApplicationManager.getApplication().getService(TipService.class);
    }

    /**
     * Returns the next tip text to display, weighted-random and excluding the
     * immediately-previous tip when alternatives exist.
     *
     * @param previousTipText the tip currently shown (may be null)
     * @return the next tip text, or null when no tips are available
     */
    @Nullable
    public String nextTip(@Nullable String previousTipText) {
        Tip selected = selectWeighted(getTips(), previousTipText, ThreadLocalRandom.current().nextDouble());
        return selected == null ? null : selected.getText();
    }

    /**
     * Returns the current tip list. Uses a cache-backed lookup with 24h TTL and
     * falls back to the hardcoded list when no cache is available or fetch fails.
     */
    @NotNull
    public List<Tip> getTips() {
        TipConfig config = cachedConfig;

        if (config == null) {
            config = restoreFromPersistentCache();
            if (config != null) {
                cachedConfig = config;
            }
        }

        if (config == null) {
            // Cold start: kick off a background fetch and use the fallback this time.
            triggerBackgroundFetch();
            return getFallbackTips();
        }

        triggerBackgroundRefreshIfStale();

        List<Tip> tips = config.getTips();
        return (tips == null || tips.isEmpty()) ? getFallbackTips() : tips;
    }

    private void triggerBackgroundRefreshIfStale() {
        long lastFetch = DevoxxGenieStateService.getInstance().getTipsLastFetchTimestamp();
        if ((System.currentTimeMillis() - lastFetch) > CACHE_TTL_MS) {
            triggerBackgroundFetch();
        }
    }

    private void triggerBackgroundFetch() {
        ApplicationManager.getApplication().executeOnPooledThread(this::fetchAndCache);
    }

    private void fetchAndCache() {
        try {
            Request request = new Request.Builder().url(TIPS_JSON_URL).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Failed to fetch tips: HTTP {}", response.code());
                    return;
                }

                String json = response.body().string();
                TipConfig config = gson.fromJson(json, TipConfig.class);

                if (config == null) {
                    log.warn("Failed to parse tips JSON");
                    return;
                }
                if (config.getSchemaVersion() > SUPPORTED_SCHEMA_VERSION) {
                    log.info("Remote tips have unsupported schema version {}, using fallback",
                            config.getSchemaVersion());
                    return;
                }

                cachedConfig = config;

                DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
                state.setTipsCachedJson(json);
                state.setTipsLastFetchTimestamp(System.currentTimeMillis());

                log.info("Successfully fetched and cached {} tips (schema v{}, updated {})",
                        config.getTips() == null ? 0 : config.getTips().size(),
                        config.getSchemaVersion(), config.getLastUpdated());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch tips: {}", e.getMessage());
        }
    }

    @Nullable
    private TipConfig restoreFromPersistentCache() {
        try {
            String cachedJson = DevoxxGenieStateService.getInstance().getTipsCachedJson();
            if (cachedJson == null || cachedJson.isEmpty()) {
                return null;
            }
            TipConfig config = gson.fromJson(cachedJson, TipConfig.class);
            if (config != null && config.getSchemaVersion() <= SUPPORTED_SCHEMA_VERSION) {
                return config;
            }
        } catch (Exception e) {
            log.warn("Failed to restore tips from persistent cache: {}", e.getMessage());
        }
        return null;
    }

    static int effectiveWeight(@Nullable Tip tip) {
        if (tip == null || tip.getWeight() <= 0) {
            return 1;
        }
        return tip.getWeight();
    }

    /**
     * Weighted-random selection over {@code tips}, excluding any tip whose text equals
     * {@code previousTipText}. Falls back to the full set if exclusion empties the
     * candidate list (e.g. a single-tip list). Blank/null tips are ignored.
     *
     * @param r a value in [0, 1) used to pick within the cumulative weight range
     */
    @Nullable
    static Tip selectWeighted(@Nullable List<Tip> tips, @Nullable String previousTipText, double r) {
        if (tips == null || tips.isEmpty()) {
            return null;
        }

        List<Tip> valid = new ArrayList<>();
        for (Tip tip : tips) {
            if (tip != null && tip.getText() != null && !tip.getText().isBlank()) {
                valid.add(tip);
            }
        }
        if (valid.isEmpty()) {
            return null;
        }

        List<Tip> candidates = new ArrayList<>();
        for (Tip tip : valid) {
            if (previousTipText == null || !previousTipText.equals(tip.getText())) {
                candidates.add(tip);
            }
        }
        if (candidates.isEmpty()) {
            candidates = valid;
        }

        int total = 0;
        for (Tip tip : candidates) {
            total += effectiveWeight(tip);
        }

        double target = r * total;
        double accumulated = 0;
        for (Tip tip : candidates) {
            accumulated += effectiveWeight(tip);
            if (target < accumulated) {
                return tip;
            }
        }
        // Floating-point safety net: r is in [0,1), so this is only reached on rounding at the upper bound.
        return candidates.get(candidates.size() - 1);
    }

    @NotNull
    static List<Tip> getFallbackTips() {
        return FALLBACK_TIPS;
    }
}
