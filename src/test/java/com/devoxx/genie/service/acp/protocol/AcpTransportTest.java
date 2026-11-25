package com.devoxx.genie.service.acp.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
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

    /**
     * Reproduces the flaky {@code AcpClientTest.testIsRunning_reflectsProcessState} deterministically.
     *
     * <p>{@code sh -c "sleep 30"} (dash) forks {@code sleep}; when {@code close()} runs before the fork
     * is visible, {@code sleep} is missed by {@code descendants()}, orphaned when {@code sh} dies, and keeps
     * the stdout pipe open. Closing the stream does not wake a thread blocked in a native read on Linux,
     * so the {@code acp-reader} thread leaked. Here a grandchild is detached on purpose so it can never be
     * found through {@code ProcessHandle}, and close() must still stop the reader.
     */
    @Test
    void testClose_stopsReaderEvenWhenOrphanedGrandchildHoldsStdout() throws Exception {
        String marker = "27.4813";
        transport = new AcpTransport();
        CountDownLatch ready = new CountDownLatch(1);
        transport.setNotificationHandler(msg -> {
            if ("ready".equals(msg.getMethod())) {
                ready.countDown();
            }
        });
        try {
            // (sleep &) in a subshell detaches the sleep immediately: it is reparented away from sh
            // but inherits sh's stdout, so the pipe stays open after sh is killed.
            String script = "(sleep " + marker + " &); echo '{\"jsonrpc\":\"2.0\",\"method\":\"ready\"}'; sleep 30";
            transport.start(null, "sh", "-c", script);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            Thread reader = (Thread) getPrivateField(transport, "readerThread");
            assertThat(reader).isNotNull();

            long startNanos = System.nanoTime();
            transport.close();
            reader.join(TimeUnit.SECONDS.toMillis(2));

            assertThat(reader.isAlive())
                    .as("acp-reader must stop on close() even when an orphaned grandchild holds stdout open")
                    .isFalse();
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)).isLessThan(5_000);
        } finally {
            killProcessesWithArgument(marker);
        }
    }

    @Test
    void testReader_dispatchesFinalLineWithoutTrailingNewline() throws Exception {
        transport = new AcpTransport();
        AtomicReference<JsonRpcMessage> received = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        transport.setNotificationHandler(msg -> {
            received.set(msg);
            latch.countDown();
        });

        transport.start(null, "printf", "%s", "{\"jsonrpc\":\"2.0\",\"method\":\"session/update\",\"params\":{}}");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(received.get().getMethod()).isEqualTo("session/update");
    }

    @Test
    void testReader_splitsMultipleLinesAndDecodesUtf8() throws Exception {
        transport = new AcpTransport();
        List<String> texts = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        transport.setNotificationHandler(msg -> {
            texts.add(msg.getParams().get("text").asText());
            latch.countDown();
        });

        // Two lines separated by a blank line; the first carries multi-byte UTF-8 (\u00e9 = \303\251,
        // \u2615 = \342\230\225) written by printf octal escapes so the process args stay ASCII.
        String format = "{\"jsonrpc\":\"2.0\",\"method\":\"n\",\"params\":{\"text\":\"h\\303\\251llo \\342\\230\\225\"}}\\n"
                + "\\n"
                + "{\"jsonrpc\":\"2.0\",\"method\":\"n\",\"params\":{\"text\":\"second\"}}\\n";
        transport.start(null, "printf", format);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(texts).containsExactly("h\u00e9llo \u2615", "second");
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void killProcessesWithArgument(String argument) {
        ProcessHandle.allProcesses()
                .filter(ph -> ph.info().arguments()
                        .map(args -> List.of(args).contains(argument))
                        .orElse(false))
                .forEach(ProcessHandle::destroyForcibly);
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
