package com.devoxx.genie.service.rag.embedding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Makes sure the embedding model is installed before indexing starts, asking the user before
 * downloading it. Blocking, so it must run off the EDT. Only Ollama implements {@link ModelSource}
 * today; other providers skip this step and rely on the indexer's test embed.
 */
public final class EmbeddingModelInstaller {

    private static final String INDEX_KEPT = "The existing RAG index was not changed.";

    /** A provider that can list and download models; the plugin passes in the real service. */
    public interface ModelSource {
        List<String> installedModels() throws IOException;

        void pull(String model, Consumer<String> status) throws IOException;
    }

    /** {@code problem} is a message for the user, set only when the model is not ready. */
    public record Result(boolean ready, @Nullable String problem) {
        public static Result success() {
            return new Result(true, null);
        }

        public static Result failure(String problem) {
            return new Result(false, problem);
        }
    }

    private EmbeddingModelInstaller() {}

    /**
     * @param confirmDownload asked with the model name when it is missing; {@code false} declines
     * @param status          receives download progress messages
     */
    public static @NotNull Result ensureInstalled(@NotNull String model,
                                                  @NotNull ModelSource source,
                                                  @NotNull Predicate<String> confirmDownload,
                                                  @NotNull Consumer<String> status) {
        try {
            if (isInstalled(model, source)) {
                return Result.success();
            }
        } catch (IOException e) {
            return Result.failure("Could not check whether embedding model " + model
                    + " is installed. Is Ollama running? " + INDEX_KEPT);
        }

        if (!confirmDownload.test(model)) {
            return Result.failure("Embedding model " + model + " is not installed. " + INDEX_KEPT);
        }

        try {
            source.pull(model, status);
            // Confirms the download produced the model Ollama will be asked to embed with.
            if (!isInstalled(model, source)) {
                return Result.failure("Downloaded embedding model " + model
                        + " but Ollama does not list it. " + INDEX_KEPT);
            }
            return Result.success();
        } catch (IOException e) {
            return Result.failure("Could not download embedding model " + model + ": "
                    + e.getMessage() + ". " + INDEX_KEPT);
        }
    }

    private static boolean isInstalled(String model, ModelSource source) throws IOException {
        return source.installedModels().stream()
                .anyMatch(installed -> EmbeddingFingerprint.sameOllamaModel(installed, model));
    }
}
