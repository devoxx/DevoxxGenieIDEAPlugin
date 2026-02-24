package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor
import kotlinx.coroutines.delay

@Composable
fun CopyButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography

    BasicText(
        text = if (copied) "\u2713 Copied" else "\u2398 Copy",
        style = typography.caption.copy(
            fontSize = 11.sp,
            color = if (copied) colors.primary else colors.onSurface.copy(alpha = 0.5f),
        ),
        modifier = modifier
            .padding(4.dp)
            .clickable {
                clipboardManager.setText(AnnotatedString(textToCopy))
                copied = true
            },
    )

    if (copied) {
        LaunchedEffect(Unit) {
            delay(2000)
            copied = false
        }
    }
}
