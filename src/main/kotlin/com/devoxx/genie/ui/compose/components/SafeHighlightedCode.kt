package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownDimens
import com.mikepenz.markdown.compose.LocalMarkdownPadding
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.elements.MarkdownCodeBackground
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.compose.elements.material.MarkdownBasicText
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.intellij.markdown.ast.ASTNode

/**
 * Syntax-highlighted code blocks that survive malformed highlight ranges.
 *
 * This mirrors `MarkdownHighlightedCodeFence` / `MarkdownHighlightedCodeBlock` from
 * multiplatform-markdown-renderer, with one difference: highlight locations are validated
 * before they reach [AnnotatedString.Builder.addStyle].
 *
 * The upstream highlighter (`dev.snipme:highlights`) pairs the Nth multiline-comment opener
 * with the Nth terminator positionally, so a snippet containing a terminator before the first
 * opener — an LLM streaming a Javadoc excerpt or a diff hunk, routinely — yields a location
 * whose end precedes its start. `addStyle` rejects that with "Reversed range is not supported",
 * thrown from a background coroutine where nothing catches it, which tears down the Compose
 * frame clock and stops the whole conversation panel from rendering (TASK-258).
 */

/**
 * Applies [highlights] to [code], discarding any location that does not describe a non-empty
 * range within the text. Out-of-bounds ends are clamped; reversed, empty and fully out-of-range
 * locations are dropped.
 */
internal fun buildSafeHighlightedAnnotatedString(
    code: String,
    highlights: List<CodeHighlight>,
): AnnotatedString = buildAnnotatedString {
    append(code)
    highlights.forEach { highlight ->
        val start = highlight.location.start.coerceIn(0, code.length)
        val end = highlight.location.end.coerceIn(0, code.length)
        if (end <= start) return@forEach

        val style = when (highlight) {
            is ColorHighlight -> SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f))
            is BoldHighlight -> SpanStyle(fontWeight = FontWeight.Bold)
        }
        addStyle(style = style, start = start, end = end)
    }
}

/**
 * Runs [highlightsProvider] and applies its result to [code], falling back to unstyled code if
 * the highlighter itself fails. A future defect in a locator then costs this one code block its
 * colours instead of the entire panel.
 */
internal fun buildSafeHighlightedAnnotatedString(
    code: String,
    highlightsProvider: () -> List<CodeHighlight>,
): AnnotatedString = try {
    buildSafeHighlightedAnnotatedString(code, highlightsProvider())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    AnnotatedString(code)
}

@Composable
fun SafeMarkdownHighlightedCodeFence(
    content: String,
    node: ASTNode,
    style: TextStyle = LocalMarkdownTypography.current.code,
    highlightsBuilder: Highlights.Builder,
    showHeader: Boolean = false,
) {
    MarkdownCodeFence(content, node, style) { code, language, codeStyle ->
        SafeMarkdownHighlightedCode(code, language, codeStyle, highlightsBuilder, showHeader)
    }
}

@Composable
fun SafeMarkdownHighlightedCodeBlock(
    content: String,
    node: ASTNode,
    style: TextStyle = LocalMarkdownTypography.current.code,
    highlightsBuilder: Highlights.Builder,
    showHeader: Boolean = false,
) {
    MarkdownCodeBlock(content, node, style) { code, language, codeStyle ->
        SafeMarkdownHighlightedCode(code, language, codeStyle, highlightsBuilder, showHeader)
    }
}

@Composable
private fun SafeMarkdownHighlightedCode(
    code: String,
    language: String?,
    style: TextStyle,
    highlightsBuilder: Highlights.Builder,
    showHeader: Boolean,
) {
    val backgroundCodeColor = LocalMarkdownColors.current.codeBackground
    val codeBackgroundCornerSize = LocalMarkdownDimens.current.codeBackgroundCornerSize
    val codeBlockPadding = LocalMarkdownPadding.current.codeBlock
    val codeHighlights: AnnotatedString by produceSafeHighlightsState(code, language, highlightsBuilder)

    MarkdownCodeBackground(
        color = backgroundCodeColor,
        shape = RoundedCornerShape(codeBackgroundCornerSize),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        showHeader = showHeader,
        language = language,
        code = code,
    ) {
        MarkdownBasicText(
            text = codeHighlights,
            style = style,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(codeBlockPadding),
        )
    }
}

@Composable
private fun produceSafeHighlightsState(
    code: String,
    language: String?,
    highlightsBuilder: Highlights.Builder,
): State<AnnotatedString> = produceState(
    initialValue = AnnotatedString(text = code),
    key1 = code,
) {
    val syntaxLanguage = language?.let { SyntaxLanguage.getByName(it) }
    val job = launch(Dispatchers.Default) {
        value = buildSafeHighlightedAnnotatedString(code) {
            highlightsBuilder
                .code(code)
                .let { if (syntaxLanguage != null) it.language(syntaxLanguage) else it }
                .build()
                .getHighlights()
        }
    }
    awaitDispose {
        job.cancel()
    }
}
