package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.ollama.OllamaService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

public class NomicEmbedTextValidator implements Validator {
    private String message;
    private ValidationActionType action = ValidationActionType.OK;

    @Override
    public boolean isValid() {
        String ollamaModelUrl = DevoxxGenieStateService.getInstance().getOllamaModelUrl();
        if (ollamaModelUrl == null || ollamaModelUrl.isEmpty()) {
            this.message = "Ollama model URL is not set";
            return false;
        }
        try {
            OllamaModelEntryDTO[] ollamaModels = OllamaService.getInstance().getModels();
            if (ollamaModels == null) {
                this.message = "Unable to check if Nomic Embed model is present";
                return false;
            }

            for (OllamaModelEntryDTO model : ollamaModels) {
                if (model.getName().startsWith("nomic-embed-text")) {
                    this.message = "Nomic Embed model found";
                    return true;
                }
            }
            this.message = "Nomic Embed model not found";
            this.action = ValidationActionType.PULL_NOMIC;
            return false;
        } catch (Exception e) {
            this.message = "Unable to check if Nomic Embed model is present";
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
        return ValidatorType.NOMIC;
    }
}
