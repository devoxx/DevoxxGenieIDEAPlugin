package com.devoxx.genie.service.agent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Matches run_command shell commands against the user-configured blacklist (issue #1209).
 *
 * Matching is token-based (whitespace-split), case-insensitive, and may start at any
 * position in the command, so blacklisted commands hidden inside compound commands
 * ("cd x && git reset --hard") are still caught. Between two matched pattern tokens only
 * flag-like tokens (starting with '-') may appear, so "git reset -q --hard" matches the
 * pattern "git reset --hard" while "rm build && grep -rf x" does NOT match "rm -rf".
 *
 * Token comparison rules:
 * - exact match (case-insensitive)
 * - a '*' in a pattern token is a wildcard ("--force*" matches "--force-with-lease")
 * - single-dash short flags match combined/reordered variants ("-rf" matches "-fr" and "-rfv")
 */
public final class CommandBlacklist {

    private CommandBlacklist() {
    }

    private static final Pattern SHORT_FLAG = Pattern.compile("-[a-z0-9]+");

    /**
     * Returns the first blacklist pattern that matches the given command, if any.
     */
    public static Optional<String> findMatch(@Nullable String command, @Nullable List<String> patterns) {
        if (command == null || command.isBlank() || patterns == null || patterns.isEmpty()) {
            return Optional.empty();
        }
        String[] commandTokens = tokenize(command);
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (matches(commandTokens, tokenize(pattern))) {
                return Optional.of(pattern);
            }
        }
        return Optional.empty();
    }

    private static String @NotNull [] tokenize(@NotNull String text) {
        return text.trim().toLowerCase(Locale.ROOT).split("\\s+");
    }

    private static boolean matches(String @NotNull [] command, String @NotNull [] pattern) {
        if (pattern.length == 0) {
            return false;
        }
        for (int start = 0; start < command.length; start++) {
            if (tokenMatches(pattern[0], command[start]) && matchesFrom(command, start + 1, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches pattern tokens 1..n against the command starting right after the token that
     * matched pattern[0]. Unmatched command tokens may only be skipped if they are flag-like.
     */
    private static boolean matchesFrom(String @NotNull [] command, int commandIndex, String @NotNull [] pattern) {
        int ci = commandIndex;
        for (int pi = 1; pi < pattern.length; pi++) {
            boolean found = false;
            while (ci < command.length) {
                if (tokenMatches(pattern[pi], command[ci])) {
                    found = true;
                    ci++;
                    break;
                }
                if (command[ci].startsWith("-")) {
                    ci++;
                    continue;
                }
                return false;
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean tokenMatches(@NotNull String patternToken, @NotNull String commandToken) {
        if (patternToken.equals(commandToken)) {
            return true;
        }
        if (patternToken.indexOf('*') >= 0) {
            return globMatches(patternToken, commandToken);
        }
        return shortFlagMatches(patternToken, commandToken);
    }

    private static boolean globMatches(@NotNull String patternToken, @NotNull String commandToken) {
        StringBuilder regex = new StringBuilder();
        for (String part : patternToken.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(part));
        }
        return commandToken.matches(regex.toString());
    }

    /**
     * "-f" matches "-fd", "-rf" matches "-fr": both tokens must be single-dash short-flag
     * clusters, and every flag letter of the pattern must be present in the command token.
     */
    private static boolean shortFlagMatches(@NotNull String patternToken, @NotNull String commandToken) {
        if (!SHORT_FLAG.matcher(patternToken).matches() || !SHORT_FLAG.matcher(commandToken).matches()) {
            return false;
        }
        for (int i = 1; i < patternToken.length(); i++) {
            if (commandToken.indexOf(patternToken.charAt(i), 1) < 0) {
                return false;
            }
        }
        return true;
    }
}
