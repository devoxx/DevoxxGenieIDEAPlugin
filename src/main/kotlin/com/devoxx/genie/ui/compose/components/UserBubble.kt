package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor
import com.devoxx.genie.ui.compose.theme.DevoxxOrange

@Composable
fun UserBubble(
    promptText: String,
    modifier: Modifier = Modifier,
) {
    if (promptText.isBlank()) return

    val colors = DevoxxGenieThemeAccessor.colors
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, DevoxxOrange, shape)
            .background(colors.userBubbleBackground, shape)
            .padding(12.dp),
    ) {
        BasicText(
            text = promptText,
            style = DevoxxGenieThemeAccessor.typography.body1.copy(
                color = colors.textPrimary,
            ),
        )
    }
}
