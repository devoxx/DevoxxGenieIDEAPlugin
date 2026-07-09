package com.devoxx.genie.chatmodel.local.customopenai;

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
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.net.http.HttpClient;

public class CustomOpenAIChatModelFactory implements ChatModelFactory {

    private static final Logger LOG = Logger.getInstance(CustomOpenAIChatModelFactory.class);

    /**
     * Dedicated fast-fail client for the best-effort {@code /models} probe.
     *
     * <p>Unlike the shared {@link com.devoxx.genie.util.HttpClientProvider} client (10s connect
     * timeout + 3 retries with exponential backoff), this client makes a single attempt with a
     * short connect/read timeout. A model-list probe should degrade quickly to "type the model
     * name manually" rather than tie up the caller for tens of seconds when the endpoint is slow
     * or unreachable (e.g. a mistyped host).</p>
     */
    private static final OkHttpClient MODELS_PROBE_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5))
            .retryOnConnectionFailure(false)
            .build();

    /**
     * Model ids returned by the last {@link #getModels()} probe. {@code null} means "not yet fetched".
     * Cleared by {@link #resetModels()} (the Refresh button). Caching prevents a network round-trip
     * on every provider selection, which otherwise re-probed the endpoint each time.
     * <p>
     * Only the ids are cached, never the assembled {@link LanguageModel}s: context window and costs
     * come from settings, so caching built models froze whatever those settings were at first probe.
     */
    private volatile List<String> cachedModelIds = null;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService stateInstance = DevoxxGenieStateService.getInstance();
        return OpenAiChatModel.builder()
                .baseUrl(stateInstance.getCustomOpenAIUrl())
                .apiKey(stateInstance.isCustomOpenAIApiKeyEnabled() ? stateInstance.getCustomOpenAIApiKey() : "na")
                .modelName(resolveModelName(customChatModel))
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(customChatModel.getTemperature())
                .maxTokens(customChatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(customChatModel.getTopP())
                .returnThinking(ThinkingSupport.isEnabled())
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
                .modelName(resolveModelName(customChatModel))
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .returnThinking(ThinkingSupport.isEnabled())
                .listeners(getListener())
                .httpClientBuilder(getHttpClientBuilder())
                .build();
    }

    /**
     * Resolve the model name to send. Priority:
     * <ol>
     *   <li>the explicit "Custom OpenAI Model Name" override field, when enabled and non-blank;</li>
     *   <li>the model selected in the dropdown ({@code customChatModel.getModelName()});</li>
     *   <li>{@code "default"} as a last resort.</li>
     * </ol>
     * Never returns blank: an empty model name makes the OpenAI client omit the
     * required {@code "model"} field, which servers reject.
     */
    private static String resolveModelName(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        if (state.isCustomOpenAIModelNameEnabled()) {
            String override = state.getCustomOpenAIModelName();
            if (override != null && !override.isBlank()) {
                return override;
            }
        }
        String selected = customChatModel.getModelName();
        if (selected != null && !selected.isBlank()) {
            return selected;
        }
        return "default";
    }

    private JdkHttpClientBuilder getHttpClientBuilder() {
        boolean forceHttp11 = DevoxxGenieStateService.getInstance().isCustomOpenAIForceHttp11();
        return forceHttp11
                ? JdkHttpClient.builder()
                        .httpClientBuilder(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1))
                : JdkHttpClient.builder();
    }

    /**
     * Fetch the model list from the custom OpenAI-compatible server's
     * {@code /models} endpoint (standard OpenAI {@code {"data":[{"id":...}]}}
     * shape) so the UI can offer a model picker.
     *
     * Best-effort: if the URL is unset, the server is unreachable, has no
     * {@code /models} endpoint, or returns an empty/unparseable body, this
     * returns an empty list rather than throwing — the user can still type a
     * model name manually and the rest of model loading is unaffected.
     *
     * @return list of available models, or an empty list on any failure
     */
    @Override
    public List<LanguageModel> getModels() {
        List<String> cached = cachedModelIds;
        if (cached == null) {
            cached = fetchModelIdsFromServer();
            // Only cache a successful, non-empty probe. An empty result usually means the endpoint
            // was momentarily unreachable/slow (e.g. right after IDE startup); caching it would make
            // the model list stick empty until a manual Refresh. Leaving it uncached lets the next
            // (background, bounded) probe recover once the endpoint is available.
            if (!cached.isEmpty()) {
                cachedModelIds = cached;
            }
        }
        return cached.stream()
                .map(CustomOpenAIChatModelFactory::toLanguageModel)
                .collect(Collectors.toList());
    }

    @Override
    public void resetModels() {
        cachedModelIds = null;
    }

    /**
     * Build a {@link LanguageModel} for a model id using the <em>current</em> settings, so a context
     * window or cost edited in Settings takes effect without re-probing the endpoint.
     */
    private static @NotNull LanguageModel toLanguageModel(@NotNull String modelId) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return LanguageModel.builder()
                .provider(ModelProvider.CustomOpenAI)
                .modelName(modelId)
                .displayName(modelId)
                .inputCost(CustomOpenAICost.resolve(state.getCustomOpenAIInputCost()))
                .outputCost(CustomOpenAICost.resolve(state.getCustomOpenAIOutputCost()))
                .inputMaxTokens(CustomOpenAIContextWindow.resolve(state.getCustomOpenAIContextWindow()))
                .apiKeyUsed(state.isCustomOpenAIApiKeyEnabled())
                .build();
    }

    private List<String> fetchModelIdsFromServer() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String baseUrl = state.getCustomOpenAIUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String modelsUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "models";
            ResponseDTO response = LocalLLMProviderUtil.getModelsFromUrl(modelsUrl, ResponseDTO.class, MODELS_PROBE_CLIENT);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .filter(model -> model != null && model.getId() != null && !model.getId().isBlank())
                    .map(Model::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Degrade gracefully: a missing/empty/failing models endpoint must
            // not break model loading for the Custom OpenAI provider.
            LOG.warn("Could not fetch models from custom OpenAI endpoint '" + baseUrl + "': " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
