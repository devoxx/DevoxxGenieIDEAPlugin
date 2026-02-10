package com.devoxx.genie.chatmodel.local;

import com.devoxx.genie.chatmodel.ChatModelFactory;
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

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class LocalChatModelFactory implements ChatModelFactory {

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

    protected ChatModel createOpenAiChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
                .baseUrl(getModelUrl())
                .httpClientBuilder(jdkHttpClientBuilder)
                .apiKey("na")
                .modelName(customChatModel.getModelName())
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(customChatModel.getTemperature())
                .maxTokens(customChatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(customChatModel.getTopP())
                .listeners(getListener(customChatModel.getProject()))
                .build();
    }

    protected StreamingChatModel createOpenAiStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(getModelUrl())
                .httpClientBuilder(jdkHttpClientBuilder)
                .apiKey("na")
                .modelName(customChatModel.getModelName())
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .listeners(getListener(customChatModel.getProject()))
                .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        if (!providerChecked) {
            checkAndFetchModels();
        }
        if (!providerRunning) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                    "LLM provider is not running. Please start it and try again.");
            return List.of();
        }
        return cachedModels;
    }

    private void checkAndFetchModels() {
        List<LanguageModel> modelNames = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try {
            Object[] models = fetchModels();
            for (Object model : models) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        LanguageModel languageModel = buildLanguageModel(model);
                        synchronized (modelNames) {
                            modelNames.add(languageModel);
                        }
                    } catch (IOException e) {
                        handleModelFetchError(e);
                    }
                }, AppExecutorUtil.getAppExecutorService());
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            cachedModels = modelNames;
            providerRunning = true;
        } catch (IOException e) {
            handleGeneralFetchError(e);
            cachedModels = List.of();
            providerRunning = false;
        } finally {
            providerChecked = true;
        }
    }

    protected abstract Object[] fetchModels() throws IOException;

    protected abstract LanguageModel buildLanguageModel(Object model) throws IOException;

    protected void handleModelFetchError(@NotNull IOException e) {
        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(), "Error fetching model details: " + e.getMessage());
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
