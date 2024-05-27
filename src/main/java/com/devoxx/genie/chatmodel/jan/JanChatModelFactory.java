package com.devoxx.genie.chatmodel.jan;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.service.JanService;
import com.devoxx.genie.service.SettingsStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class JanChatModelFactory implements ChatModelFactory {

    // Moved client instance here for the sake of better performance
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public ChatLanguageModel createChatModel(@NotNull ChatModel chatModel) {
        return LocalAiChatModel.builder()
            .baseUrl(SettingsStateService.getInstance().getJanModelUrl())
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
            .baseUrl(SettingsStateService.getInstance().getJanModelUrl())
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
    public List<String> getModelNames() {
        List<String> modelNames = new ArrayList<>();
        try {
            List<Data> models = new JanService(client).getModels();
            for (Data model : models) {
                modelNames.add(model.getId());
            }
        } catch (IOException e) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "Jan is not running, please start it.");
            return List.of();
        }
        return modelNames;
    }
}
