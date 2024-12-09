package com.devoxx.genie.service.rag;

import com.devoxx.genie.service.rag.validator.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Service
public final class RagValidatorService {

    public static RagValidatorService getInstance() {
        return ApplicationManager.getApplication().getService(RagValidatorService.class);
    }

    public ValidationResult validate() {
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
        return List.of(
                new DockerValidator(),
                new ChromeDBValidator(),
                new OllamaValidator(),
                new NomicEmbedTextValidator()
        );
    }

    private String getStatusMessage(@NotNull Validator validator, boolean isValid) {
        if (isValid) {
            return validator.getMessage();
        } else {
            return validator.getErrorMessage();
        }
    }
}