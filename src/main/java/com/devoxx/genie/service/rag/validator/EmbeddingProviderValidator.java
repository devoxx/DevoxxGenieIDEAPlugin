package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.service.rag.embedding.EmbeddingModelFactory;
import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.service.rag.embedding.EmbeddingProviderSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Checks an OpenAI-style embedding provider: enabled in the LLM settings, has a model, and its
 * server answers. Any HTTP response counts, because not every server implements {@code /models}
 * and this runs without the API key: reading the key could show a keychain dialog here. A
 * model or key problem shows up when indexing test-embeds before it touches the index.
 */
public class EmbeddingProviderValidator implements Validator {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private String message;

    @Override
    public boolean isValid() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        EmbeddingProvider provider = EmbeddingProvider.fromId(state.getRagEmbeddingProvider());
        if (!EmbeddingProviderSettings.isEnabled(provider, state)) {
            message = provider + " is disabled in the LLM settings";
            return false;
        }
        String url = EmbeddingProviderSettings.baseUrl(provider, state);
        if (url == null || url.isBlank()) {
            message = "Set the " + provider + " URL in the LLM settings";
            return false;
        }
        String model = state.getRagEmbeddingModelName();
        if (model == null || model.isBlank()) {
            message = "Enter an embedding model for " + provider;
            return false;
        }
        if (!isReachable(provider, url)) {
            message = provider + " is not reachable at " + url;
            return false;
        }
        message = provider + " is reachable";
        return true;
    }

    static boolean isReachable(@NotNull EmbeddingProvider provider, @NotNull String baseUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(EmbeddingModelFactory.openAiBaseUrl(provider, baseUrl) + "/models"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getErrorMessage() {
        return message;
    }

    @Override
    public ValidationActionType getAction() {
        return ValidationActionType.OK;
    }

    @Override
    public ValidatorType getCommand() {
        return ValidatorType.EMBEDDING_PROVIDER;
    }
}
