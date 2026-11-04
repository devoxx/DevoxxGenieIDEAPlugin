package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.service.rag.validator.*;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

@Service
public final class RagValidatorService {

    public static RagValidatorService getInstance() {
        return ApplicationManager.getApplication().getService(RagValidatorService.class);
    }

    /** Makes network and Docker calls, so never call it on the EDT. */
    public ValidationResult validate() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("RAG validation must not run on the EDT");
        }
        List<ValidatorStatus> statuses = new ArrayList<>();
        List<Validator> validators = createValidators();

        for (Validator validator : validators) {
            boolean isValid = validator.isValid();
            statuses.add(new ValidatorStatus(
                    validator,
                    validator.getCommand(),
                    validator.getAction(),
                    isValid,
                    getStatusMessage(validator, isValid)
            ));
        }

        boolean allValid = statuses.stream().allMatch(ValidatorStatus::isValid);
        return new ValidationResult(allValid, statuses);
    }

    private List<Validator> createValidators() {
        List<Validator> validators = new ArrayList<>(List.of(new DockerValidator(), new ChromeDBValidator()));
        validators.addAll(embeddingValidators(
                EmbeddingProvider.fromId(DevoxxGenieStateService.getInstance().getRagEmbeddingProvider())));
        return validators;
    }

    /** The Ollama checks only apply to Ollama; they must not block RAG on any other provider. */
    static @NotNull List<Validator> embeddingValidators(@NotNull EmbeddingProvider provider) {
        return provider == EmbeddingProvider.OLLAMA
                ? List.of(new OllamaValidator(), new EmbeddingModelValidator())
                : List.of(new EmbeddingProviderValidator());
    }

    private String getStatusMessage(@NotNull Validator validator, boolean isValid) {
        if (isValid) {
            return validator.getMessage();
        } else {
            return validator.getErrorMessage();
        }
    }
}