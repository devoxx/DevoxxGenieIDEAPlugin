package com.devoxx.genie.model.gemini.model;

// CitationMetadata.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitationMetadata {
    List<CitationSources> citationSources;
}
