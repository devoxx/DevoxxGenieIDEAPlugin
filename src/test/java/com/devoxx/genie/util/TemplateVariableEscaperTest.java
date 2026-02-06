package com.devoxx.genie.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateVariableEscaperTest {

    @Test
    void escape_tripleOpenAndCloseBraces() {
        String input = "{{{ xyzzy }}}";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).isEqualTo("\\{\\{\\{ xyzzy \\}\\}\\}");
        assertThat(escaped).doesNotContain("{{{");
        assertThat(escaped).doesNotContain("}}}");
    }

    @Test
    void escape_doubleOpenAndCloseBraces() {
        String input = "{{ variable }}";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).isEqualTo("\\{\\{ variable \\}\\}");
        assertThat(escaped).doesNotContain("{{");
        assertThat(escaped).doesNotContain("}}");
    }

    @Test
    void escape_mixedTripleAndDoubleBraces() {
        String input = "{{{ unescaped }}} and {{ escaped }}";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).isEqualTo("\\{\\{\\{ unescaped \\}\\}\\} and \\{\\{ escaped \\}\\}");
    }

    @Test
    void escape_sourceCodeWithTripleBraces() {
        // Simulates the exact scenario from issue #791
        String input = "<FileContents>\n{{{ xyzzy }}}\n</FileContents>\n<UserPrompt>\nhello\n</UserPrompt>";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).doesNotContain("{{{");
        assertThat(escaped).doesNotContain("}}}");
        assertThat(escaped).contains("\\{\\{\\{ xyzzy \\}\\}\\}");
    }

    @Test
    void escape_singleBracesUnchanged() {
        String input = "if (x > 0) { return x; }";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).isEqualTo(input);
    }

    @Test
    void escape_nullInput() {
        assertThat(TemplateVariableEscaper.escape(null)).isNull();
    }

    @Test
    void escape_emptyString() {
        assertThat(TemplateVariableEscaper.escape("")).isEmpty();
    }

    @Test
    void escape_noBraces() {
        String input = "Hello, world!";
        assertThat(TemplateVariableEscaper.escape(input)).isEqualTo(input);
    }

    @Test
    void escape_quadrupleBraces() {
        // Four braces: triple-brace escape runs first, then remaining brace
        // pairs with escaped brace neighbor to form {{ which is also escaped
        String input = "{{{{ test }}}}";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).doesNotContain("{{{");
        assertThat(escaped).doesNotContain("}}}");
        assertThat(escaped).doesNotContain("{{");
        assertThat(escaped).doesNotContain("}}");
    }

    @Test
    void escape_adjacentTripleBraces() {
        String input = "{{{a}}}{{{b}}}";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).doesNotContain("{{{");
        assertThat(escaped).doesNotContain("}}}");
    }

    @Test
    void escape_mustacheTemplateInJavaScript() {
        // Real-world scenario: JavaScript/Handlebars code attached as source
        String input = "function render() { return `{{{body}}}` + '{{title}}'; }";
        String escaped = TemplateVariableEscaper.escape(input);
        assertThat(escaped).doesNotContain("{{{");
        assertThat(escaped).doesNotContain("}}}");
        assertThat(escaped).doesNotContain("{{");
        assertThat(escaped).doesNotContain("}}");
        // Single braces from the function body should be preserved
        assertThat(escaped).contains("function render() {");
        assertThat(escaped).contains("; }");
    }
}
