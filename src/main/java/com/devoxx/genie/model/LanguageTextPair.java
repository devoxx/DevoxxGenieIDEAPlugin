package com.devoxx.genie.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LanguageTextPair {
    private String language;
    private String text;

    public LanguageTextPair() {
    }

    public LanguageTextPair(String language, String text) {
        this.language = language;
        this.text = text;
    }
}
