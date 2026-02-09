package com.devoxx.genie.completion

import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestionUpdateManager

/**
 * Extends [InlineCompletionSuggestionUpdateManager.Default] to get built-in
 * partial-accept support (insert next word / insert next line) for free.
 */
class DevoxxGenieSuggestionUpdateManager : InlineCompletionSuggestionUpdateManager.Default()
