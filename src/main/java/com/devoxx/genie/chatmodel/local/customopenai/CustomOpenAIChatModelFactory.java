package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.customopenai.CustomOpenAIModelEntryDTO;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.net.http.HttpClient;

@Slf4j
public class CustomOpenAIChatModelFactory implements ChatModelFactory {

    private List<LanguageModel> cachedModels = null;
    private boolean providerChecked = false;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService stateInstance = DevoxxGenieStateService.getInstance();
        return OpenAiChatModel.builder()
                .baseUrl(stateInstance.getCustomOpenAIUrl())
                .apiKey(stateInstance.isCustomOpenAIApiKeyEnabled() ? stateInstance.getCustomOpenAIApiKey() : "na")
                .modelName(resolveModelName(stateInstance, customChatModel))
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(customChatModel.getTemperature())
                .maxTokens(customChatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(customChatModel.getTopP())
                .listeners(getListener())
                .httpClientBuilder(getHttpClientBuilder())
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService stateInstance = DevoxxGenieStateService.getInstance();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(stateInstance.getCustomOpenAIUrl())
                .apiKey(stateInstance.isCustomOpenAIApiKeyEnabled() ? stateInstance.getCustomOpenAIApiKey() : "na")
                .modelName(resolveModelName(stateInstance, customChatModel))
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .listeners(getListener())
                .httpClientBuilder(getHttpClientBuilder())
                .build();
    }

    /**
     * Resolves the model name to use.
     * If the custom model name setting is enabled, uses the manually configured name.
     * Otherwise, falls back to the model name selected from the dropdown (passed via CustomChatModel).
     */
    private String resolveModelName(@NotNull DevoxxGenieStateService stateInstance,
                                    @NotNull CustomChatModel customChatModel) {
        if (stateInstance.isCustomOpenAIModelNameEnabled()) {
            String configuredName = stateInstance.getCustomOpenAIModelName();
            return (configuredName == null || configuredName.isBlank()) ? "default" : configuredName;
        }
        // Use model name from the dropdown selection
        String modelName = customChatModel.getModelName();
        return (modelName != null && !modelName.isBlank()) ? modelName : "default";
    }

    private JdkHttpClientBuilder getHttpClientBuilder() {
        boolean forceHttp11 = DevoxxGenieStateService.getInstance().isCustomOpenAIForceHttp11();
        return forceHttp11
                ? JdkHttpClient.builder()
                        .httpClientBuilder(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1))
                : JdkHttpClient.builder();
    }

    /**
     * Get the model names from the custom OpenAI-compliant service.
     * Fetches models from the /models endpoint if available.
     * Returns an empty list if model fetching fails (users can still use manual model name configuration).
     *
     * @return List of available models
     */
    @Override
    public List<LanguageModel> getModels() {
        if (!providerChecked) {
            fetchAndCacheModels();
        }
        return cachedModels != null ? cachedModels : Collections.emptyList();
    }

    private void fetchAndCacheModels() {
        try {
            CustomOpenAIModelEntryDTO[] models = CustomOpenAIModelService.getInstance().getModels();
            List<LanguageModel> modelList = new ArrayList<>();
            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
            boolean apiKeyUsed = stateService.isCustomOpenAIApiKeyEnabled();

            for (CustomOpenAIModelEntryDTO model : models) {
                if (model.getId() != null && !model.getId().isBlank()) {
                    modelList.add(LanguageModel.builder()
                            .provider(ModelProvider.CustomOpenAI)
                            .modelName(model.getId())
                            .displayName(model.getId())
                            .inputCost(0)
                            .outputCost(0)
                            .inputMaxTokens(0)
                            .apiKeyUsed(apiKeyUsed)
                            .build());
                }
            }
            cachedModels = modelList;
        } catch (Exception e) {
            log.debug("Could not fetch models from Custom OpenAI endpoint: {}", e.getMessage());
            cachedModels = Collections.emptyList();
        } finally {
            providerChecked = true;
        }
    }

    @Override
    public void resetModels() {
        cachedModels = null;
        providerChecked = false;
    }
}
