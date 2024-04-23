package com.devoxx.genie.model;

import lombok.Getter;

@Getter
public class LanguageTextPair {
    String language;
    String text;

    public LanguageTextPair(String language, String text) {
        this.language = language;
        this.text = text;
    }
}
