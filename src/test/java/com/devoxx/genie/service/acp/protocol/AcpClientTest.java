package com.devoxx.genie.service.acp.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcpClientTest {

    private AcpClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testConstructor_setsUpHandlers() {
        List<String> output = new ArrayList<>();
        client = new AcpClient(output::add);
        // Should not throw — handlers are wired internally
        assertThat(client).isNotNull();
    }

    @Test
    void testClose_idempotent() {
        client = new AcpClient(text -> {});
        client.close();
        client.close();
        // Should not throw
    }

    @Test
    void testSendPrompt_withoutSession_throwsIllegalState() {
        client = new AcpClient(text -> {});

        assertThatThrownBy(() -> client.sendPrompt("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active session");
    }

    @Test
    void testInitialize_successfulHandshake() throws Exception {
        // Create a script that simulates an ACP server:
        // 1. Reads the initialize request
        // 2. Responds with a valid initialize result
        String script = "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'";

        client = new AcpClient(text -> {});
        client.start(null, "sh", "-c", script);
        client.initialize();
        // If we get here without exception, initialize succeeded
    }

    @Test
    void testInitialize_errorResponse_throws() {
        // Create a script that responds with an error
        String script = "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32600,\"message\":\"Bad request\"}}'";

        client = new AcpClient(text -> {});

        assertThatThrownBy(() -> {
            client.start(null, "sh", "-c", script);
            client.initialize();
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Initialize failed");
    }

    @Test
    void testCreateSession_successfulHandshake() throws Exception {
        // Script handles both initialize and session/new requests
        String script = String.join("; ",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"sessionId\":\"test-session-123\"}}'"
        );

        client = new AcpClient(text -> {});
        client.start(null, "sh", "-c", script);
        client.initialize();
        client.createSession("/tmp/test");
        // If we get here without exception, session creation succeeded
    }

    @Test
    void testCreateSession_errorResponse_throws() {
        String script = String.join("; ",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":2,\"error\":{\"code\":-1,\"message\":\"Session failed\"}}'"
        );

        client = new AcpClient(text -> {});

        assertThatThrownBy(() -> {
            client.start(null, "sh", "-c", script);
            client.initialize();
            client.createSession("/tmp/test");
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("session/new failed");
    }

    @Test
    void testFullFlow_initializeCreateSessionSendPrompt() throws Exception {
        // Full ACP flow: initialize → session/new → session/prompt
        // Also sends a session/update notification with an agent_message_chunk
        String script = String.join("; ",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"sessionId\":\"s1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"method\":\"session/update\",\"params\":{\"sessionId\":\"s1\",\"update\":{\"sessionUpdate\":\"agent_message_chunk\",\"content\":{\"type\":\"text\",\"text\":\"Hello!\"}}}}'",
                "sleep 0.1",
                "echo '{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{}}'"
        );

        List<String> receivedText = new ArrayList<>();
        CountDownLatch textLatch = new CountDownLatch(1);

        client = new AcpClient(text -> {
            receivedText.add(text);
            textLatch.countDown();
        });
        client.start(null, "sh", "-c", script);
        client.initialize();
        client.createSession("/tmp/test");
        client.sendPrompt("Say hello");

        // Wait for the notification to be processed
        boolean received = textLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(receivedText).containsExactly("Hello!");
    }

    @Test
    void testNotification_thoughtChunksFiltered() throws Exception {
        // Thought chunks should be filtered out, only agent_message_chunk with text type passes through
        String script = String.join("; ",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"sessionId\":\"s1\"}}'",
                // Thought chunk — should be filtered
                "read line; echo '{\"jsonrpc\":\"2.0\",\"method\":\"session/update\",\"params\":{\"sessionId\":\"s1\",\"update\":{\"sessionUpdate\":\"thought_chunk\",\"content\":{\"type\":\"text\",\"text\":\"thinking...\"}}}}'",
                // Agent message chunk — should pass through
                "echo '{\"jsonrpc\":\"2.0\",\"method\":\"session/update\",\"params\":{\"sessionId\":\"s1\",\"update\":{\"sessionUpdate\":\"agent_message_chunk\",\"content\":{\"type\":\"text\",\"text\":\"Result\"}}}}'",
                "sleep 0.1",
                "echo '{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{}}'"
        );

        List<String> receivedText = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        client = new AcpClient(text -> {
            receivedText.add(text);
            latch.countDown();
        });
        client.start(null, "sh", "-c", script);
        client.initialize();
        client.createSession("/tmp/test");
        client.sendPrompt("test");

        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        // Only "Result" should be received, not "thinking..."
        assertThat(receivedText).containsExactly("Result");
    }
}
