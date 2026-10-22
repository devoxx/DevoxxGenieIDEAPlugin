package com.devoxx.genie.service.models;

import com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareChatModelFactory;
import com.devoxx.genie.chatmodel.cloud.openrouter.OpenRouterChatModelFactory;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.models.ModelConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public final class LLMModelRegistryService {

    private volatile Map<String, LanguageModel> models = new HashMap<>();

    @NotNull
    public static LLMModelRegistryService getInstance() {
        return ApplicationManager.getApplication().getService(LLMModelRegistryService.class);
    }

    public LLMModelRegistryService() {
        loadBundledModels();
        loadRemoteConfigFromCache();
    }

    private final ModelCatalogLoader catalogLoader = new ModelCatalogLoader();

    private void loadBundledModels() {
        models = catalogLoader.toModels(catalogLoader.loadBundled());
    }

    private void loadRemoteConfigFromCache() {
        try {
            ModelConfig config = ModelConfigService.getInstance().getModelConfig();
            if (config != null) refreshFromRemoteConfig(config);
        } catch (Exception e) {
            log.warn("Failed to load remote model config from cache: {}", e.getMessage());
        }
    }

    /** Rebuild from the bundled baseline so removed remote entries cannot linger. */
    public synchronized void refreshFromRemoteConfig(@NotNull ModelConfig config) {
        Map<String, LanguageModel> updated = catalogLoader.toModels(catalogLoader.loadBundled());
        Map<String, LanguageModel> remote = catalogLoader.toModels(config);
        java.util.Set<ModelProvider> providers = remote.values().stream()
                .map(LanguageModel::getProvider).collect(java.util.stream.Collectors.toSet());
        updated.values().removeIf(model -> providers.contains(model.getProvider()));
        updated.putAll(remote);
        models = updated;
    }

    @NotNull
    public List<LanguageModel> getModels() {

        // Create a copy of the current models
        Map<String, LanguageModel> modelsCopy = new HashMap<>(models);

        getOpenRouterModels(modelsCopy);
        getCloudflareModels(modelsCopy);

        return new ArrayList<>(modelsCopy.values());
    }

    private static void getOpenRouterModels(Map<String, LanguageModel> modelsCopy) {
        // Add OpenRouter models if API key exists
        OpenRouterChatModelFactory openRouterChatModelFactory = new OpenRouterChatModelFactory();
        String apiKey = openRouterChatModelFactory.getApiKey(ModelProvider.OpenRouter);
        if (apiKey != null && !apiKey.isEmpty()) {
            replaceWithLiveModels(modelsCopy, ModelProvider.OpenRouter, openRouterChatModelFactory.getModels());
        }
    }

    private static void getCloudflareModels(Map<String, LanguageModel> modelsCopy) {
        // Add Cloudflare AI Gateway models when the API key and account id are configured.
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String apiKey = state.getCloudflareKey();
        String accountId = state.getCloudflareAccountId();
        if (apiKey != null && !apiKey.isBlank() && accountId != null && !accountId.isBlank()) {
            replaceWithLiveModels(modelsCopy, ModelProvider.Cloudflare, new CloudflareChatModelFactory().getModels());
        }
    }

    private static void replaceWithLiveModels(Map<String, LanguageModel> models, ModelProvider provider,
                                              List<LanguageModel> liveModels) {
        if (liveModels == null || liveModels.isEmpty()) return;
        models.values().removeIf(model -> model.getProvider() == provider);
        liveModels.forEach(model -> models.put(provider.getName() + ":" + model.getModelName(), model));
    }

    public void setModels(Map<String, LanguageModel> models) {
        this.models = new HashMap<>(models);
    }
}
