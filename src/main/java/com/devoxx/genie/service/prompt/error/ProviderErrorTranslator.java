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
        // 'AiGatewayError' is the gateway's own routing failure (issue #1254); 'AiError' comes from
        // the model behind it rejecting the request (issue #1256). Both reach the chat as raw JSON.
        if (!message.contains("AiGatewayError") && !message.contains("AiError")) {
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
                detail = shorten(body.get("message").getAsString());
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
        sb.append(". ").append(guidanceFor(code, modelName));
        return Optional.of(sb.toString());
    }

    /**
     * Issue #1254: pick guidance matching the failure. The gateway's {@code /compat} endpoint
     * addresses models as {@code provider/model}, and the reported failure shapes are naming or
     * routing problems: code 2008 ("Invalid provider") when the prefix — or, via Custom OpenAI,
     * a URL path segment — isn't a provider Cloudflare knows; code 2005 ("Failed to get response
     * from provider") when the id carries no usable provider prefix (bare {@code kimi-k2.6} or a
     * bare Workers AI {@code @cf/...} id) or when the routed provider call itself fails
     * (e.g. a Cloudflare token without Workers AI permission).
     */
    private static @NotNull String guidanceFor(@Nullable String code, @Nullable String modelName) {
        if ("5006".equals(code)) {
            return "The model behind the gateway rejected the request: its schema wants every message "
                    + "to carry a plain string 'content'. This shows up in Agent mode, where the tool "
                    + "call and its result are sent back to the model in the next round trip. "
                    + "DevoxxGenie already reshapes those messages for Workers AI — if the error "
                    + "persists, the gateway's own conversion is at fault: try another Workers AI model "
                    + "with function calling (e.g. 'workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast') "
                    + "or turn Agent mode off for this model.";
        }
        if ("2008".equals(code)) {
            return "Cloudflare didn't recognise the provider it was asked to route to. "
                    + "Gateway models must be addressed as 'provider/model', e.g. 'openai/gpt-4o' — "
                    + "Workers AI models need the 'workers-ai/' prefix, e.g. 'workers-ai/@cf/openai/gpt-oss-20b'. "
                    + "If you configured the gateway through the Custom OpenAI provider instead, the base URL must "
                    + "end with '/compat' (https://gateway.ai.cloudflare.com/v1/ACCOUNT_ID/GATEWAY/compat), "
                    + "otherwise Cloudflare reads the next URL segment as a provider name.";
        }
        if (modelName != null && modelName.startsWith("@cf/")) {
            return "'" + modelName + "' is a bare Workers AI id: the gateway addresses models as "
                    + "'provider/model', so it must be sent as 'workers-ai/" + modelName + "'. "
                    + "The Cloudflare provider adds this prefix automatically — if you configured the gateway "
                    + "through the Custom OpenAI provider, add the 'workers-ai/' prefix to the model name yourself.";
        }
        if (modelName != null && !modelName.isBlank() && !modelName.contains("/")) {
            return "The model id has no provider prefix: gateway models must be addressed as 'provider/model', "
                    + "e.g. 'openai/gpt-4o' or 'workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast'. "
                    + "Pick a model from the dropdown (auto-discovered from your gateway), "
                    + "or prefix the model id with the provider configured in your Cloudflare AI Gateway dashboard.";
        }
        if ("2005".equals(code) && modelName != null && modelName.startsWith("workers-ai/")) {
            return "The gateway routed to Workers AI but the provider call failed. "
                    + "Check that the model id exists on Workers AI (e.g. 'workers-ai/@cf/openai/gpt-oss-20b') and "
                    + "that your Cloudflare API token includes Workers AI permission (or a Workers AI key is stored "
                    + "in your gateway's BYOK settings) — a token scoped to AI Gateway alone cannot run Workers AI models. "
                    + "The gateway's request logs in the Cloudflare dashboard show the upstream provider response.";
        }
        return "This usually means the model isn't available on your gateway, or its provider isn't configured. "
                + "Pick a model from the dropdown (auto-discovered from your gateway), "
                + "or add that provider's API key in your Cloudflare AI Gateway dashboard.";
    }

    /**
     * Keep the headline of a provider message and drop the rest. Cloudflare's {@code AiError}
     * messages append the full JSON-schema validation dump ("Error: oneOf at '/' not met, 0 matches:
     * Type mismatch of '/messages/0/content' ..." — one line per message in the conversation), which
     * is noise to a user and would swamp the guidance that follows.
     */
    private static @NotNull String shorten(@NotNull String detail) {
        // Strip the envelope name first — otherwise "AiError:" is itself mistaken for the dump marker.
        String headline = detail.replace("AiError:", "").trim();
        int schemaDump = headline.indexOf("Error:");
        if (schemaDump > 0) {
            headline = headline.substring(0, schemaDump).trim();
        }
        while (headline.endsWith(":") || headline.endsWith(",") || headline.endsWith(".")) {
            headline = headline.substring(0, headline.length() - 1).trim();
        }
        return headline.isBlank() ? detail : headline;
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
