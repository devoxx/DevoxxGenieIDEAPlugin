package com.devoxx.genie.service.acp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a JSON-RPC 2.0 message used by the ACP protocol.
 *
 * <p>A single class models all three JSON-RPC message types:
 * <ul>
 *   <li><b>Request</b> — has both {@code method} and {@code id}</li>
 *   <li><b>Notification</b> — has {@code method} but no {@code id}</li>
 *   <li><b>Response</b> — has {@code id} but no {@code method}, plus {@code result} or {@code error}</li>
 * </ul>
 *
 * <p>Use the static factory methods ({@link #request}, {@link #response}, {@link #errorResponse})
 * to construct outgoing messages. Incoming messages are deserialized by Jackson and can be
 * classified using {@link #isRequest()}, {@link #isNotification()}, and {@link #isResponse()}.
 *
 * <p>The JSON-RPC version is maintained as a private static final constant {@link #JSONRPC}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcMessage {
    private static final String JSONRPC = "2.0";

    @Setter
    @Getter
    public Integer id;

    public String jsonrpc;
    public String method;
    public JsonNode params;
    public JsonNode result;
    public JsonRpcError error;

    /** Default constructor for Jackson deserialization. */
    public JsonRpcMessage() {}

    /**
     * Returns the JSON-RPC version constant.
     *
     * @return the JSON-RPC version string (always "2.0")
     */
    public static String getJsonrpc() {
        return JSONRPC;
    }

    /**
     * Returns {@code true} if this message is a request (has both {@code method} and {@code id}).
     *
     * @return {@code true} for request messages
     */
    public boolean isRequest() {
        return method != null && id != null;
    }

    /**
     * Returns {@code true} if this message is a notification (has {@code method} but no {@code id}).
     *
     * @return {@code true} for notification messages
     */
    public boolean isNotification() {
        return method != null && id == null;
    }

    /**
     * Returns {@code true} if this message is a response (has {@code id} but no {@code method}).
     *
     * @return {@code true} for response messages
     */
    public boolean isResponse() {
        return method == null && id != null;
    }

    /**
     * Creates a JSON-RPC request message.
     *
     * @param id     the request identifier
     * @param method the method name to invoke
     * @param params the parameters object (serialized to JSON), or {@code null}
     * @return a new request message
     */
    public static JsonRpcMessage request(int id, String method, Object params) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.jsonrpc = JSONRPC;
        msg.setId(id);
        msg.method = method;
        if (params != null) {
            msg.params = AcpTransport.MAPPER.valueToTree(params);
        }
        return msg;
    }

    /**
     * Creates a successful JSON-RPC response message.
     *
     * @param id        the request identifier being responded to
     * @param resultObj the result object (serialized to JSON)
     * @return a new response message
     */
    public static JsonRpcMessage response(int id, Object resultObj) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.jsonrpc = JSONRPC;
        msg.setId(id);
        msg.result = AcpTransport.MAPPER.valueToTree(resultObj);
        return msg;
    }

    /**
     * Creates a JSON-RPC error response message.
     *
     * @param id      the request identifier being responded to
     * @param code    the JSON-RPC error code
     * @param message a human-readable error description
     * @return a new error response message
     */
    public static JsonRpcMessage errorResponse(int id, int code, String message) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.jsonrpc = JSONRPC;
        msg.setId(id);
        msg.error = new JsonRpcError(code, message);
        return msg;
    }

    /** Represents the error object in a JSON-RPC error response. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonRpcError {
        public int code;
        public String message;

        /** Default constructor for Jackson deserialization. */
        public JsonRpcError() {}

        /**
         * Creates an error with the given code and message.
         *
         * @param code    the JSON-RPC error code
         * @param message a human-readable error description
         */
        public JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
