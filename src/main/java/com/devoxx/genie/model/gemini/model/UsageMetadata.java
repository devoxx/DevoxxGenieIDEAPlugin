package com.devoxx.genie.model.gemini.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsageMetadata {
    Integer promptTokenCount;
    Integer candidatesTokenCount;
    Integer totalTokenCount;
}
