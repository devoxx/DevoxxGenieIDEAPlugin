package com.devoxx.genie.chatmodel.local;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
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
    public abstract ChatLanguageModel createChatModel(@NotNull ChatModel chatModel);

    @Override
    public abstract StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel);

    protected abstract String getModelUrl();

    protected ChatLanguageModel createOpenAiChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
                .baseUrl(getModelUrl())
                .httpClientBuilder(jdkHttpClientBuilder)
                .apiKey("na")
                .modelName(chatModel.getModelName())
                .maxRetries(chatModel.getMaxRetries())
                .temperature(chatModel.getTemperature())
                .maxTokens(chatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .topP(chatModel.getTopP())
                .listeners(getListener())
                .build();
    }

    protected StreamingChatLanguageModel createOpenAiStreamingChatModel(@NotNull ChatModel chatModel) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(getModelUrl())
                .httpClientBuilder(jdkHttpClientBuilder)
                .apiKey("na")
                .modelName(chatModel.getModelName())
                .temperature(chatModel.getTemperature())
                .topP(chatModel.getTopP())
                .timeout(Duration.ofSeconds(chatModel.getTimeout()))
                .listeners(getListener())
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
