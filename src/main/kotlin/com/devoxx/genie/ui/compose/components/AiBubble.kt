package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.model.MessageUiModel
import com.devoxx.genie.ui.compose.theme.*
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes

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
 * Custom code fence component with a copy button overlay.
 */
private val codeFenceWithCopy: MarkdownComponent = { model ->
    val codeText = extractCodeText(model.content, model.node)
    Box(modifier = Modifier.fillMaxWidth()) {
        MarkdownHighlightedCodeFence(model.content, model.node)
        CopyButton(
            textToCopy = codeText,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

/**
 * Custom code block component with a copy button overlay.
 */
private val codeBlockWithCopy: MarkdownComponent = { model ->
    val codeText = extractCodeText(model.content, model.node)
    Box(modifier = Modifier.fillMaxWidth()) {
        MarkdownHighlightedCodeBlock(model.content, model.node)
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

        // AI Response content — rendered as Markdown
        if (message.aiResponseMarkdown.isNotBlank()) {
            val typography = DevoxxGenieThemeAccessor.typography
            val textColor = colors.textPrimary
            val secondaryColor = colors.textSecondary
            val codeBg = colors.codeBackground
            val bodySize = typography.bodyFontSize.sp
            val codeSize = typography.codeFontSize.sp

            val mdColors = DefaultMarkdownColors(
                text = textColor,
                codeText = textColor,
                inlineCodeText = DevoxxBlue,
                linkText = DevoxxBlue,
                codeBackground = codeBg,
                inlineCodeBackground = codeBg,
                dividerColor = secondaryColor,
                tableText = textColor,
                tableBackground = Color.Transparent,
            )

            val baseStyle = TextStyle(fontSize = bodySize, color = textColor)
            val codeStyle = TextStyle(fontSize = codeSize, fontFamily = FontFamily.Monospace, color = textColor)

            val mdTypography = DefaultMarkdownTypography(
                h1 = baseStyle.copy(fontSize = (typography.bodyFontSize + 9).sp, fontWeight = FontWeight.Bold),
                h2 = baseStyle.copy(fontSize = (typography.bodyFontSize + 7).sp, fontWeight = FontWeight.Bold),
                h3 = baseStyle.copy(fontSize = (typography.bodyFontSize + 5).sp, fontWeight = FontWeight.Bold),
                h4 = baseStyle.copy(fontSize = (typography.bodyFontSize + 3).sp, fontWeight = FontWeight.SemiBold),
                h5 = baseStyle.copy(fontSize = (typography.bodyFontSize + 1).sp, fontWeight = FontWeight.SemiBold),
                h6 = baseStyle.copy(fontSize = bodySize, fontWeight = FontWeight.SemiBold),
                text = baseStyle,
                code = codeStyle,
                inlineCode = codeStyle.copy(color = DevoxxBlue),
                quote = baseStyle.copy(color = secondaryColor),
                paragraph = baseStyle,
                ordered = baseStyle,
                bullet = baseStyle,
                list = baseStyle,
                link = baseStyle.copy(color = DevoxxBlue),
            )

            SelectionContainer {
                Markdown(
                    content = message.aiResponseMarkdown,
                    colors = mdColors,
                    typography = mdTypography,
                    components = markdownComponents(
                        codeBlock = codeBlockWithCopy,
                        codeFence = codeFenceWithCopy,
                    ),
                )
            }
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

        if (parts.isNotEmpty()) {
            BasicText(
                text = parts.joinToString(" | "),
                style = typography.caption.copy(color = colors.textSecondary),
            )
        }
    }
}
