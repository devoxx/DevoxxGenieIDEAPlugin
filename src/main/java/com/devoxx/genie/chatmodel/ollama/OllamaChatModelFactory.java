package com.devoxx.genie.chatmodel.ollama;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.OllamaService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OllamaChatModelFactory implements ChatModelFactory {

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return OllamaChatModel.builder()
            .baseUrl(chatModel.getBaseUrl())
            .modelName(chatModel.getModelName())
            .temperature(chatModel.getTemperature())
            .topP(chatModel.getTopP())
            .maxRetries(chatModel.getMaxRetries())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .build();
    }

    @Override
    public List<String> getModelNames() {
        List<String> modelNames = new ArrayList<>();
        try {
            OllamaModelEntryDTO[] ollamaModels = new OllamaService().getModels();
            for (OllamaModelEntryDTO model : ollamaModels) {
                modelNames.add(model.getName());
            }
        } catch (IOException e) {
            NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                "Ollama is not running, please start it.");
        }
        return modelNames;
    }
}
