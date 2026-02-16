package com.devoxx.genie.ui.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WindowContextFormatterUtilTest {

    // --- format(int, String) with suffix ---

    @Test
    void format_billions_returnsB() {
        assertThat(WindowContextFormatterUtil.format(1_000_000_000, "tokens")).isEqualTo("1B tokens");
    }

    @Test
    void format_multipleBillions_returnsB() {
        assertThat(WindowContextFormatterUtil.format(2_000_000_000, "tokens")).isEqualTo("2B tokens");
    }

    @Test
    void format_billionsWithRemainder_truncatesDown() {
        assertThat(WindowContextFormatterUtil.format(1_500_000_000, "tokens")).isEqualTo("1B tokens");
    }

    @Test
    void format_millions_returnsM() {
        assertThat(WindowContextFormatterUtil.format(1_000_000, "tokens")).isEqualTo("1M tokens");
    }

    @Test
    void format_multipleMillion_returnsM() {
        assertThat(WindowContextFormatterUtil.format(5_000_000, "tokens")).isEqualTo("5M tokens");
    }

    @Test
    void format_millionsWithRemainder_truncatesDown() {
        assertThat(WindowContextFormatterUtil.format(1_999_999, "tokens")).isEqualTo("1M tokens");
    }

    @Test
    void format_thousands_returnsK() {
        assertThat(WindowContextFormatterUtil.format(1_000, "tokens")).isEqualTo("1K tokens");
    }

    @Test
    void format_multipleThousands_returnsK() {
        assertThat(WindowContextFormatterUtil.format(128_000, "tokens")).isEqualTo("128K tokens");
    }

    @Test
    void format_thousandsWithRemainder_truncatesDown() {
        assertThat(WindowContextFormatterUtil.format(1_500, "tokens")).isEqualTo("1K tokens");
    }

    @Test
    void format_lessThanThousand_returnsRawNumber() {
        assertThat(WindowContextFormatterUtil.format(999, "tokens")).isEqualTo("999 tokens");
    }

    @Test
    void format_zero_returnsZero() {
        assertThat(WindowContextFormatterUtil.format(0, "tokens")).isEqualTo("0 tokens");
    }

    @Test
    void format_one_returnsOne() {
        assertThat(WindowContextFormatterUtil.format(1, "tokens")).isEqualTo("1 tokens");
    }

    @Test
    void format_exactBoundary_millions() {
        assertThat(WindowContextFormatterUtil.format(999_999, "ctx")).isEqualTo("999K ctx");
    }

    @Test
    void format_exactBoundary_thousands() {
        assertThat(WindowContextFormatterUtil.format(999, "ctx")).isEqualTo("999 ctx");
    }

    @Test
    void format_emptySuffix() {
        assertThat(WindowContextFormatterUtil.format(128_000, "")).isEqualTo("128K ");
    }

    // --- format(int) without suffix ---

    @Test
    void format_noSuffix_billions() {
        assertThat(WindowContextFormatterUtil.format(1_000_000_000)).isEqualTo("1B ");
    }

    @Test
    void format_noSuffix_millions() {
        assertThat(WindowContextFormatterUtil.format(1_000_000)).isEqualTo("1M ");
    }

    @Test
    void format_noSuffix_thousands() {
        assertThat(WindowContextFormatterUtil.format(1_000)).isEqualTo("1K ");
    }

    @Test
    void format_noSuffix_small() {
        assertThat(WindowContextFormatterUtil.format(42)).isEqualTo("42 ");
    }

    @Test
    void format_noSuffix_zero() {
        assertThat(WindowContextFormatterUtil.format(0)).isEqualTo("0 ");
    }

    // --- Negative values ---

    @Test
    void format_negativeValue_returnsRawNumber() {
        assertThat(WindowContextFormatterUtil.format(-1, "tokens")).isEqualTo("-1 tokens");
    }

    @Test
    void format_negativeThousands_returnsRawNumber() {
        // Negative values are less than 1000, so they go through the else branch
        assertThat(WindowContextFormatterUtil.format(-1000, "tokens")).isEqualTo("-1000 tokens");
    }

    // --- Typical usage scenarios ---

    @Test
    void format_typicalGeminiContext_returnsExpected() {
        // Gemini supports 1M token context
        assertThat(WindowContextFormatterUtil.format(1_000_000, "tokens")).isEqualTo("1M tokens");
    }

    @Test
    void format_typicalGPT4Context_returnsExpected() {
        // GPT-4 128K context
        assertThat(WindowContextFormatterUtil.format(128_000, "tokens")).isEqualTo("128K tokens");
    }

    @Test
    void format_typicalSmallContext_returnsExpected() {
        // Smaller model with 4K context
        assertThat(WindowContextFormatterUtil.format(4_096, "tokens")).isEqualTo("4K tokens");
    }
}
