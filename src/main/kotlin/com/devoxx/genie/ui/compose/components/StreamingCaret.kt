package com.devoxx.genie.ui.compose.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.devoxx.genie.ui.compose.theme.DevoxxBlue

/**
 * Blinking caret shown at the tail of an AI bubble while the response is still
 * streaming. Rendered as a separate composable after the Markdown content — never
 * appended to the model text, so it cannot leak into chat memory or persistence.
 */
@Composable
fun StreamingCaret(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val caretAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Box(
        modifier = modifier
            .padding(top = 2.dp)
            .size(width = 8.dp, height = 14.dp)
            .alpha(caretAlpha)
            .background(DevoxxBlue),
    )
}
