package com.devoxx.genie.chatmodel.local;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ThinkingSupport;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class LocalChatModelFactory implements ChatModelFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LocalChatModelFactory.class);

    protected final ModelProvider modelProvider;
    public List<LanguageModel> cachedModels = null;

    protected static boolean warningShown = false;
    public boolean providerRunning = false;
    public boolean providerChecked = false;

    // LMStudio does not support HTTP_2, see https://github.com/langchain4j/langchain4j/issues/2758
    private final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1) ;
    private final JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
            .httpClientBuilder(httpClientBuilder);

    protected LocalChatModelFactory(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    @Override
    public abstract ChatModel createChatModel(@NotNull CustomChatModel customChatModel);

    @Override
    public abstract StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel);

    protected abstract String getModelUrl();

    /**
     * The langchain4j HTTP client builder used for the OpenAI-compatible chat models.
     * Exposed as a hook so providers with server-specific quirks can decorate it
     * (e.g. Jan compacts JSON request bodies, see issue #1051).
     */
    protected dev.langchain4j.http.client.HttpClientBuilder resolveHttpClientBuilder() {
        return jdkHttpClientBuilder;
    }

    protected ChatModel createOpenAiChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
                .baseUrl(getModelUrl())
                .httpClientBuilder(resolveHttpClientBuilder())
                .apiKey("na")
                .modelName(customChatModel.getModelName())
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(customChatModel.getTemperature())
                .maxTokens(customChatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(customChatModel.getTopP())
                .returnThinking(ThinkingSupport.isEnabled())
                .listeners(getListener())
                .build();
    }

    protected StreamingChatModel createOpenAiStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(getModelUrl())
                .httpClientBuilder(resolveHttpClientBuilder())
                .apiKey("na")
                .modelName(customChatModel.getModelName())
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .returnThinking(ThinkingSupport.isEnabled())
                .listeners(getListener())
                .build();
    }

    /**
     * How long to wait before re-probing a provider that was previously found to be down.
     * Without this, a failed probe (provider not started yet) would stay cached for the
     * whole IDE session and the model list would remain empty even after the provider
     * is started — until the user manually hits Refresh.
     */
    private static final long NOT_RUNNING_RECHECK_INTERVAL_MS = 15_000L;

    private long lastNotRunningCheck = 0L;

    @Override
    public List<LanguageModel> getModels() {
        if (!providerChecked) {
            checkAndFetchModels();
        } else if (!providerRunning
                && System.currentTimeMillis() - lastNotRunningCheck >= NOT_RUNNING_RECHECK_INTERVAL_MS) {
            // The provider was down on the last probe; it may have been started since,
            // so probe again instead of staying stuck on the stale "not running" state.
            resetModels();
            checkAndFetchModels();
        }
        if (!providerRunning) {
            handleProviderNotRunning();
            return List.of();
        }
        return cachedModels;
    }

    protected void handleProviderNotRunning() {
        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "LLM provider is not running. Please start it and try again.");
    }

    private void checkAndFetchModels() {
        List<LanguageModel> modelNames = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try {
            Object[] models = fetchModels();
            if (models != null) {
                for (Object model : models) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            LanguageModel languageModel = buildLanguageModel(model);
                            synchronized (modelNames) {
                                modelNames.add(languageModel);
                            }
                        } catch (IOException e) {
                            handleModelFetchError(e);
                        } catch (RuntimeException e) {
                            // A per-model detail lookup must never abort the whole fetch:
                            // an uncaught exception here would escape allOf().join() and
                            // hide the entire model list (e.g. Ollama's /api/show empty
                            // body used to NPE in the context-length parser).
                            handleModelBuildError(model, e);
                        }
                    }, AppExecutorUtil.getAppExecutorService());
                    futures.add(future);
                }
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            cachedModels = modelNames;
            // Set only after cachedModels is assigned: getModels() may run on another
            // thread and must never observe providerRunning == true while cachedModels
            // is still null. Per-model build failures are handled inside the futures
            // above, so reaching this line means the listing call succeeded.
            providerRunning = true;
        } catch (IOException e) {
            handleGeneralFetchError(e);
            cachedModels = List.of();
            providerRunning = false;
            lastNotRunningCheck = System.currentTimeMillis();
        } finally {
            providerChecked = true;
        }
    }

    protected abstract Object[] fetchModels() throws IOException;

    protected abstract LanguageModel buildLanguageModel(Object model) throws IOException;

    protected void handleModelFetchError(@NotNull IOException e) {
        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(), "Error fetching model details: " + e.getMessage());
    }

    /**
     * Handles an unexpected (non-IO) failure while building a single model. The model is
     * skipped and the rest of the list is kept; the error is logged so it can be diagnosed
     * without spamming the user with a notification per model.
     */
    protected void handleModelBuildError(@NotNull Object model, @NotNull RuntimeException e) {
        LOG.warn("Skipping model '{}' of {}: failed to build model details", model, modelProvider, e);
    }

    protected void handleGeneralFetchError(IOException e) {
        if (!warningShown) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(), "Error fetching models: " + e.getMessage());
            warningShown = true;
        }
    }

    @Override
    public void resetModels() {
        cachedModels = null;
        providerChecked = false;
        providerRunning = false;
    }
}
