package com.devoxx.genie.service.rag.validator;

import java.util.List;

public record ValidationResult(boolean isValid, List<ValidatorStatus> statuses) {}
