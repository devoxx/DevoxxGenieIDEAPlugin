package com.devoxx.genie.chatmodel.cloud.cloudflare;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Reshapes an OpenAI chat-completions request body into the message shape Cloudflare's
 * {@code /compat} endpoint can hand to Workers AI.
 *
 * <p>Issue #1256: the gateway forwards the request to the Workers AI model, whose native schema
 * requires every message to carry a <em>string</em> {@code content}. Two things trip it up:</p>
 * <ul>
 *   <li>langchain4j omits {@code content} on an assistant message that only carries
 *       {@code tool_calls} (perfectly valid OpenAI), which Workers AI rejects with
 *       {@code required properties at '/messages/N' are 'role,content'}. This is what breaks agent
 *       mode: the first round trip succeeds, the one replaying the tool call 400s.</li>
 *   <li>A multi-part (array) {@code content} is rejected with
 *       {@code Type mismatch of '/messages/N/content', 'array' not in 'string'}.</li>
 * </ul>
 *
 * <p>So: a missing or null {@code content} becomes {@code ""}, and a text-only parts array is
 * flattened into a single string. Arrays holding non-text parts (images) are left alone — vision
 * models need them, and flattening would silently drop the image. Everything else in the body is
 * passed through untouched, and a body we cannot parse is returned as-is.</p>
 */
public final class CloudflareCompatRequestNormalizer {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private CloudflareCompatRequestNormalizer() {
    }

    /**
     * @param body the JSON request body, or {@code null}
     * @return the normalised body, or the input unchanged when there is nothing to do
     */
    @Contract("null -> null; !null -> !null")
    public static @Nullable String normalize(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return body;
            }
            root = parsed.getAsJsonObject();
        } catch (Exception e) {
            return body;
        }

        JsonElement messages = root.get("messages");
        if (messages == null || !messages.isJsonArray()) {
            return body;
        }

        boolean changed = false;
        for (JsonElement element : messages.getAsJsonArray()) {
            if (element.isJsonObject()) {
                changed |= normalizeMessage(element.getAsJsonObject());
            }
        }
        return changed ? GSON.toJson(root) : body;
    }

    private static boolean normalizeMessage(JsonObject message) {
        JsonElement content = message.get("content");

        if (content == null || content.isJsonNull()) {
            message.addProperty("content", "");
            return true;
        }

        if (content.isJsonArray() && isTextOnly(content.getAsJsonArray())) {
            message.addProperty("content", flattenText(content.getAsJsonArray()));
            return true;
        }

        return false;
    }

    private static boolean isTextOnly(JsonArray parts) {
        for (JsonElement part : parts) {
            if (!part.isJsonObject()) {
                return false;
            }
            JsonObject partObject = part.getAsJsonObject();
            JsonElement type = partObject.get("type");
            if (type == null || !type.isJsonPrimitive() || !"text".equals(type.getAsString())) {
                return false;
            }
        }
        return true;
    }

    private static String flattenText(JsonArray parts) {
        StringBuilder text = new StringBuilder();
        for (JsonElement part : parts) {
            JsonElement value = part.getAsJsonObject().get("text");
            if (value != null && value.isJsonPrimitive()) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(value.getAsString());
            }
        }
        return text.toString();
    }
}
