package com.devoxx.genie.service.acp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcMessage {
    public String jsonrpc = "2.0";
    public Integer id;
    public String method;
    public JsonNode params;
    public JsonNode result;
    public JsonRpcError error;

    public JsonRpcMessage() {}

    public boolean isRequest() {
        return method != null && id != null;
    }

    public boolean isNotification() {
        return method != null && id == null;
    }

    public boolean isResponse() {
        return method == null && id != null;
    }

    public static JsonRpcMessage request(int id, String method, Object params) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = id;
        msg.method = method;
        if (params != null) {
            msg.params = AcpTransport.MAPPER.valueToTree(params);
        }
        return msg;
    }

    public static JsonRpcMessage response(int id, Object resultObj) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = id;
        msg.result = AcpTransport.MAPPER.valueToTree(resultObj);
        return msg;
    }

    public static JsonRpcMessage errorResponse(int id, int code, String message) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = id;
        msg.error = new JsonRpcError(code, message);
        return msg;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonRpcError {
        public int code;
        public String message;

        public JsonRpcError() {}

        public JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
