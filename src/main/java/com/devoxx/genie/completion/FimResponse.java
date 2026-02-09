package com.devoxx.genie.completion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FimResponse {
    private final String completionText;
    private final long durationMs;
}
