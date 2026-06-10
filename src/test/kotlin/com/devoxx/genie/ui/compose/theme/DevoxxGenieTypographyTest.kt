package com.devoxx.genie.ui.compose.theme

import androidx.compose.ui.unit.sp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DevoxxGenieTypographyTest {

    @Test
    fun `default scale keeps the configured font sizes unchanged`() {
        val typography = buildDevoxxTypography(bodyFontSize = 13, codeFontSize = 12)

        assertThat(typography.bodyFontSize).isEqualTo(13f)
        assertThat(typography.codeFontSize).isEqualTo(12f)
        assertThat(typography.scale).isEqualTo(1f)
        assertThat(typography.body1.fontSize).isEqualTo(13.sp)
        assertThat(typography.body2.fontSize).isEqualTo(12.sp)
        assertThat(typography.caption.fontSize).isEqualTo(11.sp)
        assertThat(typography.h5.fontSize).isEqualTo(24.sp)
    }

    @Test
    fun `ide zoom scale multiplies body code and derived sizes`() {
        val typography = buildDevoxxTypography(bodyFontSize = 13, codeFontSize = 12, ideScale = 1.5f)

        assertThat(typography.bodyFontSize).isEqualTo(13f * 1.5f)
        assertThat(typography.codeFontSize).isEqualTo(12f * 1.5f)
        assertThat(typography.scale).isEqualTo(1.5f)
        assertThat(typography.body1.fontSize).isEqualTo((13f * 1.5f).sp)
        assertThat(typography.body2.fontSize).isEqualTo((12f * 1.5f).sp)
        assertThat(typography.caption.fontSize).isEqualTo((11f * 1.5f).sp)
        assertThat(typography.h5.fontSize).isEqualTo((24f * 1.5f).sp)
    }

    @Test
    fun `zoom out shrinks sizes proportionally`() {
        val typography = buildDevoxxTypography(bodyFontSize = 13, codeFontSize = 12, ideScale = 0.8f)

        assertThat(typography.bodyFontSize).isEqualTo(13f * 0.8f)
        assertThat(typography.codeFontSize).isEqualTo(12f * 0.8f)
    }

    @Test
    fun `markdown heading offsets grow with the zoom scale`() {
        val typography = buildDevoxxTypography(bodyFontSize = 13, codeFontSize = 12, ideScale = 2f)

        // h1 in the bubbles = body + 9 * scale → the whole heading scales with zoom
        assertThat(typography.bodyPlus(9f)).isEqualTo((13f + 9f) * 2f)
    }

    @Test
    fun `base font sizes are clamped before scaling so zoom can exceed the clamp`() {
        val typography = buildDevoxxTypography(bodyFontSize = 99, codeFontSize = 1, ideScale = 1.5f)

        assertThat(typography.bodyFontSize).isEqualTo(24f * 1.5f)
        assertThat(typography.codeFontSize).isEqualTo(8f * 1.5f)
    }

    @Test
    fun `invalid scale values fall back to 1`() {
        assertThat(sanitizeIdeScale(Float.NaN)).isEqualTo(1f)
        assertThat(sanitizeIdeScale(Float.POSITIVE_INFINITY)).isEqualTo(1f)
        assertThat(sanitizeIdeScale(0f)).isEqualTo(1f)
        assertThat(sanitizeIdeScale(-2f)).isEqualTo(1f)
    }

    @Test
    fun `extreme but finite scales are clamped to a sane range`() {
        assertThat(sanitizeIdeScale(0.1f)).isEqualTo(0.5f)
        assertThat(sanitizeIdeScale(10f)).isEqualTo(3f)
        assertThat(sanitizeIdeScale(1.25f)).isEqualTo(1.25f)
    }
}
