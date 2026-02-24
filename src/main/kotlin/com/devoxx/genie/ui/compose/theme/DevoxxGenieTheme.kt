package com.devoxx.genie.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.intellij.util.ui.UIUtil

// Devoxx brand colours
val DevoxxOrange = Color(0xFFFF5400)
val DevoxxBlue = Color(0xFF0095C9)

/**
 * Custom color scheme replacing Material Colors to avoid classloader conflicts.
 */
@Stable
data class DevoxxColors(
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val onSurface: Color,
    val userBubbleBackground: Color,
    val assistantBubbleBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val codeBackground: Color,
)

/**
 * Custom typography replacing Material Typography.
 */
@Stable
data class DevoxxTypography(
    val h5: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal),
    val body1: TextStyle = TextStyle(fontSize = 13.sp),
    val body2: TextStyle = TextStyle(fontSize = 12.sp),
    val caption: TextStyle = TextStyle(fontSize = 11.sp),
)

val LocalDevoxxColors = staticCompositionLocalOf<DevoxxColors> {
    error("No DevoxxColors provided")
}

val LocalDevoxxTypography = staticCompositionLocalOf<DevoxxTypography> {
    error("No DevoxxTypography provided")
}

/**
 * Convenience accessor mirroring MaterialTheme API.
 */
object DevoxxGenieThemeAccessor {
    val colors: DevoxxColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDevoxxColors.current

    val typography: DevoxxTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalDevoxxTypography.current
}

object DevoxxGenieColors {
    @Composable
    fun userBubbleBackground(dark: Boolean): Color = if (dark) Color(0xFF2A2520) else Color(0xFFFFF9F0)

    @Composable
    fun assistantBubbleBackground(dark: Boolean): Color = if (dark) Color(0xFF1E282E) else Color(0xFFF0F7FF)

    @Composable
    fun textPrimary(dark: Boolean): Color = if (dark) Color(0xFFE0E0E0) else Color(0xFF000000)

    @Composable
    fun textSecondary(dark: Boolean): Color = if (dark) Color(0xFFA0A0A0) else Color(0xFF666666)

    @Composable
    fun codeBackground(dark: Boolean): Color = if (dark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)

    @Composable
    fun surfaceBackground(dark: Boolean): Color {
        return try {
            val bg = UIUtil.getPanelBackground()
            Color(bg.red, bg.green, bg.blue)
        } catch (_: Exception) {
            if (dark) Color(0xFF2B2D30) else Color(0xFFF7F8FA)
        }
    }
}

@Composable
fun DevoxxGenieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = DevoxxColors(
        primary = DevoxxOrange,
        secondary = DevoxxBlue,
        surface = DevoxxGenieColors.surfaceBackground(darkTheme),
        onSurface = DevoxxGenieColors.textPrimary(darkTheme),
        userBubbleBackground = DevoxxGenieColors.userBubbleBackground(darkTheme),
        assistantBubbleBackground = DevoxxGenieColors.assistantBubbleBackground(darkTheme),
        textPrimary = DevoxxGenieColors.textPrimary(darkTheme),
        textSecondary = DevoxxGenieColors.textSecondary(darkTheme),
        codeBackground = DevoxxGenieColors.codeBackground(darkTheme),
    )

    val typography = DevoxxTypography()

    CompositionLocalProvider(
        LocalDevoxxColors provides colors,
        LocalDevoxxTypography provides typography,
        content = content,
    )
}
