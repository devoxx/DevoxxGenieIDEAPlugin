package com.devoxx.genie.service.semanticsearch.validator;

import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.ollama.OllamaService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

public class NomicEmbedTextValidator implements Validator {

    @Override
    public boolean isValid() {
        String ollamaModelUrl = DevoxxGenieStateService.getInstance().getOllamaModelUrl();
        if (ollamaModelUrl == null || ollamaModelUrl.isEmpty()) {
            return false;
        }
        try {
            OllamaModelEntryDTO[] ollamaModels = OllamaService.getInstance().getModels();
            if (ollamaModels == null || ollamaModels.length == 0) {
                return false;
            }

            for (OllamaModelEntryDTO model : ollamaModels) {
                if (model.getName().startsWith("nomic-embed-text")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String getName() {
        return "Nomic Embed Text check";
    }

    public String getCommand() {
        return ValidatorType.NOMIC.name().toLowerCase();
    }
}
