package com.devoxx.genie.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.theme.DevoxxGenieThemeAccessor
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CopyButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
    label: String = "\u2398 Copy",
    copiedLabel: String = "\u2713 Copied",
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography

    BasicText(
        text = if (copied) copiedLabel else label,
        style = typography.caption.copy(
            color = if (copied) colors.primary else colors.onSurface.copy(alpha = 0.5f),
        ),
        modifier = modifier
            .padding(4.dp)
            .clickable {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(StringSelection(textToCopy)))
                }
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
