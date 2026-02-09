package com.devoxx.genie.model.acp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a JSON-RPC 2.0 message used by the Agent Client Protocol.
 * Handles both requests (with id), notifications (without id), and responses.
 */
@Data
public class ACPMessage {

    private static final Gson GSON = new GsonBuilder().create();

    private final String jsonrpc = "2.0";
    @Nullable
    private Integer id;
    @Nullable
    private String method;
    @Nullable
    private JsonElement params;
    @Nullable
    private JsonElement result;
    @Nullable
    private JsonObject error;

    /**
     * Create a JSON-RPC request message.
     */
    public static ACPMessage request(int id, String method, JsonElement params) {
        ACPMessage msg = new ACPMessage();
        msg.id = id;
        msg.method = method;
        msg.params = params;
        return msg;
    }

    /**
     * Create a JSON-RPC notification (no id, no response expected).
     */
    public static ACPMessage notification(String method, JsonElement params) {
        ACPMessage msg = new ACPMessage();
        msg.method = method;
        msg.params = params;
        return msg;
    }

    /**
     * Create a JSON-RPC response (for answering agent requests like request_permission).
     */
    public static ACPMessage response(int id, JsonElement result) {
        ACPMessage msg = new ACPMessage();
        msg.id = id;
        msg.result = result;
        return msg;
    }

    /**
     * Parse a JSON-RPC message from a JSON string.
     */
    public static ACPMessage fromJson(String json) {
        return GSON.fromJson(json, ACPMessage.class);
    }

    /**
     * Serialize this message to JSON.
     */
    public String toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("jsonrpc", jsonrpc);
        if (id != null) {
            obj.addProperty("id", id);
        }
        if (method != null) {
            obj.addProperty("method", method);
        }
        if (params != null) {
            obj.add("params", params);
        }
        if (result != null) {
            obj.add("result", result);
        }
        if (error != null) {
            obj.add("error", error);
        }
        return GSON.toJson(obj);
    }

    public boolean isResponse() {
        return id != null && method == null;
    }

    public boolean isRequest() {
        return id != null && method != null;
    }

    public boolean isNotification() {
        return id == null && method != null;
    }

    public boolean isError() {
        return error != null;
    }
}
