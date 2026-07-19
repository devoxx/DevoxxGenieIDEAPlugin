package com.devoxx.genie.chatmodel.cloud.cloudflare;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ThinkingSupport;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cloudflare AI Gateway provider. Talks to the OpenAI-compatible {@code /compat} endpoint of a
 * user-specified account + gateway, authenticating with a single Cloudflare API token (BYOK).
 *
 * <p>Follows the Custom OpenAI patterns: a fast-fail best-effort {@code /models} probe, a model-name
 * override that skips the probe, and caching of model ids only (never built {@link LanguageModel}s).</p>
 */
public class CloudflareChatModelFactory implements ChatModelFactory {

    private static final Logger LOG = Logger.getInstance(CloudflareChatModelFactory.class);

    /** Modern gateway models; Cloudflare's /compat/models does not report a context length. */
    private static final int DEFAULT_CONTEXT_WINDOW = 128_000;

    private static final OkHttpClient MODELS_PROBE_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5))
            .retryOnConnectionFailure(false)
            .build();

    /** Model ids from the last probe; {@code null} = not yet fetched. Cleared by {@link #resetModels()}. */
    private volatile List<String> cachedModelIds = null;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl(state))
                .apiKey(apiKeyOrPlaceholder(state))
                .modelName(resolveModelName(customChatModel))
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(customChatModel.getTemperature())
                .maxTokens(customChatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(customChatModel.getTopP())
                .returnThinking(ThinkingSupport.isEnabled())
                .listeners(getListener())
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl(state))
                .apiKey(apiKeyOrPlaceholder(state))
                .modelName(resolveModelName(customChatModel))
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .returnThinking(ThinkingSupport.isEnabled())
                .listeners(getListener())
                .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        // Honour an explicit model name: skip discovery entirely.
        if (state.isCloudflareModelNameEnabled()) {
            String override = state.getCloudflareModelName();
            if (override != null && !override.isBlank()) {
                return List.of(toLanguageModel(override.trim()));
            }
        }

        List<String> cached = cachedModelIds;
        if (cached == null) {
            cached = fetchModelIdsFromServer(state);
            if (!cached.isEmpty()) {
                cachedModelIds = cached;
            }
        }
        return cached.stream()
                .map(this::toLanguageModel)
                .collect(Collectors.toList());
    }

    @Override
    public void resetModels() {
        cachedModelIds = null;
    }

    private List<String> fetchModelIdsFromServer(@NotNull DevoxxGenieStateService state) {
        String base = CloudflareGatewayUrl.compatBaseUrl(state.getCloudflareAccountId(), state.getCloudflareGatewayName());
        if (base == null) {
            return Collections.emptyList();
        }
        try {
            String modelsUrl = base + "/models";
            String token = state.getCloudflareKey();
            String bearer = (token != null && !token.isBlank()) ? token : null;
            ResponseDTO response = LocalLLMProviderUtil.getModelsFromUrl(modelsUrl, ResponseDTO.class, MODELS_PROBE_CLIENT, bearer);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .filter(model -> model != null && model.getId() != null && !model.getId().isBlank())
                    .map(Model::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.debug("Could not fetch models from Cloudflare AI Gateway: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private @NotNull LanguageModel toLanguageModel(@NotNull String modelId) {
        return LanguageModel.builder()
                .provider(ModelProvider.Cloudflare)
                .modelName(modelId)
                .displayName(modelId)
                .inputCost(0.0)
                .outputCost(0.0)
                .inputMaxTokens(DEFAULT_CONTEXT_WINDOW)
                .apiKeyUsed(true)
                .build();
    }

    private static String baseUrl(@NotNull DevoxxGenieStateService state) {
        String base = CloudflareGatewayUrl.compatBaseUrl(state.getCloudflareAccountId(), state.getCloudflareGatewayName());
        // langchain4j appends /chat/completions; a trailing slash keeps the join clean.
        return base == null ? null : base + "/";
    }

    private static String apiKeyOrPlaceholder(@NotNull DevoxxGenieStateService state) {
        String token = state.getCloudflareKey();
        return (token != null && !token.isBlank()) ? token : "na";
    }

    private static String resolveModelName(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        if (state.isCloudflareModelNameEnabled()) {
            String override = state.getCloudflareModelName();
            if (override != null && !override.isBlank()) {
                return override.trim();
            }
        }
        String selected = customChatModel.getModelName();
        return (selected != null && !selected.isBlank()) ? selected : "default";
    }
}
