package com.devoxx.genie.service.semanticsearch.validator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@Service
public final class ValidationService {

    public static ValidationService getInstance() {
        return ApplicationManager.getApplication().getService(ValidationService.class);
    }

    public @NotNull ValidationResult validateSemanticSearch() {
        List<Validator> validators = createValidators();

        boolean allValid = validators.stream().allMatch(Validator::isValid);

        return new ValidationResult(
                allValid,
                validators.stream()
                        .map(validator -> new ValidatorStatus(
                                validator.getName(),
                                validator.isValid(),
                                validator.getValidationMessage()
                        ))
                        .collect(Collectors.toList())
        );
    }

    private List<Validator> createValidators() {
        return List.of(
                new DockerValidator(),
                new ChromeDBValidator(),
                new OllamaValidator(),
                new NomicEmbedTextValidator()
        );
    }
}

