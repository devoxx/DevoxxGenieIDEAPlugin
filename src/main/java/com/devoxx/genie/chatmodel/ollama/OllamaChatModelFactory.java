package com.devoxx.genie.chatmodel.ollama;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.OllamaService;
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

public class OllamaChatModelFactory implements ChatModelFactory {

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
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {
        if (cachedModels != null) {
            return cachedModels;
        }

        List<LanguageModel> modelNames = new ArrayList<>();
        try {
            OllamaModelEntryDTO[] ollamaModels = OllamaService.getInstance().getModels();
            for (OllamaModelEntryDTO model : ollamaModels) {
                modelNames.add(
                    LanguageModel.builder()
                        .provider(ModelProvider.Ollama)
                        .modelName(model.getName())
                        .displayName(model.getName())
                        .inputCost(0)
                        .outputCost(0)
                        .contextWindow(8_000)
                        .apiKeyUsed(false)
                        .build()
                );
            }
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

    public void resetCache() {
        cachedModels = null;
        warningShown = false;
    }
}
