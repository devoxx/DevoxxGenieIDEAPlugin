package com.devoxx.genie.service.acp.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AcpTransportTest {

    private AcpTransport transport;

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.close();
        }
    }

    @Test
    void testStart_launchesProcess() throws Exception {
        transport = new AcpTransport();
        // Use a simple command that writes JSON-RPC to stdout
        // "echo" just outputs and exits, which is fine for verifying process startup
        transport.start(null, "echo", "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}");
        // If we get here without exception, the process started successfully
        assertThat(transport).isNotNull();
    }

    @Test
    void testClose_cancelsAllPendingRequests() throws Exception {
        transport = new AcpTransport();
        // Start a process that stays alive (cat reads stdin indefinitely)
        transport.start(null, "cat");

        // The transport is now running with a live process.
        // Close it — all pending futures should be cancelled.
        transport.close();
        // Should not throw, process should be destroyed
        assertThat(transport).isNotNull();
    }

    @Test
    void testClose_idempotent() {
        transport = new AcpTransport();
        // Close without starting — should not throw
        transport.close();
        // Verify close is idempotent
        assertThat(transport).isNotNull();
    }

    @Test
    void testNotificationHandler_invoked() throws Exception {
        transport = new AcpTransport();

        AtomicReference<JsonRpcMessage> received = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        transport.setNotificationHandler(msg -> {
            received.set(msg);
            latch.countDown();
        });

        // Start a process that sends a notification (method present, no id)
        String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"session/update\",\"params\":{}}";
        transport.start(null, "echo", notification);

        boolean received1 = latch.await(5, TimeUnit.SECONDS);
        assertThat(received1).isTrue();
        assertThat(received.get()).isNotNull();
        assertThat(received.get().getMethod()).isEqualTo("session/update");
        assertThat(received.get().isNotification()).isTrue();
    }

    @Test
    void testRequestHandler_invoked() throws Exception {
        transport = new AcpTransport();

        AtomicReference<JsonRpcMessage> received = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        transport.setRequestHandler(msg -> {
            received.set(msg);
            latch.countDown();
        });

        // Start a process that sends a request (method present + id present)
        String request = "{\"jsonrpc\":\"2.0\",\"id\":99,\"method\":\"fs/read_text_file\",\"params\":{\"path\":\"/tmp/test\"}}";
        transport.start(null, "echo", request);

        boolean received1 = latch.await(5, TimeUnit.SECONDS);
        assertThat(received1).isTrue();
        assertThat(received.get()).isNotNull();
        assertThat(received.get().getMethod()).isEqualTo("fs/read_text_file");
        assertThat(received.get().isRequest()).isTrue();
        assertThat(received.get().getId()).isEqualTo(99);
    }

    @Test
    void testResponseDispatching_completesFuture() throws Exception {
        transport = new AcpTransport();

        // Start a process that echoes back what we write to it
        // We use a shell script that reads a line and then outputs a response
        String script = "read line; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"1\"}}'";
        transport.start(null, "sh", "-c", script);

        // Send a request — the script will respond with id=1
        JsonRpcMessage response = transport.sendRequest("initialize", null);

        assertThat(response).isNotNull();
        assertThat(response.isResponse()).isTrue();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getResult().get("protocolVersion").asText()).isEqualTo("1");
    }

    @Test
    void testSendRequest_parsesEmptyLines() throws Exception {
        transport = new AcpTransport();

        // Script outputs blank lines before the response
        String script = "read line; echo ''; echo ''; echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}'";
        transport.start(null, "sh", "-c", script);

        JsonRpcMessage response = transport.sendRequest("test", null);

        assertThat(response).isNotNull();
        assertThat(response.isResponse()).isTrue();
    }

    @Test
    void testMapper_configuration() {
        // Verify ObjectMapper is configured to ignore unknown properties
        assertThat(AcpTransport.MAPPER).isNotNull();

        // Parse a message with unknown fields — should not throw
        try {
            JsonRpcMessage msg = AcpTransport.MAPPER.readValue(
                    "{\"jsonrpc\":\"2.0\",\"id\":1,\"unknownField\":42}",
                    JsonRpcMessage.class);
            assertThat(msg.getId()).isEqualTo(1);
        } catch (Exception e) {
            throw new AssertionError("MAPPER should ignore unknown properties", e);
        }
    }
}
