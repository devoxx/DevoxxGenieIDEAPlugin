package com.devoxx.genie.service.acp.protocol;

import com.devoxx.genie.service.acp.protocol.exception.AcpConnectionException;
import com.devoxx.genie.service.acp.protocol.exception.AcpSessionException;
import com.devoxx.genie.service.acp.protocol.exception.AcpTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
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
        assertThatCode(() -> client.close()).doesNotThrowAnyException();
    }

    @Test
    void testClose_transportFailureDoesNotPropagateAndCleansState() throws Exception {
        ThrowingCloseTransport throwingCloseTransport = new ThrowingCloseTransport();
        client = new AcpClient(throwingCloseTransport, text -> {}, null, 30);

        setPrivateField(client, "sessionId", "s1");

        assertThatCode(() -> client.close()).doesNotThrowAnyException();
        assertThatCode(() -> client.close()).doesNotThrowAnyException();

        assertThat(getPrivateField(client, "sessionId")).isNull();
        assertThat(getPrivateField(client, "outputConsumer")).isNull();
        assertThat(throwingCloseTransport.closeCalls).isEqualTo(1);
    }

    @Test
    void testSendPrompt_withoutSession_throwsAcpSessionException() {
        client = new AcpClient(text -> {});

        assertThatThrownBy(() -> client.sendPrompt("hello"))
                .isInstanceOf(AcpSessionException.class)
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
        assertThatCode(() -> client.initialize()).doesNotThrowAnyException();
    }

    @Test
    void testInitialize_errorResponse_throwsAcpConnectionException() {
        // Create a script that responds with an error
        String script = "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32600,\"message\":\"Bad request\"}}'";

        client = new AcpClient(text -> {});

        assertThatThrownBy(() -> {
            client.start(null, "sh", "-c", script);
            client.initialize();
        }).isInstanceOf(AcpConnectionException.class)
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
        assertThatCode(() -> {
            client.initialize();
            client.createSession("/tmp/test");
        }).doesNotThrowAnyException();
    }

    @Test
    void testCreateSession_errorResponse_throwsAcpSessionException() {
        String script = String.join("; ",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":2,\"error\":{\"code\":-1,\"message\":\"Session failed\"}}'"
        );

        client = new AcpClient(text -> {});

        assertThatThrownBy(() -> {
            client.start(null, "sh", "-c", script);
            client.initialize();
            client.createSession("/tmp/test");
        }).isInstanceOf(AcpSessionException.class)
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
                "read line; echo '{\"jsonrpc\":\"2.0\",\"method\":\"session/update\",\"params\":{\"sessionId\":\"s1\",\"update\":{\"sessionUpdate\":\"thought_chunk\",\"content\":{\"type\":\"text\",\"text\":\"thinking\"}}}}'",
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

    @Test
    void testInitialize_alreadyInitialized_throwsAcpConnectionException() throws Exception {
        // First initialize succeeds
        String script = String.join("; ",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'",
                "read line; sleep 30"
        );

        client = new AcpClient(text -> {});
        client.start(null, "sh", "-c", script);
        client.initialize();

        // Second initialize should fail with state validation
        assertThatThrownBy(() -> client.initialize())
                .isInstanceOf(AcpConnectionException.class)
                .hasMessageContaining("Already initialized");
    }

    @Test
    void testCreateSession_withoutInitialize_throwsAcpSessionException() throws Exception {
        String script = "read line; sleep 30";

        client = new AcpClient(text -> {});
        client.start(null, "sh", "-c", script);

        // createSession without initialize should fail
        assertThatThrownBy(() -> client.createSession("/tmp/test"))
                .isInstanceOf(AcpSessionException.class)
                .hasMessageContaining("Not initialized");
    }

    @Test
    void testIsInitialized_reflectsLifecycleState() throws Exception {
        String script = "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'; sleep 30";

        client = new AcpClient(text -> {});
        assertThat(client.isInitialized()).isFalse();

        client.start(null, "sh", "-c", script);
        client.initialize();
        assertThat(client.isInitialized()).isTrue();

        client.close();
        assertThat(client.isInitialized()).isFalse();
    }

    @Test
    void testIsRunning_reflectsProcessState() throws Exception {
        String script = "sleep 30";

        client = new AcpClient(text -> {});
        assertThat(client.isRunning()).isFalse();

        client.start(null, "sh", "-c", script);
        assertThat(client.isRunning()).isTrue();

        client.close();
        assertThat(client.isRunning()).isFalse();
    }

    @Test
    void testPing_success_returnsTrue() throws Exception {
        String script = "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}'";

        client = new AcpClient(text -> {});
        client.start(null, "sh", "-c", script);

        assertThat(client.ping()).isTrue();
    }

    @Test
    void testPing_errorResponseStillReturnsTrue() throws Exception {
        String script = "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}'";

        client = new AcpClient(text -> {});
        client.start(null, "sh", "-c", script);

        assertThat(client.ping()).isTrue();
    }

    @Test
    void testPing_returnsFalseWhenTransportNotRunning() {
        client = new AcpClient(text -> {});
        assertThat(client.ping()).isFalse();
    }

    @Test
    void testPing_timeout_returnsFalse() throws Exception {
        String script = "read line; sleep 30";

        client = new AcpClient(text -> {}, 1);
        client.start(null, "sh", "-c", script);

        assertThat(client.ping()).isFalse();
    }

    @Test
    void testConstants_haveExpectedValues() {
        assertThat(AcpClient.PROTOCOL_VERSION).isEqualTo(1);
        assertThat(AcpClient.CLIENT_NAME).isEqualTo("DevoxxGenie");
        assertThat(AcpClient.CLIENT_VERSION).isEqualTo("1.0.0");
        assertThat(AcpTransport.DEFAULT_REQUEST_TIMEOUT_SECONDS).isEqualTo(120);
        assertThat(AcpTransport.SHUTDOWN_WAIT_SECONDS).isEqualTo(5);
    }

    @Test
    void testConstructor_defaultTimeout_usesTransportDefault() {
        client = new AcpClient(text -> {});
        // Default constructor should work without error
        assertThat(client).isNotNull();
    }

    @Test
    void testConstructor_customTimeout() {
        client = new AcpClient(text -> {}, 30);
        assertThat(client).isNotNull();
    }

    @Test
    void testCustomTimeout_timesOutFaster() throws Exception {
        // Script that never responds — the client should time out
        String script = "read line; sleep 30";

        client = new AcpClient(text -> {}, 1); // 1-second timeout
        client.start(null, "sh", "-c", script);

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.initialize())
                .isInstanceOf(AcpConnectionException.class)
                .hasCauseInstanceOf(AcpTimeoutException.class);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        // Should time out in ~1 second, definitely less than the default 120s
        assertThat(elapsedMs).isLessThan(10_000);
    }

    @Test
    void testCustomTimeout_fullFlowWithCustomTimeout() throws Exception {
        String script = String.join("; ",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"sessionId\":\"s1\"}}'",
                "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{}}'"
        );

        // Custom timeout of 10 seconds — should still succeed for fast responses
        client = new AcpClient(text -> {}, 10);
        client.start(null, "sh", "-c", script);
        assertThatCode(() -> client.initialize()).doesNotThrowAnyException();
        assertThatCode(() -> client.createSession("/tmp/test")).doesNotThrowAnyException();
        assertThatCode(() -> client.sendPrompt("hello")).doesNotThrowAnyException();
    }

    @Test
    void testBuilder_defaults() throws Exception {
        client = AcpClient.builder().build();

        assertThat(client).isNotNull();
        assertThat(getPrivateField(client, "requestTimeoutSeconds"))
                .isEqualTo(AcpTransport.DEFAULT_REQUEST_TIMEOUT_SECONDS);
        assertThat(getPrivateField(client, "outputConsumer")).isNotNull();
    }

    @Test
    void testBuilder_customConfiguration() throws Exception {
        ThrowingCloseTransport transport = new ThrowingCloseTransport();
        List<String> output = new ArrayList<>();

        client = AcpClient.builder()
                .transport(transport)
                .outputConsumer(output::add)
                .requestTimeoutSeconds(7)
                .build();

        assertThat(getPrivateField(client, "requestTimeoutSeconds")).isEqualTo(7L);
        assertThat(client.getTransport()).isSameAs(transport);

        assertThatCode(() -> client.close()).doesNotThrowAnyException();
        assertThat(transport.closeCalls).isEqualTo(1);
    }

    @Test
    void testBuilder_invalidTimeout_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> AcpClient.builder().requestTimeoutSeconds(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void testBuilder_nullOutputConsumer_throwsNullPointerException() {
        assertThatThrownBy(() -> AcpClient.builder().outputConsumer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("outputConsumer");
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class ThrowingCloseTransport extends AcpTransport {
        private int closeCalls;

        @Override
        public void close() {
            closeCalls++;
            throw new RuntimeException("close failure");
        }
    }
}
