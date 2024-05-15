package com.devoxx.genie.model.gemini.model;

// CitationMetadata.java
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitationMetadata {
    List<CitationSources> citationSources;
}
