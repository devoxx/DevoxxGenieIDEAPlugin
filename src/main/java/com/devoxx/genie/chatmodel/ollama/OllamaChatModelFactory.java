package com.devoxx.genie.chatmodel.ollama;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.ollama.OllamaApiService;
import com.devoxx.genie.service.ollama.OllamaService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OllamaChatModelFactory implements ChatModelFactory {

    private final ModelProvider MODEL_PROVIDER = ModelProvider.Ollama;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static boolean warningShown = false;
    private List<LanguageModel> cachedModels = null;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OllamaChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getOllamaModelUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxRetries(chatModel.getMaxRetries())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OllamaStreamingChatModel.builder()
            .baseUrl(DevoxxGenieStateService.getInstance().getOllamaModelUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    /**
     * Get the model names from the Ollama service.
     * We're currently adding a fixed number of tokens to the model size.
     * TODO - Get the model size from the Ollama service or have the user define them in Options panel?
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
            OllamaModelEntryDTO[] ollamaModels = OllamaService.getInstance().getModels();
            for (OllamaModelEntryDTO model : ollamaModels) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        int contextWindow = OllamaApiService.getModelContext(model.getName());
                        LanguageModel languageModel = LanguageModel.builder()
                            .provider(MODEL_PROVIDER)
                            .modelName(model.getName())
                            .displayName(model.getName())
                            .inputCost(0)
                            .outputCost(0)
                            .contextWindow(contextWindow)
                            .apiKeyUsed(false)
                            .build();
                        synchronized (modelNames) {
                            modelNames.add(languageModel);
                        }
                    } catch (IOException e) {
                        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                            "Error fetching context window for model: " + model.getName());
                    }
                }, executorService);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            cachedModels = modelNames;
        } catch (IOException e) {
            if (!warningShown) {
                NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                    "Ollama is not running, please start it.");
                warningShown = true;
            }
            cachedModels = List.of();
        }
        return cachedModels;
    }

    @Override
    public void resetModels() {
        cachedModels = null;
    }

}
