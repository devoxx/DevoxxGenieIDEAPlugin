package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.chatmodel.local.ollama.OllamaModelService;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

/**
 * Checks that the configured RAG embedding model (TASK-212) is installed in Ollama. Matches
 * the exact model name, treating {@code name} and {@code name:latest} as the same model.
 */
public class EmbeddingModelValidator implements Validator {
    private String message;
    private ValidationActionType action = ValidationActionType.OK;

    @Override
    public boolean isValid() {
        action = ValidationActionType.OK;
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String ollamaModelUrl = state.getOllamaModelUrl();
        if (ollamaModelUrl == null || ollamaModelUrl.isEmpty()) {
            this.message = "Ollama model URL is not set";
            return false;
        }
        String modelName = EmbeddingFingerprint.ollama(state.getRagEmbeddingModelName()).modelName();
        try {
            OllamaModelEntryDTO[] ollamaModels = OllamaModelService.getInstance().getModels();
            if (ollamaModels == null) {
                this.message = "Unable to check if embedding model " + modelName + " is present";
                return false;
            }

            for (OllamaModelEntryDTO model : ollamaModels) {
                if (model.getName() != null && EmbeddingFingerprint.sameOllamaModel(model.getName(), modelName)) {
                    this.message = "Embedding model " + modelName + " found";
                    return true;
                }
            }
            this.message = "Embedding model " + modelName + " not found";
            this.action = ValidationActionType.PULL_EMBEDDING_MODEL;
            return false;
        } catch (Exception e) {
            this.message = "Unable to check if embedding model " + modelName + " is present";
            return false;
        }
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
        return ValidatorType.EMBEDDING_MODEL;
    }
}
