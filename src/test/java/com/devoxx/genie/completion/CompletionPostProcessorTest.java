package com.devoxx.genie.completion;

import com.devoxx.genie.completion.CompletionPostProcessor.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompletionPostProcessorTest {

    @Test
    void emptyCompletion_returnsEmptyElements() {
        ProcessedCompletion result = CompletionPostProcessor.process("", "");
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.elements()).isEmpty();
    }

    @Test
    void singleLine_noSuffix_returnsOneGrayText() {
        ProcessedCompletion result = CompletionPostProcessor.process("hello world", "");
        assertThat(result.elements()).containsExactly(
                new GrayText("hello world")
        );
    }

    @Test
    void singleLine_withSuffixOverlap_returnsGrayTextAndSkipText() {
        // completion ends with ")" which matches start of suffix ")"
        ProcessedCompletion result = CompletionPostProcessor.process("foo(bar)", ")");
        assertThat(result.elements()).containsExactly(
                new GrayText("foo(bar"),
                new SkipText(")")
        );
    }

    @Test
    void singleLine_longerSuffixOverlap() {
        // completion ends with ");" which matches start of suffix ");"
        ProcessedCompletion result = CompletionPostProcessor.process("getValue());", ");");
        assertThat(result.elements()).containsExactly(
                new GrayText("getValue()"),
                new SkipText(");")
        );
    }

    @Test
    void singleLine_noOverlap_returnsJustGrayText() {
        ProcessedCompletion result = CompletionPostProcessor.process("hello", "world");
        assertThat(result.elements()).containsExactly(
                new GrayText("hello")
        );
    }

    @Test
    void singleLine_fullOverlap_returnsJustSkipText() {
        // completion == suffix
        ProcessedCompletion result = CompletionPostProcessor.process(")", ")");
        assertThat(result.elements()).containsExactly(
                new SkipText(")")
        );
    }

    @Test
    void multiLine_noSuffix_returnsFirstLineAndRemaining() {
        ProcessedCompletion result = CompletionPostProcessor.process("line1\nline2\nline3", "");
        assertThat(result.elements()).containsExactly(
                new GrayText("line1"),
                new GrayText("\nline2\nline3")
        );
    }

    @Test
    void multiLine_withSuffixOverlap() {
        // completion first line "foo();" ends with ");" which matches suffix ");"
        ProcessedCompletion result = CompletionPostProcessor.process("foo();\nbar();", ");");
        assertThat(result.elements()).containsExactly(
                new GrayText("foo("),
                new SkipText(");"),
                new GrayText("\nbar();")
        );
    }

    @Test
    void leadingNewlines_areStripped() {
        ProcessedCompletion result = CompletionPostProcessor.process("\n\nhello", "");
        assertThat(result.elements()).containsExactly(
                new GrayText("hello")
        );
    }

    @Test
    void leadingNewlinesOnly_returnsEmpty() {
        ProcessedCompletion result = CompletionPostProcessor.process("\n\n\n", "");
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void suffixWithTrailingWhitespace_isTrimmedBeforeOverlap() {
        // suffix has trailing spaces but overlap detection should still work
        ProcessedCompletion result = CompletionPostProcessor.process("foo)", ")   ");
        assertThat(result.elements()).containsExactly(
                new GrayText("foo"),
                new SkipText(")")
        );
    }

    // ── Helper method tests ────────────────────────────────────────

    @Test
    void findSuffixOverlap_exactMatch() {
        assertThat(CompletionPostProcessor.findSuffixOverlap("abc", "abc")).isEqualTo(3);
    }

    @Test
    void findSuffixOverlap_partialMatch() {
        assertThat(CompletionPostProcessor.findSuffixOverlap("hello)", ")")).isEqualTo(1);
    }

    @Test
    void findSuffixOverlap_noMatch() {
        assertThat(CompletionPostProcessor.findSuffixOverlap("abc", "xyz")).isEqualTo(0);
    }

    @Test
    void findSuffixOverlap_emptyStrings() {
        assertThat(CompletionPostProcessor.findSuffixOverlap("", "abc")).isEqualTo(0);
        assertThat(CompletionPostProcessor.findSuffixOverlap("abc", "")).isEqualTo(0);
    }

    @Test
    void stripLeadingNewlines_noNewlines() {
        assertThat(CompletionPostProcessor.stripLeadingNewlines("hello")).isEqualTo("hello");
    }

    @Test
    void stripLeadingNewlines_mixedNewlines() {
        assertThat(CompletionPostProcessor.stripLeadingNewlines("\r\n\nhello")).isEqualTo("hello");
    }

    @Test
    void stripLeadingNewlines_preservesInternalNewlines() {
        assertThat(CompletionPostProcessor.stripLeadingNewlines("\nhello\nworld")).isEqualTo("hello\nworld");
    }
}
