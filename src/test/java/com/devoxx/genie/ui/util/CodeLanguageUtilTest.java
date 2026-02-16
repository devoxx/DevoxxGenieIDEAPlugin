package com.devoxx.genie.ui.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CodeLanguageUtilTest {

    // --- Null and empty input tests ---

    @ParameterizedTest
    @NullAndEmptySource
    void mapLanguageToPrism_nullOrEmpty_returnsPlaintext(String input) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo("plaintext");
    }

    // --- JavaScript variants ---

    @ParameterizedTest
    @CsvSource({"js, javascript", "javascript, javascript"})
    void mapLanguageToPrism_javascript_returnsJavascript(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    // --- TypeScript variants ---

    @ParameterizedTest
    @CsvSource({"ts, typescript", "typescript, typescript"})
    void mapLanguageToPrism_typescript_returnsTypescript(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    // --- Python variants ---

    @ParameterizedTest
    @CsvSource({"py, python", "python, python"})
    void mapLanguageToPrism_python_returnsPython(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    // --- C# variants ---

    @ParameterizedTest
    @ValueSource(strings = {"c#", "csharp", "cs"})
    void mapLanguageToPrism_csharp_returnsCsharp(String input) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo("csharp");
    }

    // --- Simple language mappings ---

    @Test
    void mapLanguageToPrism_java_returnsJava() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("java")).isEqualTo("java");
    }

    @Test
    void mapLanguageToPrism_cpp_returnsCpp() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("c++")).isEqualTo("cpp");
    }

    @Test
    void mapLanguageToPrism_go_returnsGo() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("go")).isEqualTo("go");
    }

    @Test
    void mapLanguageToPrism_rust_returnsRust() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("rust")).isEqualTo("rust");
    }

    @ParameterizedTest
    @CsvSource({"rb, ruby", "ruby, ruby"})
    void mapLanguageToPrism_ruby_returnsRuby(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"kt, kotlin", "kotlin, kotlin"})
    void mapLanguageToPrism_kotlin_returnsKotlin(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    @Test
    void mapLanguageToPrism_json_returnsJson() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("json")).isEqualTo("json");
    }

    @ParameterizedTest
    @CsvSource({"yaml, yaml", "yml, yaml"})
    void mapLanguageToPrism_yaml_returnsYaml(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    @Test
    void mapLanguageToPrism_html_returnsMarkup() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("html")).isEqualTo("markup");
    }

    @Test
    void mapLanguageToPrism_css_returnsCss() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("css")).isEqualTo("css");
    }

    @ParameterizedTest
    @CsvSource({"sh, bash", "bash, bash"})
    void mapLanguageToPrism_bash_returnsBash(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"md, markdown", "markdown, markdown"})
    void mapLanguageToPrism_markdown_returnsMarkdown(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    @Test
    void mapLanguageToPrism_sql_returnsSql() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("sql")).isEqualTo("sql");
    }

    @ParameterizedTest
    @CsvSource({"docker, docker", "dockerfile, docker"})
    void mapLanguageToPrism_docker_returnsDocker(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    @Test
    void mapLanguageToPrism_dart_returnsDart() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("dart")).isEqualTo("dart");
    }

    @Test
    void mapLanguageToPrism_graphql_returnsGraphql() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("graphql")).isEqualTo("graphql");
    }

    @Test
    void mapLanguageToPrism_hcl_returnsHcl() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("hcl")).isEqualTo("hcl");
    }

    @Test
    void mapLanguageToPrism_nginx_returnsNginx() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("nginx")).isEqualTo("nginx");
    }

    @ParameterizedTest
    @CsvSource({"powershell, powershell", "ps, powershell"})
    void mapLanguageToPrism_powershell_returnsPowershell(String input, String expected) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo(expected);
    }

    // --- Unknown languages ---

    @ParameterizedTest
    @ValueSource(strings = {"scala", "perl", "r", "lua", "unknown", "xyz"})
    void mapLanguageToPrism_unknownLanguage_returnsPlaintext(String input) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo("plaintext");
    }

    // --- Case insensitivity ---

    @ParameterizedTest
    @ValueSource(strings = {"JAVA", "Java", "jAvA"})
    void mapLanguageToPrism_caseInsensitive_returnsJava(String input) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo("java");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PYTHON", "Python", "pYtHoN"})
    void mapLanguageToPrism_caseInsensitive_returnsPython(String input) {
        assertThat(CodeLanguageUtil.mapLanguageToPrism(input)).isEqualTo("python");
    }

    // --- Whitespace trimming ---

    @Test
    void mapLanguageToPrism_leadingWhitespace_trimmed() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("  java")).isEqualTo("java");
    }

    @Test
    void mapLanguageToPrism_trailingWhitespace_trimmed() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("java  ")).isEqualTo("java");
    }

    @Test
    void mapLanguageToPrism_surroundingWhitespace_trimmed() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("  python  ")).isEqualTo("python");
    }

    @Test
    void mapLanguageToPrism_whitespaceOnly_returnsPlaintext() {
        assertThat(CodeLanguageUtil.mapLanguageToPrism("   ")).isEqualTo("plaintext");
    }
}
