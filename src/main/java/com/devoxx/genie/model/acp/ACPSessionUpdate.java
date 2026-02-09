package com.devoxx.genie.model.acp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a parsed session/update notification from an ACP agent.
 * These are streamed during a prompt turn to report progress.
 */
@Data
public class ACPSessionUpdate {

    public enum UpdateType {
        AGENT_MESSAGE_CHUNK,
        AGENT_THOUGHT_CHUNK,
        USER_MESSAGE_CHUNK,
        TOOL_CALL,
        TOOL_CALL_UPDATE,
        PLAN,
        AVAILABLE_COMMANDS_UPDATE,
        CURRENT_MODE_UPDATE,
        CONFIG_OPTION_UPDATE,
        UNKNOWN
    }

    private UpdateType type;
    private String sessionId;

    // For message chunks
    @Nullable
    private String textContent;

    // For tool calls
    @Nullable
    private String toolCallId;
    @Nullable
    private String toolCallTitle;
    @Nullable
    private String toolCallStatus;
    @Nullable
    private String toolCallRawInput;
    @Nullable
    private String toolCallRawOutput;

    // Raw update JSON for types we don't fully parse yet
    @Nullable
    private JsonObject rawUpdate;

    /**
     * Parse a session/update notification from its params JSON.
     */
    public static ACPSessionUpdate fromParams(@NotNull JsonElement params) {
        ACPSessionUpdate update = new ACPSessionUpdate();

        if (!params.isJsonObject()) {
            update.setType(UpdateType.UNKNOWN);
            return update;
        }

        JsonObject paramsObj = params.getAsJsonObject();
        if (paramsObj.has("sessionId")) {
            update.setSessionId(paramsObj.get("sessionId").getAsString());
        }

        if (!paramsObj.has("update") || !paramsObj.get("update").isJsonObject()) {
            update.setType(UpdateType.UNKNOWN);
            return update;
        }

        JsonObject updateObj = paramsObj.getAsJsonObject("update");
        update.setRawUpdate(updateObj);

        String typeStr = updateObj.has("type") ? updateObj.get("type").getAsString() : "";
        update.setType(parseType(typeStr));

        switch (update.getType()) {
            case AGENT_MESSAGE_CHUNK, AGENT_THOUGHT_CHUNK, USER_MESSAGE_CHUNK ->
                    update.setTextContent(extractTextContent(updateObj));
            case TOOL_CALL, TOOL_CALL_UPDATE -> parseToolCall(updateObj, update);
            default -> { /* raw JSON preserved in rawUpdate */ }
        }

        return update;
    }

    private static UpdateType parseType(String typeStr) {
        return switch (typeStr) {
            case "agent_message_chunk" -> UpdateType.AGENT_MESSAGE_CHUNK;
            case "agent_thought_chunk" -> UpdateType.AGENT_THOUGHT_CHUNK;
            case "user_message_chunk" -> UpdateType.USER_MESSAGE_CHUNK;
            case "tool_call" -> UpdateType.TOOL_CALL;
            case "tool_call_update" -> UpdateType.TOOL_CALL_UPDATE;
            case "plan" -> UpdateType.PLAN;
            case "available_commands_update" -> UpdateType.AVAILABLE_COMMANDS_UPDATE;
            case "current_mode_update" -> UpdateType.CURRENT_MODE_UPDATE;
            case "config_option_update" -> UpdateType.CONFIG_OPTION_UPDATE;
            default -> UpdateType.UNKNOWN;
        };
    }

    @Nullable
    private static String extractTextContent(@NotNull JsonObject updateObj) {
        if (!updateObj.has("content") || !updateObj.get("content").isJsonObject()) {
            return null;
        }
        JsonObject content = updateObj.getAsJsonObject("content");
        if (content.has("text")) {
            return content.get("text").getAsString();
        }
        return null;
    }

    private static void parseToolCall(@NotNull JsonObject updateObj, @NotNull ACPSessionUpdate update) {
        if (updateObj.has("toolCallId")) {
            update.setToolCallId(updateObj.get("toolCallId").getAsString());
        }
        if (updateObj.has("title")) {
            update.setToolCallTitle(updateObj.get("title").getAsString());
        }
        if (updateObj.has("status")) {
            update.setToolCallStatus(updateObj.get("status").getAsString());
        }
        if (updateObj.has("rawInput")) {
            update.setToolCallRawInput(updateObj.get("rawInput").getAsString());
        }
        if (updateObj.has("rawOutput")) {
            update.setToolCallRawOutput(updateObj.get("rawOutput").getAsString());
        }
    }
}
