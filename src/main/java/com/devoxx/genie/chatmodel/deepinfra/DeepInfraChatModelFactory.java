package com.devoxx.genie.chatmodel.deepinfra;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.OllamaService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DeepInfraChatModelFactory implements ChatModelFactory {

    private String apiKey;
    private String modelName;

    public DeepInfraChatModelFactory() {}

    public DeepInfraChatModelFactory(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public ChatLanguageModel createChatModel(ChatModel chatModel) {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.deepinfra.com/v1/openai")
            .apiKey(apiKey)
            .modelName(modelName)
            .maxRetries(chatModel.getMaxRetries())
            .temperature(chatModel.getTemperature())
            .maxTokens(chatModel.getMaxTokens())
            .timeout(Duration.ofSeconds(chatModel.getTimeout()))
            .topP(chatModel.getTopP())
            .build();
    }

    @Override
    public List<String> getModelNames() {
       return List.of(
           "meta-llama/Meta-Llama-3-70B-Instruct",
           "meta-llama/Meta-Llama-3-8B-Instruct",
           "mistralai/Mixtral-8x7B-Instruct-v0.1",
           "mistralai/Mixtral-8x22B-Instruct-v0.1",
           "microsoft/WizardLM-2-8x22B",
           "microsoft/WizardLM-2-7B",
           "databricks/dbrx-instruct",
           "openchat/openchat_3.5",
           "google/gemma-7b-it",
           "Phind/Phind-CodeLlama-34B-v2",
           "bigcode/starcoder2-15b"
       );
    }
}
