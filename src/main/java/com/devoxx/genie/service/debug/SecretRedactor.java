package com.devoxx.genie.service.debug;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks likely API keys/tokens/secrets in raw LLM request/response payloads before they are
 * shown in the Activity Log or copied to the clipboard. Best-effort pattern matching, not a
 * substitute for keeping real secrets out of prompts.
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
}
