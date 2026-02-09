package com.devoxx.genie.completion;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FimRequest {
    private final String prefix;
    private final String suffix;
    private final String modelName;
    private final String baseUrl;
    @Builder.Default
    private final double temperature = 0.0;
    @Builder.Default
    private final int maxTokens = 64;
    @Builder.Default
    private final int timeoutMs = 5000;
}
