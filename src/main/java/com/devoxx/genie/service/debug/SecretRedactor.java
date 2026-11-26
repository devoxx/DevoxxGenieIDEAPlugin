package com.devoxx.genie.service.debug;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Masks likely API keys/tokens/secrets in raw LLM request/response payloads before they are
 * shown in the Activity Log or copied to the clipboard. Best-effort pattern matching, not a
 * substitute for keeping real secrets out of prompts.
 * <p>
 * {@link #redactForTrace} is the stricter variant used for stored agent traces (task-279):
 * a wider set of patterns (ported from copilot-traces-hf's {@code redact.ts}), whole-value
 * placeholders such as {@code [REDACTED_GITHUB_TOKEN]}, home-directory/username replacement and
 * user-supplied patterns.
 */
public final class SecretRedactor {

    private SecretRedactor() {}

    private static final Pattern BEARER_TOKEN =
            Pattern.compile("(?i)(bearer\\s+)([A-Za-z0-9\\-_.=]{8,})");

    /** Well-known provider API key prefixes/shapes (OpenAI/Anthropic, AWS, Google, Groq, ...). */
    private static final Pattern KNOWN_KEY_SHAPES = Pattern.compile(
            "\\b(sk-ant-[A-Za-z0-9\\-_]{10,}|sk-[A-Za-z0-9\\-_]{16,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z\\-_]{20,}|gsk_[A-Za-z0-9]{16,})\\b");

    /** Any JSON string field whose name looks secret-ish, regardless of value shape. */
    private static final Pattern JSON_SECRET_FIELD = Pattern.compile(
            "(?i)(\"(?:api[_-]?key|access[_-]?token|auth[_-]?token|authorization|secret|password|client[_-]?secret)\"\\s*:\\s*\")([^\"]{4,})(\")");

    @Contract("null -> null; !null -> !null")
    public static @Nullable String redact(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // quoteReplacement: replaceAll(Function) feeds the produced string through
        // appendReplacement, so an unquoted '$' or '\' in a matched secret would throw
        // "Illegal group reference" or corrupt the output.
        String result = BEARER_TOKEN.matcher(text)
                .replaceAll(m -> Matcher.quoteReplacement(m.group(1) + mask(m.group(2))));
        result = KNOWN_KEY_SHAPES.matcher(result)
                .replaceAll(m -> Matcher.quoteReplacement(mask(m.group(1))));
        result = JSON_SECRET_FIELD.matcher(result)
                .replaceAll(m -> Matcher.quoteReplacement(m.group(1) + mask(m.group(2)) + m.group(3)));
        return result;
    }

    private static @NotNull String mask(@NotNull String secret) {
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    // ---------------------------------------------------------------------------------------
    // Trace redaction (task-279)
    // ---------------------------------------------------------------------------------------

    /** Prefix of every placeholder {@link #redactForTrace} writes; patterns never re-match it. */
    public static final String PLACEHOLDER_PREFIX = "[REDACTED";

    private record LabeledPattern(String label, Pattern pattern, int secretGroup) {}

    /**
     * Ordered: more specific shapes first (e.g. Anthropic before the generic {@code sk-} key), the
     * generic key=value rule last so it only sees what the shape rules left behind.
     */
    private static final List<LabeledPattern> TRACE_PATTERNS = List.of(
            new LabeledPattern("PRIVATE_KEY", Pattern.compile(
                    "-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"), 0),
            new LabeledPattern("HF_TOKEN", Pattern.compile("\\bhf_[A-Za-z0-9]{30,}\\b"), 0),
            new LabeledPattern("GITHUB_PAT", Pattern.compile("\\bgithub_pat_[A-Za-z0-9_]{40,}\\b"), 0),
            new LabeledPattern("GITHUB_TOKEN", Pattern.compile("\\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{36,}\\b"), 0),
            new LabeledPattern("ANTHROPIC_KEY", Pattern.compile("\\bsk-ant-[A-Za-z0-9_-]{20,}"), 0),
            new LabeledPattern("STRIPE_KEY", Pattern.compile("\\b(?:sk|rk)_(?:live|test)_[A-Za-z0-9]{20,}\\b"), 0),
            new LabeledPattern("OPENAI_KEY", Pattern.compile("\\bsk-(?:proj-|svcacct-|admin-)?[A-Za-z0-9_-]{20,}"), 0),
            new LabeledPattern("GROQ_KEY", Pattern.compile("\\bgsk_[A-Za-z0-9]{20,}\\b"), 0),
            new LabeledPattern("AWS_ACCESS_KEY", Pattern.compile("\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b"), 0),
            new LabeledPattern("GOOGLE_API_KEY", Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b"), 0),
            new LabeledPattern("SLACK_TOKEN", Pattern.compile("\\bxox[abprs]-[A-Za-z0-9-]{10,}"), 0),
            new LabeledPattern("JWT", Pattern.compile(
                    "\\beyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"), 0),
            new LabeledPattern("BEARER_TOKEN", Pattern.compile(
                    "(?i)(\\bbearer\\s+)(?!\\[REDACTED)([A-Za-z0-9\\-_.=]{8,})"), 2),
            // key=value / key: value / "key": "value" assignments of obviously secret names.
            new LabeledPattern("SECRET", Pattern.compile(
                    "(?i)(\\b(?:api[_-]?key|secret|password|passwd|access[_-]?token|auth[_-]?token|client[_-]?secret)\\b[\"']?\\s*[:=]\\s*[\"']?)"
                            + "(?!\\[REDACTED)([^\\s\"',;]{8,})"), 2));

    /**
     * Options for {@link #redactForTrace}.
     *
     * @param replacements   literal strings to replace (e.g. home directory → {@code ~}), applied in order
     * @param customPatterns extra user patterns; each match becomes {@code [REDACTED]}
     */
    public record TraceRedactionOptions(@NotNull List<Replacement> replacements,
                                        @NotNull List<Pattern> customPatterns) {

        public TraceRedactionOptions {
            replacements = List.copyOf(replacements);
            customPatterns = List.copyOf(customPatterns);
        }

        public static final TraceRedactionOptions SECRETS_ONLY = new TraceRedactionOptions(List.of(), List.of());

        /**
         * Builds options from settings. Invalid custom patterns are skipped (the settings page
         * refuses them, but a hand-edited settings file could still contain one).
         *
         * @param homeDirectory directory to replace with {@code ~}, or {@code null} to skip
         * @param userName      OS username to replace with {@code <user>}, or {@code null} to skip
         */
        public static @NotNull TraceRedactionOptions of(@Nullable String homeDirectory,
                                                        @Nullable String userName,
                                                        @NotNull List<String> customPatterns) {
            List<Replacement> replacements = new ArrayList<>();
            if (homeDirectory != null && homeDirectory.length() >= 3) {
                replacements.add(Replacement.literal(homeDirectory, "~"));
                String alternate = homeDirectory.indexOf('\\') >= 0
                        ? homeDirectory.replace('\\', '/')
                        : null;
                if (alternate != null) {
                    replacements.add(Replacement.literal(alternate, "~"));
                    // JSON-escaped Windows paths, as they appear inside tool-call arguments.
                    replacements.add(Replacement.literal(homeDirectory.replace("\\", "\\\\"), "~"));
                }
            }
            if (userName != null && userName.length() >= 3) {
                replacements.add(Replacement.word(userName, "<user>"));
            }
            List<Pattern> patterns = new ArrayList<>();
            for (String p : customPatterns) {
                if (p == null || p.isBlank()) {
                    continue;
                }
                try {
                    patterns.add(Pattern.compile(p));
                } catch (PatternSyntaxException ignored) {
                    // skipped: validated in the settings UI
                }
            }
            return new TraceRedactionOptions(replacements, patterns);
        }
    }

    /** A literal (optionally whole-word) replacement, e.g. the home directory or the username. */
    public record Replacement(@NotNull Pattern pattern, @NotNull String replacement) {

        public static @NotNull Replacement literal(@NotNull String find, @NotNull String replace) {
            return new Replacement(Pattern.compile(Pattern.quote(find)), replace);
        }

        /** Matches {@code find} only when not part of a longer identifier (so user "sam" leaves "sample"). */
        public static @NotNull Replacement word(@NotNull String find, @NotNull String replace) {
            return new Replacement(Pattern.compile("(?<![A-Za-z0-9_.-])" + Pattern.quote(find) + "(?![A-Za-z0-9_])"), replace);
        }
    }

    /** Result of {@link #redactForTrace}: the redacted text and how many replacements were made. */
    public record TraceRedaction(@Nullable String text, int count) {}

    /**
     * Redacts a string bound for a stored agent trace. Secrets are replaced as a whole with a
     * labelled placeholder ({@code [REDACTED_<LABEL>]}); custom pattern matches with
     * {@code [REDACTED]}; then literal replacements (home directory, username) are applied.
     */
    public static @NotNull TraceRedaction redactForTrace(@Nullable String text, @NotNull TraceRedactionOptions options) {
        if (text == null || text.isEmpty()) {
            return new TraceRedaction(text, 0);
        }
        int[] count = {0};
        String result = text;
        for (LabeledPattern lp : TRACE_PATTERNS) {
            String placeholder = PLACEHOLDER_PREFIX + "_" + lp.label() + "]";
            result = lp.pattern().matcher(result).replaceAll(m -> {
                count[0]++;
                String prefix = lp.secretGroup() == 0 ? "" : m.group(1);
                return Matcher.quoteReplacement(prefix + placeholder);
            });
        }
        for (Pattern custom : options.customPatterns()) {
            result = custom.matcher(result).replaceAll(m -> {
                if (m.group().isEmpty()) {
                    return "";
                }
                count[0]++;
                return Matcher.quoteReplacement(PLACEHOLDER_PREFIX + "]");
            });
        }
        for (Replacement r : options.replacements()) {
            result = r.pattern().matcher(result).replaceAll(m -> {
                count[0]++;
                return Matcher.quoteReplacement(r.replacement());
            });
        }
        return new TraceRedaction(result, count[0]);
    }
}
