package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.model.ActivityEntryUiModel
import com.devoxx.genie.ui.compose.model.ActivityStatus
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.model.TerminalState
import com.devoxx.genie.ui.compose.theme.*
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import kotlin.math.roundToInt

/**
 * Compact token formatter for the metadata row, mirroring WindowContextFormatterUtil
 * (e.g. 128000 -> "128K", 1_000_000 -> "1M"). Keeps the context indicator short.
 */
private fun formatTokens(tokens: Long): String = when {
    tokens >= 1_000_000_000 -> "${tokens / 1_000_000_000}B"
    tokens >= 1_000_000 -> "${tokens / 1_000_000}M"
    tokens >= 1_000 -> "${tokens / 1_000}K"
    else -> tokens.toString()
}

/**
 * Extracts the raw code text from a code fence or code block AST node.
 */
private fun extractCodeText(content: String, node: org.intellij.markdown.ast.ASTNode): String {
    // For code fences (```...```), collect CODE_FENCE_CONTENT and EOL children
    val codeLines = mutableListOf<String>()
    for (child in node.children) {
        when (child.type) {
            MarkdownTokenTypes.CODE_FENCE_CONTENT ->
                codeLines.add(content.substring(child.startOffset, child.endOffset))
            MarkdownTokenTypes.EOL ->
                if (codeLines.isNotEmpty()) codeLines.add("\n")
        }
    }
    if (codeLines.isNotEmpty()) {
        return codeLines.joinToString("").trimEnd()
    }

    // For indented code blocks, collect CODE_LINE children
    for (child in node.children) {
        if (child.type == MarkdownTokenTypes.CODE_LINE) {
            codeLines.add(content.substring(child.startOffset, child.endOffset))
        } else if (child.type == MarkdownTokenTypes.EOL) {
            if (codeLines.isNotEmpty()) codeLines.add("\n")
        }
    }
    if (codeLines.isNotEmpty()) {
        return codeLines.joinToString("").trimEnd()
    }

    // Fallback: use the full node text minus the fence markers
    val fullText = content.substring(node.startOffset, node.endOffset)
    return fullText
        .removePrefix("```").substringAfter('\n')
        .removeSuffix("```").trimEnd()
}

/**
 * Creates a theme-aware Highlights.Builder that uses the correct dark/light syntax theme
 * to ensure mark characters ({, }, (, ), etc.) are visible against the code background.
 */
private fun createHighlightsBuilder(isDark: Boolean): Highlights.Builder =
    Highlights.Builder().theme(SyntaxThemes.default(darkMode = isDark))

/**
 * Creates a custom code fence component with a copy button overlay.
 * Accepts [isDark] to select the appropriate syntax highlighting theme.
 */
