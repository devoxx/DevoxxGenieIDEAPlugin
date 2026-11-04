package com.devoxx.genie.service.rag.embedding;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpClient;

/**
 * Builds the embedding client for a provider. No IntelliJ dependencies: the plugin passes in
 * values from the LLM settings, and the standalone CLI passes in environment variables.
 *
 * <p>The output dimension is never sent to the server, because local servers may ignore it.
 * {@link ProfiledEmbedder} truncates the returned vectors instead.
 */
public final class EmbeddingModelFactory {

    /** Local servers ignore the key, but the OpenAI client requires one. */
    private static final String PLACEHOLDER_API_KEY = "na";

    private EmbeddingModelFactory() {}

    public static @NotNull EmbeddingModel create(@NotNull EmbeddingProvider provider,
                                                 @NotNull String baseUrl,
                                                 @Nullable String apiKey,
                                                 @NotNull String modelName) {
        if (provider == EmbeddingProvider.OLLAMA) {
            return OllamaEmbeddingModel.builder().baseUrl(baseUrl).modelName(modelName).build();
        }
        return OpenAiEmbeddingModel.builder()
                .baseUrl(openAiBaseUrl(provider, baseUrl))
                .apiKey(apiKey == null || apiKey.isBlank() ? PLACEHOLDER_API_KEY : apiKey)
                .modelName(modelName)
                // Same as the chat models: LM Studio doesn't support HTTP/2.
                .httpClientBuilder(JdkHttpClient.builder().httpClientBuilder(
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)))
                .build();
    }

    /**
     * llama.cpp's default URL has no {@code /v1}, which is fine for chat, but its OpenAI-format
     * embeddings are served under {@code /v1/embeddings}.
     */
    public static @NotNull String openAiBaseUrl(@NotNull EmbeddingProvider provider, @NotNull String baseUrl) {
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        return provider == EmbeddingProvider.LLAMA_CPP && !trimmed.endsWith("/v1")
                ? trimmed + "/v1"
                : trimmed;
    }
}
