package com.devoxx.genie.ui.util;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Guesses the available IntelliJ Language of a fenced code block based on the language name.
 * Language.findLanguageByID is not used it returns Language.ANY for unknown/undefined languages
 * which makes com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet
 * fails.
 * Also, it is pretty common for markdown to use language identifier that not always match
 * the language id used by IntelliJ.
 * Inspired by https://github.com/asciidoctor/asciidoctor-intellij-plugin/blob/3ac99c53b21bc5d0ecb4961dc5d9c2095c3ea342/src/main/java/org/asciidoc/intellij/injection/LanguageGuesser.java
 */
public class LanguageGuesser {
    private static final Map<String, Language> langToLanguageMap = new HashMap<>();

    static {
        for (Language language : LanguageUtil.getInjectableLanguages()) {
            String languageInfoId = language.getID().toLowerCase(Locale.US).replace(" ", "");
            langToLanguageMap.put(languageInfoId, language);
        }

        associateIfAvailable("js", "javascript");
        associateIfAvailable("bash", "shellscript");
        associateIfAvailable("shell", "shellscript");
    }

    private LanguageGuesser() {
        // Private constructor to prevent instantiation
    }

    public static Language guessLanguage(String languageName) {
        if (languageName == null || languageName.trim().isEmpty()) {
            return null;
        }
        return langToLanguageMap.get(languageName.toLowerCase(Locale.US));
    }

    private static void associateIfAvailable(String newLanguageKey, String existingLanguageKey) {
        langToLanguageMap.computeIfAbsent(newLanguageKey, k -> langToLanguageMap.get(existingLanguageKey));
    }
}
