package com.devoxx.genie.service.rag.validator;

public record ValidatorStatus(
        Validator validator,
        ValidatorType validatorType,
        ValidationActionType action,
        boolean isValid,
        String message) {}