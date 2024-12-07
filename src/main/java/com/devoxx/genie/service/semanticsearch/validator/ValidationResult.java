package com.devoxx.genie.service.semanticsearch.validator;

import java.util.List;

public record ValidationResult(boolean isValid, List<ValidatorStatus> statuses) {}
