package com.devoxx.genie.ui.compose.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devoxx.genie.ui.compose.model.CustomPromptUi
import com.devoxx.genie.ui.compose.model.FEATURES
import com.devoxx.genie.ui.compose.model.FeatureDoc
import com.devoxx.genie.ui.compose.theme.*
import com.devoxx.genie.ui.compose.util.fireTrackingPixel
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil
import com.intellij.ide.BrowserUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ResourceBundle

@Composable
fun WelcomeScreen(
    resourceBundle: ResourceBundle,
    customPrompts: List<CustomPromptUi>,
    onCustomPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { fireTrackingPixel() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))

        // DevoxxGenie logo
        val density = LocalDensity.current
        val iconPath = if (colors.isDark) "/icons/pluginIcon_dark.svg" else "/icons/pluginIcon.svg"
        val logoPainter = remember(density, iconPath) {
            DevoxxGenieIconsUtil::class.java.getResourceAsStream(iconPath)
                ?.use { loadSvgPainter(it, density) }
        }
        if (logoPainter != null) {
            Image(
                painter = logoPainter,
                contentDescription = "DevoxxGenie",
                modifier = Modifier.height(80.dp),
            )
            Spacer(Modifier.height(12.dp))
        }

        // Greeting
        val greeting = remember {
            when (java.time.LocalTime.now().hour) {
                in 5..11 -> "Good morning"
                in 12..17 -> "Good afternoon"
                in 18..21 -> "Good evening"
                else -> "Good night"
            }
        }

        BasicText(
            text = "$greeting!",
            style = typography.body1.copy(color = colors.textSecondary),
        )

        Spacer(Modifier.height(4.dp))

        BasicText(
            text = "Welcome to DevoxxGenie",
            style = typography.h5.copy(
                fontWeight = FontWeight.Bold,
                color = DevoxxOrange,
            ),
        )

        Spacer(Modifier.height(4.dp))

        BasicText(
            text = "Your AI Code Assistant",
            style = typography.body1.copy(color = colors.textSecondary),
        )

        Spacer(Modifier.height(20.dp))

        // Features section
        SectionHeader("Explore Features")
        Spacer(Modifier.height(8.dp))
        FeatureChipGrid()

        Spacer(Modifier.height(20.dp))

        // Commands section
        if (customPrompts.isNotEmpty()) {
            SectionHeader("Quick Commands")
            Spacer(Modifier.height(8.dp))
            CommandsList(customPrompts, onCustomPromptClick)
        }

        Spacer(Modifier.height(20.dp))

        // Footer
        FooterLinks()

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    BasicText(
        text = title,
        style = typography.body2.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            color = colors.textSecondary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FeatureChipGrid() {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    // Derive chip colors from the theme surface — slightly offset for contrast
    val chipBg = colors.surface.blendWith(colors.onSurface, 0.06f)
    val chipBorder = colors.surface.blendWith(colors.onSurface, 0.14f)
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        FEATURES.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                row.forEach { feature ->
                    FeatureChip(
                        feature = feature,
                        typography = typography,
                        textColor = colors.textPrimary,
                        bgColor = chipBg,
                        borderColor = chipBorder,
                        shape = shape,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeatureChip(
    feature: FeatureDoc,
    typography: DevoxxTypography,
    textColor: Color,
    bgColor: Color,
    borderColor: Color,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val tooltipShape = RoundedCornerShape(4.dp)

    TooltipArea(
        tooltip = {
            Box(
                modifier = Modifier
                    .shadow(4.dp, tooltipShape)
                    .background(Color(0xFF2B2D30), tooltipShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                BasicText(
                    text = feature.tooltip,
                    style = typography.caption.copy(
                        fontSize = 11.sp,
                        color = Color.White,
                    ),
                )
            }
        },
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bgColor, shape)
                .border(1.dp, borderColor, shape)
                .clickable { BrowserUtil.browse(feature.url) }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = feature.emoji,
                style = typography.body2.copy(fontSize = 13.sp),
            )
            Spacer(Modifier.width(7.dp))
            BasicText(
                text = feature.name,
                style = typography.body2.copy(color = textColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommandsList(
    customPrompts: List<CustomPromptUi>,
    onCustomPromptClick: (String) -> Unit,
) {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    val cardBg = colors.surface.blendWith(colors.onSurface, 0.04f)
    val cardBorder = colors.surface.blendWith(colors.onSurface, 0.12f)
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, shape)
            .border(1.dp, cardBorder, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        customPrompts.forEach { prompt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onCustomPromptClick("/${prompt.name}") }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(vertical = 3.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "/${prompt.name}",
                    style = typography.body2.copy(
                        fontWeight = FontWeight.Bold,
                        color = DevoxxBlue,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                BasicText(
                    text = prompt.prompt,
                    style = typography.body2.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FooterLinks() {
    val colors = DevoxxGenieThemeAccessor.colors
    val typography = DevoxxGenieThemeAccessor.typography
    val linkColor = DevoxxBlue.copy(alpha = 0.85f)
    val linkStyle = typography.caption.copy(fontSize = 11.sp, color = linkColor)
    val dotStyle = typography.caption.copy(
        fontSize = 11.sp,
        color = colors.textSecondary.copy(alpha = 0.5f),
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FooterLink("GitHub", "https://github.com/devoxx/DevoxxGenieIDEAPlugin", linkStyle)
        BasicText(text = "  \u00B7  ", style = dotStyle)
        FooterLink("X/Twitter", "https://x.com/DevoxxGenie", linkStyle)
        BasicText(text = "  \u00B7  ", style = dotStyle)
        FooterLink("Bluesky", "https://bsky.app/profile/devoxxgenie.bsky.social", linkStyle)
        BasicText(text = "  \u00B7  ", style = dotStyle)
        FooterLink("Rate Plugin \u2764\uFE0F", "https://plugins.jetbrains.com/plugin/24169-devoxxgenie/reviews", linkStyle)
    }
}

@Composable
private fun FooterLink(label: String, url: String, style: androidx.compose.ui.text.TextStyle) {
    BasicText(
        text = label,
        style = style,
        modifier = Modifier
            .clickable { BrowserUtil.browse(url) }
            .pointerHoverIcon(PointerIcon.Hand),
    )
}

/** Blend [this] color toward [other] by the given [fraction] (0..1). */
private fun Color.blendWith(other: Color, fraction: Float): Color = Color(
    red = red + (other.red - red) * fraction,
    green = green + (other.green - green) * fraction,
    blue = blue + (other.blue - blue) * fraction,
    alpha = alpha,
)
