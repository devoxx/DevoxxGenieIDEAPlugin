package com.devoxx.genie.model.gemini;

import com.devoxx.genie.model.gemini.model.Candidate;
import com.devoxx.genie.model.gemini.model.UsageMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiCompletionResponse {
    List<Candidate> candidates;
    UsageMetadata usageMetadata;
}
