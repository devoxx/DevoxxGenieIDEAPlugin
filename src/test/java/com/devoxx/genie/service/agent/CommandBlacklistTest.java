package com.devoxx.genie.service.agent;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the command blacklist matcher (issue #1209).
 *
 * Matching semantics under test:
 * - Case-insensitive, whitespace-normalized, token-based matching.
 * - A pattern matches when its tokens appear in order anywhere in the command,
 *   allowing only flag-like tokens (starting with '-') in between.
 * - Short single-dash flags match combined variants ("-f" matches "-fd", "-rf" matches "-fr").
 * - A '*' inside a pattern token acts as a wildcard.
 */
class CommandBlacklistTest {

    private static final List<String> PATTERNS = List.of(
            "git reset --hard",
            "git clean -f",
            "rm -rf",
            "git push --force"
    );

    @Test
    void exactMatch() {
        assertThat(CommandBlacklist.findMatch("git reset --hard", PATTERNS))
                .contains("git reset --hard");
    }

    @Test
    void matchIsCaseInsensitive() {
        assertThat(CommandBlacklist.findMatch("GIT RESET --HARD", PATTERNS))
                .contains("git reset --hard");
    }

    @Test
    void matchToleratesExtraWhitespace() {
        assertThat(CommandBlacklist.findMatch("git   reset \t --hard", PATTERNS))
                .contains("git reset --hard");
    }

    @Test
    void matchesInsideCompoundCommand() {
        assertThat(CommandBlacklist.findMatch("cd subdir && git reset --hard HEAD~1", PATTERNS))
                .contains("git reset --hard");
    }

    @Test
    void matchesWithInterveningFlag() {
        // "-q" between "reset" and "--hard" is flag-like, so the pattern still applies
        assertThat(CommandBlacklist.findMatch("git reset -q --hard", PATTERNS))
                .contains("git reset --hard");
    }

    @Test
    void nonFlagTokensBetweenPatternTokensPreventMatch() {
        // 'rm' and '-rf' both appear, but separated by non-flag tokens:
        // this is "rm build" followed by "grep -rf", not a destructive rm.
        assertThat(CommandBlacklist.findMatch("rm build && grep -rf pattern .", PATTERNS))
                .isEmpty();
    }

    @Test
    void shortFlagMatchesCombinedFlags() {
        // "-rf" should match reordered/extended short-flag combos
        assertThat(CommandBlacklist.findMatch("rm -fr target", PATTERNS)).contains("rm -rf");
        assertThat(CommandBlacklist.findMatch("rm -rfv target", PATTERNS)).contains("rm -rf");
        // "git clean -f" should match "git clean -fd"
        assertThat(CommandBlacklist.findMatch("git clean -fd", PATTERNS)).contains("git clean -f");
    }

    @Test
    void shortFlagDoesNotMatchWhenLettersMissing() {
        // "-r" alone must not satisfy the "-rf" pattern
        assertThat(CommandBlacklist.findMatch("rm -r target", PATTERNS)).isEmpty();
    }

    @Test
    void wildcardTokenMatches() {
        List<String> patterns = List.of("git push --force*");
        assertThat(CommandBlacklist.findMatch("git push --force-with-lease origin", patterns))
                .contains("git push --force*");
        assertThat(CommandBlacklist.findMatch("git push --force origin", patterns))
                .contains("git push --force*");
    }

    @Test
    void tokenMatchingAvoidsSubstringFalsePositives() {
        // "firm" contains "rm" as a substring but is a different token
        assertThat(CommandBlacklist.findMatch("firm -rf", PATTERNS)).isEmpty();
        // "git resetx --hard" is not "git reset --hard"
        assertThat(CommandBlacklist.findMatch("git resetx --hard", PATTERNS)).isEmpty();
    }

    @Test
    void harmlessCommandsDoNotMatch() {
        assertThat(CommandBlacklist.findMatch("git status", PATTERNS)).isEmpty();
        assertThat(CommandBlacklist.findMatch("./gradlew test", PATTERNS)).isEmpty();
        assertThat(CommandBlacklist.findMatch("git push origin main", PATTERNS)).isEmpty();
    }

    @Test
    void prefixedCommandsStillMatch() {
        assertThat(CommandBlacklist.findMatch("sudo rm -rf /tmp/build", PATTERNS))
                .contains("rm -rf");
    }

    @Test
    void nullOrBlankCommandNeverMatches() {
        assertThat(CommandBlacklist.findMatch(null, PATTERNS)).isEmpty();
        assertThat(CommandBlacklist.findMatch("", PATTERNS)).isEmpty();
        assertThat(CommandBlacklist.findMatch("   ", PATTERNS)).isEmpty();
    }

    @Test
    void nullEmptyAndBlankPatternsAreIgnored()  {
        assertThat(CommandBlacklist.findMatch("git reset --hard", null)).isEmpty();
        assertThat(CommandBlacklist.findMatch("git reset --hard", Collections.emptyList())).isEmpty();

        List<String> withBlanks = new java.util.ArrayList<>();
        withBlanks.add("   ");
        withBlanks.add(null);
        withBlanks.add("git reset --hard");
        assertThat(CommandBlacklist.findMatch("git reset --hard", withBlanks))
                .contains("git reset --hard");
    }
}
