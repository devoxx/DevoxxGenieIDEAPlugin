package com.devoxx.genie.service.semanticsearch.validator;

public interface Validator {
    boolean isValid();
    default String getValidationMessage() {
        return null;
    }
    String getName();
    String getCommand();
}
