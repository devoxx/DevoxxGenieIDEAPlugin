package com.devoxx.genie.service.prompt.error;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Turns opaque provider error payloads into a short, actionable, user-facing message.
 *
 * <p>Some providers surface their raw JSON error body through the langchain4j exception message,
 * which then reaches the chat as an unreadable blob (e.g. Cloudflare AI Gateway's
 * {@code {"name":"AiGatewayError","internalCode":2005,...}}). A developer seeing that has no idea
 * what to do. This translator recognises such payloads and replaces them with guidance.</p>
 *
 * <p>Scope is intentionally narrow (Cloudflare AI Gateway only). Anything unrecognised returns
 * {@link Optional#empty()} so the caller keeps its existing message untouched.</p>
 */
public final class ProviderErrorTranslator {

    private ProviderErrorTranslator() {
    }

    /**
     * @param error     the failure (its cause chain is inspected)
     * @param modelName the model that was used, for a concrete message; may be {@code null}
     * @return a friendly, actionable message when the error is recognised, otherwise empty
     */
    public static @NotNull Optional<String> translate(@Nullable Throwable error, @Nullable String modelName) {
        Throwable seen = null;
        for (Throwable t = error; t != null && t != seen; ) {
            String message = t.getMessage();
            if (message != null) {
                Optional<String> friendly = translateCloudflare(message, modelName);
                if (friendly.isPresent()) {
                    return friendly;
                }
            }
            // Guard against self-referential cause chains (t.getCause() == t).
            Throwable next = t.getCause();
            seen = t;
            t = (next == t) ? null : next;
        }
        return Optional.empty();
    }

    private static @NotNull Optional<String> translateCloudflare(@NotNull String message, @Nullable String modelName) {
        if (!message.contains("AiGatewayError")) {
            return Optional.empty();
        }

        String code = null;
        String detail = null;
        JsonObject body = extractJsonObject(message);
        if (body != null) {
            if (body.has("internalCode") && !body.get("internalCode").isJsonNull()) {
                code = body.get("internalCode").getAsString();
            }
            if (body.has("message") && !body.get("message").isJsonNull()) {
                detail = body.get("message").getAsString();
            }
        }

        StringBuilder sb = new StringBuilder("Cloudflare AI Gateway couldn't run ");
        sb.append(modelName != null && !modelName.isBlank() ? "model '" + modelName + "'" : "the selected model");
        if (code != null && !code.isBlank()) {
            sb.append(" (error ").append(code);
            if (detail != null && !detail.isBlank()) {
                sb.append(": ").append(detail);
            }
            sb.append(')');
        } else if (detail != null && !detail.isBlank()) {
            sb.append(" (").append(detail).append(')');
        }
        sb.append(". This usually means the model isn't available on your gateway, or its provider isn't configured. ");
        sb.append("Pick a model from the dropdown (auto-discovered from your gateway), ");
        sb.append("or add that provider's API key in your Cloudflare AI Gateway dashboard.");
        return Optional.of(sb.toString());
    }

    /**
     * Extract the JSON object embedded in a provider error message. The message often has a prefix
     * (e.g. {@code "status code: 400; body: {...}"} or {@code "Provider unavailable: {...}"}), so we
     * take the span from the first '{' to the last '}' and parse it leniently.
     */
    private static @Nullable JsonObject extractJsonObject(@NotNull String message) {
        int start = message.indexOf('{');
        int end = message.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return JsonParser.parseString(message.substring(start, end + 1)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
