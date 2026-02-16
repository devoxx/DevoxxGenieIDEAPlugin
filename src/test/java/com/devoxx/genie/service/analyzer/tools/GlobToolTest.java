package com.devoxx.genie.service.analyzer.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobToolTest {

    @Test
    void convertGlobToRegex_singleStar_matchesWithinDirectory() {
        String regex = GlobTool.convertGlobToRegex("*.java");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("Test.java").matches()).isTrue();
        assertThat(pattern.matcher("MyClass.java").matches()).isTrue();
        assertThat(pattern.matcher("src/Test.java").matches()).isFalse(); // single * does not cross /
    }

    @Test
    void convertGlobToRegex_doubleStar_matchesAcrossDirectories() {
        String regex = GlobTool.convertGlobToRegex("**/*.java");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("src/Test.java").matches()).isTrue();
        assertThat(pattern.matcher("src/main/java/Test.java").matches()).isTrue();
    }

    @Test
    void convertGlobToRegex_questionMark_matchesSingleCharacter() {
        String regex = GlobTool.convertGlobToRegex("?.txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("a.txt").matches()).isTrue();
        assertThat(pattern.matcher("1.txt").matches()).isTrue();
        assertThat(pattern.matcher("ab.txt").matches()).isFalse();
        assertThat(pattern.matcher("/.txt").matches()).isFalse(); // ? does not match /
    }

    @Test
    void convertGlobToRegex_characterClass_matchesRange() {
        String regex = GlobTool.convertGlobToRegex("[abc].txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("a.txt").matches()).isTrue();
        assertThat(pattern.matcher("b.txt").matches()).isTrue();
        assertThat(pattern.matcher("c.txt").matches()).isTrue();
        assertThat(pattern.matcher("d.txt").matches()).isFalse();
    }

    @Test
    void convertGlobToRegex_negatedCharacterClass_matchesCorrectly() {
        String regex = GlobTool.convertGlobToRegex("[!abc].txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("d.txt").matches()).isTrue();
        assertThat(pattern.matcher("a.txt").matches()).isFalse();
    }

    @Test
    void convertGlobToRegex_braceGroup_matchesAlternatives() {
        String regex = GlobTool.convertGlobToRegex("{foo,bar}.txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("foo.txt").matches()).isTrue();
        assertThat(pattern.matcher("bar.txt").matches()).isTrue();
        assertThat(pattern.matcher("baz.txt").matches()).isFalse();
    }

    @Test
    void convertGlobToRegex_dotEscaped() {
        String regex = GlobTool.convertGlobToRegex("file.txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("file.txt").matches()).isTrue();
        assertThat(pattern.matcher("filextxt").matches()).isFalse(); // dot should not match any char
    }

    @Test
    void convertGlobToRegex_escapedBackslash() {
        String regex = GlobTool.convertGlobToRegex("file\\*");

        // The glob "\*" should result in a literal * in the regex
        assertThat(regex).contains("\\*");
    }

    @Test
    void convertGlobToRegex_specialRegexCharsEscaped() {
        String regex = GlobTool.convertGlobToRegex("file(1).txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("file(1).txt").matches()).isTrue();
    }

    @Test
    void convertGlobToRegex_plusSignEscaped() {
        String regex = GlobTool.convertGlobToRegex("file+name.txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("file+name.txt").matches()).isTrue();
    }

    @Test
    void convertGlobToRegex_dollarSignEscaped() {
        String regex = GlobTool.convertGlobToRegex("$file.txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("$file.txt").matches()).isTrue();
    }

    @Test
    void convertGlobToRegex_pipeEscaped() {
        String regex = GlobTool.convertGlobToRegex("a|b.txt");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("a|b.txt").matches()).isTrue();
        assertThat(pattern.matcher("a.txt").matches()).isFalse();
    }

    @Test
    void convertGlobToRegex_commaOutsideBraces_treatedAsLiteral() {
        String regex = GlobTool.convertGlobToRegex("a,b");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("a,b").matches()).isTrue();
    }

    @Test
    void convertGlobToRegex_trailingBackslash() {
        String regex = GlobTool.convertGlobToRegex("file\\");

        // A trailing backslash should be preserved as a literal backslash
        assertThat(regex).endsWith("\\");
    }

    @Test
    void convertGlobToRegex_escapedComma() {
        String regex = GlobTool.convertGlobToRegex("file\\,name");

        assertThat(regex).contains("\\,");
    }

    @Test
    void convertGlobToRegex_atSignEscaped() {
        String regex = GlobTool.convertGlobToRegex("user@host");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("user@host").matches()).isTrue();
    }

    @Test
    void convertGlobToRegex_percentSignEscaped() {
        String regex = GlobTool.convertGlobToRegex("100%");
        Pattern pattern = Pattern.compile(regex);

        assertThat(pattern.matcher("100%").matches()).isTrue();
    }

    @Test
    void convertGlobToRegex_caretInCharClass_isEscaped() {
        // [^ should be escaped so it does not become a negation
        String regex = GlobTool.convertGlobToRegex("[^a]");
        // The ^ after [ should be double-escaped: the [ handler adds one \, the ^ handler adds another \ before ^
        assertThat(regex).contains("[\\\\^a]");
    }
}
