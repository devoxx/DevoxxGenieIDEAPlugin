package com.devoxx.genie.completion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOpenAIFimProviderTest {

    @Test
    void firstMeaningfulSuffixLine_returnsFirstLongEnoughLine() {
        assertThat(CustomOpenAIFimProvider.firstMeaningfulSuffixLine(
                "\n\n    public String toString() {")).isEqualTo("    public String toString() {");
    }

    @Test
    void firstMeaningfulSuffixLine_skipsBlankAndClosingBraceLines() {
        assertThat(CustomOpenAIFimProvider.firstMeaningfulSuffixLine(
                "\n}\n});\n    return calculateValue();")).isEqualTo("    return calculateValue();");
    }

    @Test
    void firstMeaningfulSuffixLine_returnsNullWhenFirstCandidateTooShort() {
        assertThat(CustomOpenAIFimProvider.firstMeaningfulSuffixLine(
                "\n    x();\n    return calculateValue();")).isNull();
    }

    @Test
    void firstMeaningfulSuffixLine_returnsNullForNullSuffix() {
        assertThat(CustomOpenAIFimProvider.firstMeaningfulSuffixLine(null)).isNull();
    }

    @Test
    void firstMeaningfulSuffixLine_returnsNullWhenOnlyClosers() {
        assertThat(CustomOpenAIFimProvider.firstMeaningfulSuffixLine("\n}\n});")).isNull();
    }
}