private fun codeFenceWithCopy(isDark: Boolean): MarkdownComponent = { model ->
    val codeText = extractCodeText(model.content, model.node)
    Box(modifier = Modifier.fillMaxWidth()) {
        MarkdownHighlightedCodeFence(model.content, model.node, highlightsBuilder = createHighlightsBuilder(isDark))
        CopyButton(
            textToCopy = codeText,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

/**
 * Creates a custom code block component with a copy button overlay.
 * Accepts [isDark] to select the appropriate syntax highlighting theme.
 */
private fun codeBlockWithCopy(isDark: Boolean): MarkdownComponent = { model ->
    val codeText = extractCodeText(model.content, model.node)
    Box(modifier = Modifier.fillMaxWidth()) {
        MarkdownHighlightedCodeBlock(model.content, model.node, highlightsBuilder = createHighlightsBuilder(isDark))
        CopyButton(
            textToCopy = codeText,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

@Composable
fun AiBubble(
    message: MessageUiModel,
    modifier: Modifier = Modifier,
    onRetryClick: (String) -> Unit = {},
    onOpenAgentSettings: () -> Unit = {},
) {
    val colors = DevoxxGenieThemeAccessor.colors
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, DevoxxBlue, shape)
            .background(colors.assistantBubbleBackground, shape)
            .padding(12.dp),
    ) {
        // Metadata row
        if (message.modelName.isNotBlank() || message.executionTimeMs > 0) {
            MetadataRow(message)
            Spacer(Modifier.height(8.dp))
        }

        // Loading indicator
        if (message.isLoadingIndicatorVisible && message.aiResponseMarkdown.isBlank()) {
            ThinkingIndicator()
        }

        // Always-on live agent status: while the run is in flight, surface the most
        // recent open tool call ("Running search_files… (step 4/25)") regardless of the
        // "Show tool activity in chat output" setting — without this, a multi-minute
        // agent run with that setting off is completely silent.
        if (message.isLoadingIndicatorVisible || message.isStreaming) {
            val openEntry = message.activityEntries.lastOrNull {
                it.status == ActivityStatus.RUNNING || it.status == ActivityStatus.PENDING_APPROVAL
            }
            if (openEntry != null) {
                LiveStatusLine(openEntry)
            }
        }

        // AI Response content — rendered as Markdown
        if (message.aiResponseMarkdown.isNotBlank()) {
            val typography = DevoxxGenieThemeAccessor.typography
            val textColor = colors.textPrimary
            val secondaryColor = colors.textSecondary
            val codeBg = colors.codeBackground
            val bodySize = typography.bodyFontSize.sp
            val codeSize = typography.codeFontSize.sp

            // markdown-renderer 0.38+: text colors moved into typography TextStyles;
            // link styling moved to MarkdownTypography.textLink.
            val mdColors = DefaultMarkdownColors(
                text = textColor,
                codeBackground = codeBg,
                inlineCodeBackground = codeBg,
                dividerColor = secondaryColor,
                tableBackground = Color.Transparent,
            )

            val baseStyle = TextStyle(fontSize = bodySize, color = textColor)
            val codeStyle = TextStyle(fontSize = codeSize, fontFamily = FontFamily.Monospace, color = textColor)

            val mdTypography = DefaultMarkdownTypography(
                h1 = baseStyle.copy(fontSize = typography.bodyPlus(9f).sp, fontWeight = FontWeight.Bold),
                h2 = baseStyle.copy(fontSize = typography.bodyPlus(7f).sp, fontWeight = FontWeight.Bold),
                h3 = baseStyle.copy(fontSize = typography.bodyPlus(5f).sp, fontWeight = FontWeight.Bold),
                h4 = baseStyle.copy(fontSize = typography.bodyPlus(3f).sp, fontWeight = FontWeight.SemiBold),
                h5 = baseStyle.copy(fontSize = typography.bodyPlus(1f).sp, fontWeight = FontWeight.SemiBold),
                h6 = baseStyle.copy(fontSize = bodySize, fontWeight = FontWeight.SemiBold),
                text = baseStyle,
                code = codeStyle,
                inlineCode = codeStyle.copy(color = DevoxxBlue),
                quote = baseStyle.copy(color = secondaryColor),
                paragraph = baseStyle,
                ordered = baseStyle,
                bullet = baseStyle,
                list = baseStyle,
                textLink = TextLinkStyles(style = SpanStyle(color = DevoxxBlue)),
                table = baseStyle,
            )

            SelectionContainer {
                Markdown(
                    content = message.aiResponseMarkdown,
                    colors = mdColors,
                    typography = mdTypography,
                    components = markdownComponents(
                        codeBlock = codeBlockWithCopy(colors.isDark),
                        codeFence = codeFenceWithCopy(colors.isDark),
                    ),
                    // Keep the previously rendered content visible while the updated
                    // markdown is re-parsed. Without this every streaming update swaps
                    // the bubble to a blank loading state first — full-text flicker.
                    retainState = true,
                )
            }

            // In-flight affordance: once the first token replaces the ThinkingIndicator,
            // this caret is the only signal that the response is still streaming.
            if (message.isStreaming) {
                StreamingCaret()
            }
        }

        // Terminal-state markers: durable in-chat feedback for abnormal completion.
        when (message.terminalState) {
            TerminalState.STOPPED -> StoppedFooter(hasContent = message.aiResponseMarkdown.isNotBlank())
            TerminalState.ERROR -> ErrorCard(
                errorText = message.errorText,
                retryAttempted = message.retryAttempted,
                onRetryClick = { onRetryClick(message.id) },
            )
            TerminalState.LOOP_LIMIT -> LoopLimitNotice(
                maxCalls = message.loopLimitMaxCalls,
                onOpenAgentSettings = onOpenAgentSettings,
            )
            TerminalState.COMPLETED -> { /* normal completion — no marker */ }
        }

        // Copy button
        if (message.aiResponseMarkdown.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CopyButton(textToCopy = message.aiResponseMarkdown)
            }
        }
    }
}

/**
 * One-line live agent status shown only while the run is active. RUNNING gets a
 * pulsing dot and "Running <tool>… (step n/max)"; PENDING_APPROVAL explains why
 * everything is frozen while the approval dialog (120s timeout) is up.
 */
@Composable
private fun LiveStatusLine(entry: ActivityEntryUiModel) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        RunningSpinner()
        Spacer(Modifier.width(6.dp))
        val text = if (entry.status == ActivityStatus.PENDING_APPROVAL) {
            "Waiting for your approval… (${entry.toolName ?: "tool"})"
        } else {
            val step = if (entry.callNumber > 0 && entry.maxCalls > 0) {
                " (step ${entry.callNumber}/${entry.maxCalls})"
            } else {
                ""
            }
            "Running ${entry.toolName ?: "tool"}…$step"
        }
        BasicText(
            text = text,
            style = typography.caption.copy(color = colors.textSecondary),
        )
    }
}

@Composable
private fun MetadataRow(message: MessageUiModel) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.modelName.isNotBlank()) {
            BasicText(
                text = message.modelName,
                style = typography.caption.copy(color = colors.textSecondary),
            )
        }

        val parts = mutableListOf<String>()
        if (message.executionTimeMs > 0) {
            val seconds = message.executionTimeMs / 1000.0
            parts.add("%.1fs".format(seconds))
        }
        val usage = message.tokenUsage
        if (usage.inputTokens > 0 || usage.outputTokens > 0) {
            parts.add("${usage.inputTokens}/${usage.outputTokens} tokens")
        }
        if (usage.cost > 0) {
            parts.add("$%.4f".format(usage.cost))
        }
        // Used window context: how much of the model's input window this exchange occupied.
        if (usage.contextWindowMax > 0 && (usage.inputTokens > 0 || usage.outputTokens > 0)) {
            val used = usage.inputTokens + usage.outputTokens
            val pct = (used.toDouble() / usage.contextWindowMax * 100).roundToInt()
            parts.add("${formatTokens(used)}/${formatTokens(usage.contextWindowMax)} context ($pct%)")
        }

        if (parts.isNotEmpty()) {
            BasicText(
                text = parts.joinToString(" | "),
                style = typography.caption.copy(color = colors.textSecondary),
            )
        }
    }
}

