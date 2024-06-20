package com.devoxx.genie.model.gemini.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Candidate {
    Content content;
    String finishReason;
    int index;
    List<SafetyRatings> safetyRatings;  // unused
    CitationMetadata citationMetadata;  // unused
}
