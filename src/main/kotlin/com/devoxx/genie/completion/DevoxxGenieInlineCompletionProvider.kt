package com.devoxx.genie.completion

import com.devoxx.genie.ui.settings.DevoxxGenieStateService
import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionSkipTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestionUpdateManager
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * IntelliJ InlineCompletionProvider that provides ghost text suggestions
 * using Ollama Fill-in-the-Middle (FIM) models.
 *
 * Requires IntelliJ 2024.3+ (debounced inline completion API).
 */
class DevoxxGenieInlineCompletionProvider : DebouncedInlineCompletionProvider() {

    override val id: InlineCompletionProviderID
        get() = InlineCompletionProviderID("DevoxxGenieInlineCompletion")

    override val suggestionUpdateManager: InlineCompletionSuggestionUpdateManager
        get() = DevoxxGenieSuggestionUpdateManager()

    override suspend fun getDebounceDelay(request: InlineCompletionRequest): Duration {
        val ms = DevoxxGenieStateService.getInstance().inlineCompletionDebounceMs ?: 300
        return ms.milliseconds
    }

    override fun isEnabled(event: InlineCompletionEvent): Boolean {
        // EDT — fast checks only
        if (event !is InlineCompletionEvent.DocumentChange) return false

        val state = DevoxxGenieStateService.getInstance()
        if (state.inlineCompletionProvider.isNullOrBlank()) return false
        val model = state.inlineCompletionModel
        if (model.isNullOrBlank()) return false

        val editor = event.editor
        if (!isEditorSuitable(editor)) return false

        return true
    }

    override suspend fun getSuggestionDebounced(request: InlineCompletionRequest): InlineCompletionSuggestion {
        val project = request.editor.project
        if (project == null || project.isDisposed) return InlineCompletionSuggestion.Empty
        if (request.editor.isDisposed) return InlineCompletionSuggestion.Empty

        val document = request.document
        val offset = request.startOffset

        val completionText = withContext(Dispatchers.IO) {
            val context = EditorContextExtractor.extract(document, offset)
            InlineCompletionService.getInstance()
                .getCompletion(context.prefix, context.suffix)
        }

        if (completionText.isNullOrEmpty()) {
            return InlineCompletionSuggestion.Empty
        }

        // Extract the suffix on the current line (text after cursor to end of line)
        val lineNumber = document.getLineNumber(offset)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineSuffix = document.getText(com.intellij.openapi.util.TextRange(offset, lineEnd))

        val processed = CompletionPostProcessor.process(completionText, lineSuffix)
        if (processed.isEmpty) {
            return InlineCompletionSuggestion.Empty
        }

        return InlineCompletionSingleSuggestion.build {
            for (element in processed.elements()) {
                when (element) {
                    is CompletionPostProcessor.GrayText ->
                        emit(InlineCompletionGrayTextElement(element.text()))

                    is CompletionPostProcessor.SkipText ->
                        emit(InlineCompletionSkipTextElement(element.text()))
                }
            }
        }
    }

    private fun isEditorSuitable(editor: Editor): Boolean {
        if (editor.isViewer) return false

        val document = editor.document
        if (!document.isWritable) return false

        val project = editor.project ?: return false
        val virtualFile = editor.virtualFile ?: return true // allow unsaved scratch files

        if (virtualFile.fileType.isBinary) return false
        if (virtualFile.length > MAX_FILE_SIZE) return false

        // Don't show inline completions when code completion popup is visible
        if (LookupManager.getInstance(project).activeLookup != null) return false

        return true
    }

    companion object {
        private const val MAX_FILE_SIZE = 500_000L
    }
}
