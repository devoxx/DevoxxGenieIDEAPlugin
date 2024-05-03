package com.devoxx.genie.ui.util

import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import java.util.*

/**
 * Guesses the available IntelliJ [Language] of a fenced code block based on the language name.
 *
 * [Language.findLanguageByID] is not used it returns [Language.ANY] for unknown/undefined languages
 * which makes [com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet]
 * fails.
 *
 * Also, it is pretty common for markdown to use language identifier that not always match
 * the language id used by IntelliJ.
 *
 * Inspired by https://github.com/asciidoctor/asciidoctor-intellij-plugin/blob/3ac99c53b21bc5d0ecb4961dc5d9c2095c3ea342/src/main/java/org/asciidoc/intellij/injection/LanguageGuesser.java
 */
object LanguageGuesser {
    private val langToLanguageMap: Map<String, Language> = buildMap {
        for (language in LanguageUtil.getInjectableLanguages()) {
            val languageInfoId = language.id.lowercase(Locale.US).replace(" ", "")
            this[languageInfoId] = language
        }

        associateIfAvailable("js", "javascript")
        associateIfAvailable("bash", "shellscript")
        associateIfAvailable("shell", "shellscript")
    }

    fun guessLanguage(languageName: String?): Language? {
        if (languageName.isNullOrBlank()) {
            return null
        }
        return langToLanguageMap[languageName.lowercase(Locale.US)]
    }

    private fun MutableMap<String, Language>.associateIfAvailable(newLanguageKey: String, existingLanguageKey: String) {
        @Suppress("UNCHECKED_CAST")
        (this as MutableMap<String, Language?>).computeIfAbsent(newLanguageKey) { this[existingLanguageKey] }
    }
}
