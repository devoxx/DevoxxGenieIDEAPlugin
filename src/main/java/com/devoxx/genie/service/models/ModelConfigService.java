package com.devoxx.genie.service.models;

import com.devoxx.genie.model.models.ModelConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Maintains the catalog cache and performs network work away from the EDT. */
@Slf4j
@Service
public final class ModelConfigService {
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private final ModelCatalogLoader loader = new ModelCatalogLoader();
    private final AtomicBoolean backgroundFetch = new AtomicBoolean();
    private ModelConfig cachedConfig;
    private String cachedUrl;

    @NotNull
    public static ModelConfigService getInstance() {
        return ApplicationManager.getApplication().getService(ModelConfigService.class);
    }

    private String catalogUrl() {
        String url = DevoxxGenieStateService.getInstance().getModelCatalogUrl();
        return url == null ? "" : url.trim();
    }

    @Nullable
    public synchronized ModelConfig getModelConfig() {
        String url = catalogUrl();
        if (!url.equals(cachedUrl)) {
            cachedUrl = url;
            cachedConfig = url.isEmpty() ? null : loader.loadCached(url);
        }
        if (!url.isEmpty() && (cachedConfig == null || !loader.isCacheFresh(url, CACHE_TTL_MS))
                && backgroundFetch.compareAndSet(false, true)) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    fetchAndCache();
                } finally {
                    backgroundFetch.set(false);
                }
            });
        }
        return cachedConfig;
    }

    /** The callback runs on the EDT after success or bundled fallback. */
    public void forceRefresh(@Nullable Runnable callback) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                fetchAndCache();
            } finally {
                if (callback != null) ApplicationManager.getApplication().invokeLater(callback);
            }
        });
    }

    private void fetchAndCache() {
        String url = catalogUrl();
        ModelConfig config;
        try {
            config = url.isEmpty() ? loader.loadBundled() : loader.fetch(url);
        } catch (Exception e) {
            log.warn("Cannot fetch model catalog; using bundled models: {}", e.getMessage());
            config = loader.loadBundled();
        }
        synchronized (this) {
            // A settings change during a request must not publish the old URL's catalog.
            if (!url.equals(catalogUrl())) return;
            cachedConfig = config;
            cachedUrl = url;
        }
        LLMModelRegistryService.getInstance().refreshFromRemoteConfig(config);
    }
}
