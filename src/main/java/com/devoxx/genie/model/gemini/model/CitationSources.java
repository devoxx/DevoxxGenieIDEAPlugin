package com.devoxx.genie.model.gemini.model;

// CitationSources.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitationSources {
    int startIndex;
    int endIndex;
    String uri;
    String license;
}
