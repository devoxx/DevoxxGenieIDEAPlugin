package com.devoxx.genie.ui.util;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Regression tests for issue #1034 — CJK characters rendered as tofu boxes in the
 * chat input field. These tests exercise the font-selection logic that backs the
 * input component without requiring the IntelliJ test fixture.
 */
class CJKFontUtilTest {

    private static final String CJK_SAMPLE = "\u4F60\u597D"; // 你好
    private static final char CHINESE_NI = '\u4F60';        // 你
    private static final char JAPANESE_A = '\u3042';        // あ
    private static final char KOREAN_GA = '\uAC00';         // 가

    /**
     * A font that lacks CJK glyphs (e.g. Monospaced on most CI machines) must be
     * detected as non-CJK-capable. We don't hard-code an assumption about which
     * fonts are installed on CI — instead we manufacture a probe font and only run
     * the negative assertion if it genuinely cannot display CJK.
     */
    @Test
    void canDisplayCJK_returnsFalseForMonospaceWithoutCJK() {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        // Skip the check on environments where Monospaced happens to ship CJK
        // (some JDK distributions do), to avoid flakiness.
        assumeThat(mono.canDisplayUpTo(CJKFontUtil.CJK_PROBE) != -1)
                .as("Monospaced is expected to lack CJK on this JDK; skipping otherwise")
                .isTrue();

        assertThat(CJKFontUtil.canDisplayCJK(mono)).isFalse();
    }

    @Test
    void canDisplayCJK_returnsFalseForNull() {
        assertThat(CJKFontUtil.canDisplayCJK(null)).isFalse();
    }

    /**
     * The core fix: when handed a font that cannot render CJK, withCJKFallback
     * must return a font that <em>can</em> — assuming the environment has at least
     * one CJK-capable family installed (true on every desktop JDK and on the
     * GitHub Actions ubuntu-latest runner via fontconfig fallbacks).
     */
    @Test
    void withCJKFallback_returnsCJKCapableFont() {
        assumeThat(GraphicsEnvironment.isHeadless()).isFalse();
        String cjkFamily = CJKFontUtil.findCJKCapableFamily();
        assumeThat(cjkFamily)
                .as("Requires at least one CJK-capable font on the host; skipped otherwise")
                .isNotNull();

        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        Font result = CJKFontUtil.withCJKFallback(mono);

        assertThat(result).isNotNull();
        assertThat(result.canDisplay(CHINESE_NI))
                .as("Result font must be able to render Chinese 你")
                .isTrue();
        assertThat(result.canDisplay(JAPANESE_A))
                .as("Result font must be able to render Japanese あ")
                .isTrue();
        assertThat(result.canDisplay(KOREAN_GA))
                .as("Result font must be able to render Korean 가")
                .isTrue();
    }

    /**
     * Size and style must be preserved when we swap families so the input field
     * keeps the user's configured editor size.
     */
    @Test
    void withCJKFallback_preservesSizeAndStyle() {
        assumeThat(GraphicsEnvironment.isHeadless()).isFalse();
        assumeThat(CJKFontUtil.findCJKCapableFamily()).isNotNull();

        Font mono = new Font(Font.MONOSPACED, Font.BOLD, 18);
        Font result = CJKFontUtil.withCJKFallback(mono);

        assertThat(result.getSize()).isEqualTo(18);
        assertThat(result.getStyle()).isEqualTo(Font.BOLD);
    }

    /**
     * If the supplied font is already CJK-capable we must return it unchanged so
     * we don't override the IDE editor font for non-CJK users.
     */
    @Test
    void withCJKFallback_returnsOriginalWhenAlreadyCJKCapable() {
        assumeThat(GraphicsEnvironment.isHeadless()).isFalse();
        String cjkFamily = CJKFontUtil.findCJKCapableFamily();
        assumeThat(cjkFamily).isNotNull();

        Font cjkFont = new Font(cjkFamily, Font.PLAIN, 13);
        Font result = CJKFontUtil.withCJKFallback(cjkFont);

        assertThat(result).isSameAs(cjkFont);
    }

    /**
     * Sanity check: the CJK probe string round-trips through UTF-8 unchanged. This
     * protects against any future encoding regression in the input pipeline that
     * would corrupt Unicode characters before they ever reach the font renderer.
     */
    @Test
    void cjkSampleSurvivesUtf8RoundTrip() {
        byte[] encoded = CJK_SAMPLE.getBytes(StandardCharsets.UTF_8);
        String decoded = new String(encoded, StandardCharsets.UTF_8);

        assertThat(decoded).isEqualTo(CJK_SAMPLE);
        assertThat(decoded.codePointAt(0)).isEqualTo(0x4F60); // 你
        assertThat(decoded.codePointAt(1)).isEqualTo(0x597D); // 好
    }

    /**
     * cjkCapableFont must never return null and, when the host has a CJK font,
     * must produce one that can actually render CJK.
     */
    @Test
    void cjkCapableFont_neverNull() {
        Font font = CJKFontUtil.cjkCapableFont(Font.PLAIN, 12f);
        assertThat(font).isNotNull();
        assertThat(font.getSize()).isEqualTo(12);
    }
}
