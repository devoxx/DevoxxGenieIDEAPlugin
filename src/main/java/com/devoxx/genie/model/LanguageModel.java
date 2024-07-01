package com.devoxx.genie.model;

import com.devoxx.genie.model.enumarations.ModelProvider;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
public class LanguageModel implements Comparable<LanguageModel> {
    private ModelProvider provider;
    private String modelName;
    private String displayName;
    private boolean apiKeyUsed;
    private double inputCost;
    private double outputCost;
    private int contextWindow;

    public int compareTo(@NotNull LanguageModel languageModel) {
        return this.displayName.compareTo(languageModel.displayName);
    }

    public String toString() {
        return provider.getName();
    }
}
