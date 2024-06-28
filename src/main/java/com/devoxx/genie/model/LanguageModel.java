package com.devoxx.genie.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class LanguageModel implements Comparable<LanguageModel> {
    String name;
    String displayName;
    Integer maxTokens;

    @Override
    public int compareTo(@NotNull LanguageModel other) {
        return this.name.compareTo(other.name);
    }
}
