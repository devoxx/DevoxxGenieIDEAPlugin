package com.devoxx.genie.service.rag.validator;

public interface Validator {

    boolean isValid();

    String getMessage();

    String getErrorMessage();

    ValidatorType getCommand();

    default ValidationActionType getAction() {
        return ValidationActionType.OK;
    }
}
