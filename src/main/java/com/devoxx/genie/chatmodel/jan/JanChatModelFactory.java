package com.devoxx.genie.chatmodel.jan;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.devoxx.genie.service.jan.JanService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JanChatModelFactory implements ChatModelFactory {
    private List<LanguageModel> cachedModels = null;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(DevoxxGenieSettingsServiceProvider.getInstance().getJanModelUrl())
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }


    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return LocalAiStreamingChatModel.builder()
            .baseUrl(DevoxxGenieSettingsServiceProvider.getInstance().getJanModelUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    /**
     * Get the model names from the Jan service.
     *
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {
        if (cachedModels != null) {
            return cachedModels;
        }

        List<LanguageModel> modelNames = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            List<Data> models = JanService.getInstance().getModels();
            for (Data model : models) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    LanguageModel languageModel = LanguageModel.builder()
                        .provider(ModelProvider.Jan)
                        .modelName(model.getId())
                        .displayName(model.getName())
                        .inputCost(0)
                        .outputCost(0)
                        .contextWindow(model.getSettings().getCtxLen() == null ? 8_000 : model.getSettings().getCtxLen())
                        .apiKeyUsed(false)
                        .build();
                    synchronized (modelNames) {
                        modelNames.add(languageModel);
                    }
                }, executorService);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            cachedModels = modelNames;
        } catch (IOException e) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "Unable to reach OpenRouter, please try again later.");
            cachedModels = List.of();
        }
        return cachedModels;
    }
}
