package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor
import com.devoxx.genie.ui.compose.theme.DevoxxOrange
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography

@Composable
fun UserBubble(
    promptText: String,
    modifier: Modifier = Modifier,
) {
    if (promptText.isBlank()) return

    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    val shape = RoundedCornerShape(8.dp)

    val textColor = colors.textPrimary
    val secondaryColor = colors.textSecondary
    val codeBg = colors.codeBackground
    val bodySize = typography.bodyFontSize.sp
    val codeSize = typography.codeFontSize.sp

    val mdColors = DefaultMarkdownColors(
        text = textColor,
        codeText = textColor,
        inlineCodeText = DevoxxOrange,
        linkText = DevoxxOrange,
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
        inlineCode = codeStyle.copy(color = DevoxxOrange),
        quote = baseStyle.copy(color = secondaryColor),
        paragraph = baseStyle,
        ordered = baseStyle,
        bullet = baseStyle,
        list = baseStyle,
        link = baseStyle.copy(color = DevoxxOrange),
    )

    val codeFence: com.mikepenz.markdown.compose.components.MarkdownComponent = { model ->
        Box(modifier = Modifier.fillMaxWidth()) {
            MarkdownHighlightedCodeFence(model.content, model.node)
        }
    }

    val codeBlock: com.mikepenz.markdown.compose.components.MarkdownComponent = { model ->
        Box(modifier = Modifier.fillMaxWidth()) {
            MarkdownHighlightedCodeBlock(model.content, model.node)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, DevoxxOrange, shape)
            .background(colors.userBubbleBackground, shape)
            .padding(12.dp),
    ) {
        SelectionContainer {
            Markdown(
                content = promptText,
                colors = mdColors,
                typography = mdTypography,
                components = markdownComponents(
                    codeBlock = codeBlock,
                    codeFence = codeFence,
                ),
            )
        }
    }
}
