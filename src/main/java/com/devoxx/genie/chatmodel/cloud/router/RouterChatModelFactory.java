package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ThinkingSupport;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Chat model factory for any OpenAI-compatible router described by a {@link RouterDefinition}.
 *
 * <p>One instance is shared across panels and projects, so the catalog is an immutable snapshot
 * published under {@link #lock}. The lock is never held during a fetch, a key read or a
 * notification, which keeps {@link #getCachedModels()} safe to call on the EDT.
 *
 * <p>A fetch that started before {@link #resetModels()} is stale: it publishes nothing and records
 * no failure, so a slow request made with an old key can't restore that account's catalog.
 */
public class RouterChatModelFactory implements ChatModelFactory {

    private static final Logger LOG = Logger.getInstance(RouterChatModelFactory.class);
    private static final long FAILURE_BACKOFF_MILLIS = 60_000;

    private final RouterDefinition def;
    private final BooleanSupplier enabled;
    private final Supplier<String> keySupplier;
    private final Supplier<String> baseUrl;
    private final RouterCatalogClient client;
    private final LongSupplier clockMillis;

    private final Object lock = new Object();
    private final AtomicLong generation = new AtomicLong();
    private List<LanguageModel> catalog;
    private Long failedAtMillis;

    public RouterChatModelFactory(RouterDefinition def) {
        this.def = def;
        this.enabled = () -> DevoxxGenieStateService.getInstance().isRouterEnabled(def.provider());
        this.keySupplier = () -> getApiKey(def.provider());
        this.baseUrl = () -> DevoxxGenieStateService.getInstance().getRouterUrl(def);
        this.client = new RouterCatalogClient.OkHttp();
        this.clockMillis = () -> System.nanoTime() / 1_000_000; // monotonic: immune to wall-clock changes
    }

    RouterChatModelFactory(RouterDefinition def,
                           BooleanSupplier enabled,
                           Supplier<String> keySupplier,
                           RouterCatalogClient client,
                           LongSupplier clockMillis) {
        this(def, enabled, keySupplier, def::baseUrl, client, clockMillis);
    }

    RouterChatModelFactory(RouterDefinition def,
                           BooleanSupplier enabled,
                           Supplier<String> keySupplier,
                           Supplier<String> baseUrl,
                           RouterCatalogClient client,
                           LongSupplier clockMillis) {
        this.def = def;
        this.enabled = enabled;
        this.keySupplier = keySupplier;
        this.baseUrl = baseUrl;
        this.client = client;
        this.clockMillis = clockMillis;
    }

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl.get())
            .apiKey(readKey())
            .modelName(customChatModel.getModelName())
            .maxRetries(customChatModel.getMaxRetries())
            .temperature(customChatModel.getTemperature())
            .maxTokens(maxTokens(customChatModel))
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .topP(customChatModel.getTopP())
            .returnThinking(ThinkingSupport.isEnabled())
            .listeners(getListener())
            .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl.get())
            .apiKey(readKey())
            .modelName(customChatModel.getModelName())
            .maxTokens(maxTokens(customChatModel))
            .temperature(customChatModel.getTemperature())
            .topP(customChatModel.getTopP())
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .returnThinking(ThinkingSupport.isEnabled())
            .listeners(getListener())
            .build();
    }

    /**
     * May read the key and fetch the catalog, so never call this on the EDT.
     *
     * @return an immutable list; empty when disabled, without a key, or when the catalog can't be loaded
     */
    @Override
    public List<LanguageModel> getModels() {
        if (!enabled.getAsBoolean()) {
            return List.of();
        }

        long gen;
        synchronized (lock) {
            if (catalog != null) {
                return catalog;
            }
            if (failedAtMillis != null) {
                if (clockMillis.getAsLong() - failedAtMillis < FAILURE_BACKOFF_MILLIS) {
                    return List.of();
                }
                failedAtMillis = null;
            }
            gen = generation.get();
        }

        // Read after capturing the generation: a reset in between leaves this fetch stale, whichever key it got.
        String key = readKey();
        if (key.isEmpty()) {
            return List.of();
        }

        Map<String, LanguageModel> merged = new LinkedHashMap<>();
        boolean endpointFailed = false;
        for (String endpoint : def.modelEndpoints()) {
            if (gen != generation.get()) {
                break;
            }
            String url = joinUrl(baseUrl.get(), endpoint);
            try {
                for (LanguageModel model : RouterCatalogParser.parse(def, client.fetch(url, key))) {
                    merged.putIfAbsent(model.getModelName(), model);
                }
            } catch (Exception e) {
                // Besides IOException: a non-JSON body and a key that is no valid header value are runtime exceptions.
                endpointFailed = true;
                LOG.warn("Could not fetch " + def.label() + " models from " + url + ": " + safeMessage(e, key));
            }
        }

        return publish(gen, merged, endpointFailed);
    }

    private List<LanguageModel> publish(long gen, Map<String, LanguageModel> merged, boolean endpointFailed) {
        List<LanguageModel> result;
        boolean notify = false;
        synchronized (lock) {
            // A concurrent fetch of this generation may already have published: the first catalog
            // stays, so readers keep a stable list and a late failure can't hide a good result.
            if (gen == generation.get() && catalog == null) {
                if (!merged.isEmpty()) {
                    catalog = List.copyOf(merged.values());
                    failedAtMillis = null;
                } else if (failedAtMillis == null) {
                    failedAtMillis = clockMillis.getAsLong();
                    notify = endpointFailed;
                }
            }
            result = snapshot();
        }
        if (notify) {
            notifyFetchFailure();
        }
        return result;
    }

    /** Never fetches or reads credentials, so this is safe on the EDT. */
    public List<LanguageModel> getCachedModels() {
        synchronized (lock) {
            return snapshot();
        }
    }

    @Override
    public void resetModels() {
        synchronized (lock) {
            generation.incrementAndGet();
            catalog = null;
            failedAtMillis = null;
        }
    }

    protected void notifyFetchFailure() {
        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
            "Unable to reach " + def.label() + ", please try again later.");
    }

    /** Caller holds {@link #lock}. */
    private List<LanguageModel> snapshot() {
        return catalog != null ? catalog : List.of();
    }

    private String readKey() {
        String key = keySupplier.get();
        return key == null ? "" : key.strip();
    }

    private int maxTokens(CustomChatModel customChatModel) {
        return def.maxTokens() != null ? def.maxTokens() : customChatModel.getMaxTokens();
    }

    private static String joinUrl(String baseUrl, String endpoint) {
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + endpoint;
    }

    /** An exception description for the log, with the key removed in case something echoed it. */
    static String safeMessage(Exception e, String key) {
        String message = e.getMessage();
        return e.getClass().getSimpleName() + (message == null ? "" : ": " + message.replace(key, "***"));
    }
}
