package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.chatmodel.local.ollama.OllamaModelService;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

/**
 * Validates that the configured generative chat model used by the reranker is available in
 * the local Ollama instance.
 *
 * <p>The reranker uses chat-completion scoring against {@code /api/generate} (Ollama has no
 * dedicated rerank endpoint), so the model being validated here is an instruction-tuned
 * generative model such as {@code llama3.2:1b} — <strong>not</strong> a cross-encoder model
 * like {@code bge-reranker} (those are served via {@code /api/embeddings} and would always
 * return empty/malformed responses through this path).
 *
 * <p>Matches the {@link NomicEmbedTextValidator} pattern: only runs the check when the
 * reranker feature toggle is on, surfaces a "model not found" status with the
 * {@link ValidationActionType#PULL_RERANKER} action so the UI can offer a one-click pull.
 *
 * <p>The check is intentionally lenient on name matching (compared on the base name with the
 * {@code :tag} stripped) because Ollama appends tags (e.g. {@code llama3.2:1b}) and users
 * may pull different tagged variants.
 */
public class RerankerValidator implements Validator {

    private String message;
    private ValidationActionType action = ValidationActionType.OK;

    @Override
    public boolean isValid() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        // The reranker is opt-in — treat the validator as a no-op when the user has not
        // enabled it so the RAG status panel doesn't surface a noisy "model not found"
        // banner for a feature nobody is using.
        if (!Boolean.TRUE.equals(state.getRerankResults())) {
            this.message = "Reranker disabled";
            return true;
        }

        String ollamaModelUrl = state.getOllamaModelUrl();
        if (ollamaModelUrl == null || ollamaModelUrl.isEmpty()) {
            this.message = "Ollama model URL is not set";
            return false;
        }

        String configuredModel = state.getRerankerModelName();
        if (configuredModel == null || configuredModel.isEmpty()) {
            this.message = "Reranker model name is not set";
            return false;
        }

        try {
            OllamaModelEntryDTO[] ollamaModels = OllamaModelService.getInstance().getModels();
            if (ollamaModels == null) {
                this.message = "Unable to check if reranker model is present";
                return false;
            }

            // Compare on the base name (strip any ":tag") so "bge-reranker" matches
            // "bge-reranker:latest" pulled by Ollama.
            String wanted = stripTag(configuredModel);
            for (OllamaModelEntryDTO model : ollamaModels) {
                String name = model.getName();
                if (name != null && stripTag(name).equalsIgnoreCase(wanted)) {
                    this.message = "Reranker model '" + configuredModel + "' found";
                    return true;
                }
            }
            this.message = "Reranker model '" + configuredModel + "' not found in Ollama";
            this.action = ValidationActionType.PULL_RERANKER;
            return false;
        } catch (Exception e) {
            this.message = "Unable to check if reranker model is present";
            return false;
        }
    }

    private static String stripTag(String name) {
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(0, idx) : name;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public String getErrorMessage() {
        return this.message;
    }

    @Override
    public ValidationActionType getAction() {
        return this.action;
    }

    @Override
    public ValidatorType getCommand() {
        return ValidatorType.RERANKER;
    }
}