/**
 * Muted footer rendered when the user stopped the response mid-stream. Whatever
 * partial text was kept stays visible above it.
 */
@Composable
private fun StoppedFooter(hasContent: Boolean) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    if (hasContent) {
        Spacer(Modifier.height(8.dp))
    }
    BasicText(
        text = "\u23F9 Stopped by user",
        style = typography.caption.copy(color = colors.textSecondary),
    )
}

/**
 * Compact inline error card: red-tinted background, human-readable error summary and
 * a Retry affordance. The card is part of the message model, so it persists across
 * scrolling and is not a transient overlay. Retry is one-shot: [retryAttempted]
 * disables the affordance after the first click (double-submission guard).
 */
@Composable
private fun ErrorCard(
    errorText: String?,
    retryAttempted: Boolean,
    onRetryClick: () -> Unit,
) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    val errorRed = Color(0xFFE53935)
    val shape = RoundedCornerShape(6.dp)

    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, errorRed.copy(alpha = 0.6f), shape)
            .background(errorRed.copy(alpha = if (colors.isDark) 0.15f else 0.08f), shape)
            .padding(10.dp),
    ) {
        BasicText(
            text = "\u26A0 Request failed",
            style = typography.caption.copy(color = errorRed, fontWeight = FontWeight.Bold),
        )
        if (!errorText.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = errorText,
                style = typography.caption.copy(color = colors.textPrimary),
            )
        }
        Spacer(Modifier.height(6.dp))
        val retryLabel = if (retryAttempted) "\u21BB Retried" else "\u21BB Retry"
        val retryColor = if (retryAttempted) colors.textSecondary else DevoxxBlue
        BasicText(
            text = retryLabel,
            style = typography.caption.copy(color = retryColor, fontWeight = FontWeight.SemiBold),
            modifier = if (retryAttempted) {
                Modifier.padding(2.dp) // disabled after first click — one retry per card
            } else {
                Modifier.padding(2.dp).clickable(onClick = onRetryClick)
            },
        )
    }
}

/**
 * Notice rendered when the agent hit its configured tool-call limit, with an
 * affordance that opens Settings → Agent Mode.
 */
@Composable
private fun LoopLimitNotice(
    maxCalls: Int,
    onOpenAgentSettings: () -> Unit,
) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    val warnOrange = DevoxxOrange
    val shape = RoundedCornerShape(6.dp)

    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, warnOrange.copy(alpha = 0.6f), shape)
            .background(warnOrange.copy(alpha = if (colors.isDark) 0.12f else 0.08f), shape)
            .padding(10.dp),
    ) {
        val limitText = if (maxCalls > 0) "Reached max tool calls ($maxCalls)" else "Reached max tool calls"
        BasicText(
            text = "\u26A0 $limitText — you can raise the limit in Settings → Agent",
            style = typography.caption.copy(color = colors.textPrimary),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = "Open Agent settings",
            style = typography.caption.copy(
                color = DevoxxBlue,
                textDecoration = TextDecoration.Underline,
            ),
            modifier = Modifier.padding(2.dp).clickable(onClick = onOpenAgentSettings),
        )
    }
}
