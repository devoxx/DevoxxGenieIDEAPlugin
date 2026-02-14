package com.devoxx.genie.service.models;

import com.devoxx.genie.chatmodel.cloud.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.cost.LLMCostService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public final class LLMModelRegistryService {

    private Map<String, LanguageModel> models = new HashMap<>();

    @NotNull
    public static LLMModelRegistryService getInstance() {
        return ApplicationManager.getApplication().getService(LLMModelRegistryService.class);
    }

    public LLMModelRegistryService() {
        loadModelsFromCostService();
    }

    private void loadModelsFromCostService() {
        models = new HashMap<>(LLMCostService.getInstance().getModelCosts());
    }

    @NotNull
    public List<LanguageModel> getModels() {

        // Create a copy of the current models
        Map<String, LanguageModel> modelsCopy = new HashMap<>(models);

        getOpenRouterModels(modelsCopy);

        return new ArrayList<>(modelsCopy.values());
    }

    private static void getOpenRouterModels(Map<String, LanguageModel> modelsCopy) {
        // Add OpenRouter models if API key exists
        OpenRouterChatModelFactory openRouterChatModelFactory = new OpenRouterChatModelFactory();
        String apiKey = openRouterChatModelFactory.getApiKey(ModelProvider.OpenRouter);
        if (apiKey != null && !apiKey.isEmpty()) {
            openRouterChatModelFactory.getModels().forEach(model ->
                modelsCopy.put(ModelProvider.OpenRouter.getName() + ":" + model.getModelName(), model));
        }
    }

    public void setModels(Map<String, LanguageModel> models) {
        this.models = new HashMap<>(models);
    }
}
