package com.devoxx.genie.chatmodel.cloud.demonroute;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ThinkingSupport;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
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
 * DemonRoute chat model factory. DemonRoute exposes an OpenAI-compatible
 * endpoint at {@code https://api.demonroute.com/v1}, so we reuse the
 * langchain4j OpenAI client and only swap the base URL + API key.
 */
public class DemonRouteChatModelFactory implements ChatModelFactory {

    private static final Logger LOG = Logger.getInstance(DemonRouteChatModelFactory.class);

    private static final String BASE_URL = "https://api.demonroute.com/v1";
    private static final String MODELS_URL = BASE_URL + "/models";

    /**
     * DemonRoute's OpenAI-compatible {@code /v1/models} endpoint returns only model ids
     * (standard OpenAI {@code {"data":[{"id":...}]}} shape) with no context length, so
     * every dynamically-listed model is given a default window instead of the
     * primitive-int default of 0. This value only feeds the token-usage bar and the
     * "add project to context" budget — it never truncates a conversation.
     */
    private static final int DEFAULT_CONTEXT_WINDOW = 128_000;

    /**
     * Dedicated fast-fail client for the best-effort {@code /models} probe: a single
     * attempt with short timeouts so the model-list fetch degrades quickly to the
     * curated fallback rather than tying up the caller when the endpoint is slow.
     */
    private static final OkHttpClient MODELS_PROBE_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5))
            .retryOnConnectionFailure(false)
            .build();

    /** Cached result of the last {@link #getModels()} probe. Cleared by {@link #resetModels()} (Refresh). */
    private volatile List<LanguageModel> cachedModels = null;

    private final ModelProvider MODEL_PROVIDER = ModelProvider.DemonRoute;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return OpenAiChatModel.builder()
            .baseUrl(BASE_URL)
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
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
        return OpenAiStreamingChatModel.builder()
            .baseUrl(BASE_URL)
            .apiKey(getApiKey(MODEL_PROVIDER))
            .modelName(customChatModel.getModelName())
            .temperature(customChatModel.getTemperature())
            .topP(customChatModel.getTopP())
            .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
            .returnThinking(ThinkingSupport.isEnabled())
            .listeners(getListener())
            .build();
    }

    /**
     * Returns the models available on DemonRoute by probing the OpenAI-compatible
     * {@code /v1/models} endpoint with the user's API key. On any failure
     * (unreachable/empty/unparseable) this falls back to the curated hardcoded list
     * from the model registry so the provider still offers a model offline.
     * Successful probes are cached until {@link #resetModels()}.
     */
    @Override
    public List<LanguageModel> getModels() {
        List<LanguageModel> cached = cachedModels;
        if (cached != null) {
            return cached;
        }
        List<LanguageModel> models = fetchModelsFromServer();
        if (models.isEmpty()) {
            // Degrade gracefully to the curated registry list; do not cache the fallback so
            // the next probe can recover once the endpoint is reachable.
            return getModels(MODEL_PROVIDER);
        }
        cachedModels = models;
        return models;
    }

    @Override
    public void resetModels() {
        cachedModels = null;
    }

    private List<LanguageModel> fetchModelsFromServer() {
        try {
            String apiKey = getApiKey(MODEL_PROVIDER);
            String bearer = (apiKey != null && !apiKey.isBlank()) ? apiKey : null;
            ResponseDTO response = LocalLLMProviderUtil.getModelsFromUrl(MODELS_URL, ResponseDTO.class, MODELS_PROBE_CLIENT, bearer);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .filter(model -> model != null && model.getId() != null && !model.getId().isBlank())
                    .map(model -> toLanguageModel(model.getId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn("Could not fetch DemonRoute models from '" + MODELS_URL + "': " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static @NotNull LanguageModel toLanguageModel(@NotNull String modelId) {
        return LanguageModel.builder()
                .provider(ModelProvider.DemonRoute)
                .modelName(modelId)
                .displayName(modelId)
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(DEFAULT_CONTEXT_WINDOW)
                .apiKeyUsed(true)
                .build();
    }
}
