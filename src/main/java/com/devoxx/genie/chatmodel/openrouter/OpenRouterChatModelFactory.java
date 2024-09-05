package com.devoxx.genie.chatmodel.openrouter;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.devoxx.genie.service.openrouter.OpenRouterService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OpenRouterChatModelFactory implements ChatModelFactory {

    private List<LanguageModel> cachedModels = null;

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(4_000)
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(@NotNull ChatModel chatModel) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .apiKey(getApiKey())
            .modelName(chatModel.getModelName())
            .maxTokens(4_000)
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public String getApiKey() {
        return DevoxxGenieSettingsServiceProvider.getInstance().getDeepSeekKey().trim();
    }

    /**
     * Get the model names from the Jan service.
     *
     * @return List of model names
     */
    @Override
    public List<LanguageModel> getModels() {
        List<LanguageModel> modelNames = new ArrayList<>();
        try {
            List<Data> models = OpenRouterService.getInstance().getModels();
            for (Data model : models) {
                int ctxLen = model.getSettings().getCtxLen();
                modelNames.add(
                    LanguageModel.builder()
                        .provider(ModelProvider.Jan)
                        .modelName(model.getName())
                        .displayName(model.getName())
                        .contextWindow(ctxLen)
                        .apiKeyUsed(false)
                        .inputCost(0)
                        .outputCost(0)
                        .build()
                );
            }
        } catch (IOException e) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "Jan is not running, please start it.");
            return List.of();
        }
        return modelNames;
    }
}
