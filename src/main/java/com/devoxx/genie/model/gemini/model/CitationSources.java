package com.devoxx.genie.model.gemini.model;

// CitationSources.java
import lombok.*;

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
