package com.devoxx.genie.service.acp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRpcMessageTest {

    @Test
    void testRequest_hasMethodAndId() {
        JsonRpcMessage msg = JsonRpcMessage.request(1, "initialize", Map.of("key", "value"));

        assertThat(msg.isRequest()).isTrue();
        assertThat(msg.isNotification()).isFalse();
        assertThat(msg.isResponse()).isFalse();
        assertThat(msg.id).isEqualTo(1);
        assertThat(msg.method).isEqualTo("initialize");
        assertThat(msg.jsonrpc).isEqualTo("2.0");
        assertThat(msg.params).isNotNull();
    }

    @Test
    void testRequest_withNullParams() {
        JsonRpcMessage msg = JsonRpcMessage.request(2, "session/new", null);

        assertThat(msg.isRequest()).isTrue();
        assertThat(msg.params).isNull();
    }

    @Test
    void testResponse_hasIdAndResult() {
        JsonRpcMessage msg = JsonRpcMessage.response(1, Map.of("protocolVersion", "1"));

        assertThat(msg.isResponse()).isTrue();
        assertThat(msg.isRequest()).isFalse();
        assertThat(msg.isNotification()).isFalse();
        assertThat(msg.id).isEqualTo(1);
        assertThat(msg.result).isNotNull();
        assertThat(msg.error).isNull();
    }

    @Test
    void testErrorResponse_hasIdAndError() {
        JsonRpcMessage msg = JsonRpcMessage.errorResponse(1, -32603, "Internal error");

        assertThat(msg.isResponse()).isTrue();
        assertThat(msg.id).isEqualTo(1);
        assertThat(msg.error).isNotNull();
        assertThat(msg.error.code).isEqualTo(-32603);
        assertThat(msg.error.message).isEqualTo("Internal error");
        assertThat(msg.result).isNull();
    }

    @Test
    void testNotification_hasMethodButNoId() {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.method = "session/update";
        // id is null by default

        assertThat(msg.isNotification()).isTrue();
        assertThat(msg.isRequest()).isFalse();
        assertThat(msg.isResponse()).isFalse();
    }

    @Test
    void testSerialization_roundTrip() throws Exception {
        JsonRpcMessage original = JsonRpcMessage.request(42, "session/prompt",
                Map.of("sessionId", "abc", "text", "hello"));

        String json = AcpTransport.MAPPER.writeValueAsString(original);
        JsonRpcMessage deserialized = AcpTransport.MAPPER.readValue(json, JsonRpcMessage.class);

        assertThat(deserialized.id).isEqualTo(42);
        assertThat(deserialized.method).isEqualTo("session/prompt");
        assertThat(deserialized.jsonrpc).isEqualTo("2.0");
        assertThat(deserialized.isRequest()).isTrue();
    }

    @Test
    void testSerialization_excludesNullFields() throws Exception {
        JsonRpcMessage msg = JsonRpcMessage.request(1, "test", null);

        String json = AcpTransport.MAPPER.writeValueAsString(msg);

        assertThat(json).contains("\"id\"");
        assertThat(json).contains("\"method\"");
        assertThat(json).doesNotContain("\"params\"");
        assertThat(json).doesNotContain("\"result\"");
        assertThat(json).doesNotContain("\"error\"");
    }

    @Test
    void testDeserialization_unknownFieldsIgnored() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"test\",\"unknownField\":true}";

        JsonRpcMessage msg = AcpTransport.MAPPER.readValue(json, JsonRpcMessage.class);

        assertThat(msg.id).isEqualTo(1);
        assertThat(msg.method).isEqualTo("test");
    }

    @Test
    void testResponse_resultCanBeComplex() throws Exception {
        Map<String, Object> resultData = Map.of(
                "protocolVersion", "1",
                "capabilities", Map.of("streaming", true)
        );
        JsonRpcMessage msg = JsonRpcMessage.response(1, resultData);

        assertThat(msg.result.has("protocolVersion")).isTrue();
        assertThat(msg.result.get("protocolVersion").asText()).isEqualTo("1");
        assertThat(msg.result.has("capabilities")).isTrue();
    }

    @Test
    void testJsonRpcError_defaultConstructor() {
        JsonRpcMessage.JsonRpcError error = new JsonRpcMessage.JsonRpcError();
        assertThat(error.code).isEqualTo(0);
        assertThat(error.message).isNull();
    }
}
