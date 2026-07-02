package com.devoxx.genie.chatmodel.cloud.nvidia;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NVIDIA NIM (build.nvidia.com) chat model factory. NVIDIA exposes an
 * OpenAI-compatible endpoint at {@code https://integrate.api.nvidia.com/v1},
 * so we reuse the langchain4j OpenAI client and only swap the base URL + API key.
 */
public class NvidiaChatModelFactory implements ChatModelFactory {

    private static final Logger LOG = Logger.getInstance(NvidiaChatModelFactory.class);

    private static final String BASE_URL = "https://integrate.api.nvidia.com/v1";
    private static final String MODELS_URL = BASE_URL + "/models";

    /**
     * NVIDIA's OpenAI-compatible {@code /v1/models} endpoint returns only model ids
     * (standard OpenAI {@code {"data":[{"id":...}]}} shape) with no context length, so
     * every dynamically-listed model is given a heuristic window instead of the
     * primitive-int default of 0 (the zero-context symptom seen through the generic
     * "Custom OpenAI URL" path). This value only feeds the token-usage bar and the
     * "add project to context" budget — it never truncates a conversation — so a
     * best-effort approximation is fine. 128K covers the bulk of NVIDIA's instruct
     * LLMs (Llama 3.x, Nemotron, Qwen3, DeepSeek, GPT-OSS, GLM, Kimi, Mistral 2/3, …).
     */
    private static final int DEFAULT_CONTEXT_WINDOW = 128_000;

    /**
     * Heuristic context-window overrides for the model families whose window is NOT 128K,
     * keyed by a lowercase substring of the model id and checked in insertion order
     * (first match wins). Values are derived from published NIM/model specs. Auxiliary /
     * non-chat models (embeddings, rerankers, safety-guards, OCR/parse, translation,
     * vision-only) are listed FIRST and given a modest window so a base-model name in
     * their id (e.g. {@code nv-embedqa-mistral-7b}) cannot mis-bucket them. Anything not
     * matched here falls back to {@link #DEFAULT_CONTEXT_WINDOW}. Verified against the
     * full live {@code /v1/models} catalogue (121 models).
     */
    private static final Map<String, Integer> CONTEXT_WINDOW_BY_SUBSTRING = new LinkedHashMap<>();
    static {
        Map<String, Integer> m = CONTEXT_WINDOW_BY_SUBSTRING;
        // --- Auxiliary / non-chat (embeddings, rerankers, moderation, OCR/parse, translation, vision-only) ---
        m.put("embed", 8_192);
        m.put("bge-", 8_192);
        m.put("nvclip", 8_192);
        m.put("nemoretriever", 8_192);
        m.put("-parse", 8_192);
        m.put("guard", 8_192);
        m.put("safety", 8_192);
        m.put("topic-control", 8_192);
        m.put("reward", 8_192);
        m.put("gliner", 8_192);
        m.put("translate", 8_192);
        m.put("detector", 8_192);
        m.put("deplot", 8_192);
        m.put("kosmos", 8_192);
        m.put("diffusiongemma", 8_192);
        // --- 4K-context models ---
        m.put("nemotron-4-340b", 4_096);
        m.put("nemotron-mini", 4_096);
        m.put("llama2-70b", 4_096);
        m.put("solar-10.7b", 4_096);
        m.put("neva", 4_096);
        m.put("fuyu", 4_096);
        m.put("vila", 4_096);
        // --- Code models ---
        m.put("codellama", 16_384);
        m.put("starcoder2", 16_384);
        m.put("deepseek-coder", 16_384);
        m.put("granite-34b-code", 8_192);
        m.put("granite-8b-code", 8_192);
        m.put("codestral", 32_768);
        // --- 8K-context models ---
        m.put("minitron-8b-8k", 8_192);
        m.put("chatqa", 8_192);
        m.put("sea-lion", 8_192);
        m.put("gemma-2b", 8_192);
        m.put("gemma-2-", 8_192);
        m.put("codegemma", 8_192);
        m.put("recurrentgemma", 8_192);
        // --- 16K-context models ---
        m.put("zamba2", 16_384);
        // --- 32K-context models ---
        m.put("gemma-3n", 32_768);
        m.put("yi-large", 32_768);
        m.put("mixtral-8x7b", 32_768);
        m.put("mistral-7b", 32_768);
        m.put("dbrx", 32_768);
        m.put("sarvam", 32_768);
        m.put("palmyra-fin", 32_768);
        m.put("palmyra-med", 32_768);
        // Mistral Large: v2/v3 keep 128K; plain v1 is 32K (more specific keys first).
        m.put("mistral-large-2", 128_000);
        m.put("mistral-large-3", 128_000);
        m.put("mistral-large", 32_768);
        // --- 64K-context models ---
        m.put("mixtral-8x22b", 65_536);
        // --- 256K-context models ---
        m.put("jamba", 256_000);
        // --- 1M-context models ---
        m.put("llama-4", 1_000_000);
        m.put("maverick", 1_000_000);
    }

    /**
     * Best-effort context window for a NVIDIA model id, using {@link #CONTEXT_WINDOW_BY_SUBSTRING}
     * (first substring match wins) and falling back to {@link #DEFAULT_CONTEXT_WINDOW}.
     */
    static int contextWindowFor(@NotNull String modelId) {
        String id = modelId.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : CONTEXT_WINDOW_BY_SUBSTRING.entrySet()) {
            if (id.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_CONTEXT_WINDOW;
    }

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

    private final ModelProvider MODEL_PROVIDER = ModelProvider.Nvidia;

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
     * Returns the full catalogue of NVIDIA models by probing the OpenAI-compatible
     * {@code /v1/models} endpoint. On any failure (unreachable/empty/unparseable) this
     * falls back to the curated hardcoded list from the model registry so the provider
     * still offers models offline. Successful probes are cached until {@link #resetModels()}.
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
            ResponseDTO response = LocalLLMProviderUtil.getModelsFromUrl(MODELS_URL, ResponseDTO.class, MODELS_PROBE_CLIENT);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .filter(model -> model != null && model.getId() != null && !model.getId().isBlank())
                    .map(model -> LanguageModel.builder()
                            .provider(MODEL_PROVIDER)
                            .modelName(model.getId())
                            .displayName(model.getId())
                            .inputCost(0)
                            .outputCost(0)
                            .inputMaxTokens(contextWindowFor(model.getId()))
                            .apiKeyUsed(true)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn("Could not fetch NVIDIA models from '" + MODELS_URL + "': " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
