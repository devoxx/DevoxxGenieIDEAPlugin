package com.devoxx.genie.service.acp.protocol;

import com.devoxx.genie.service.acp.protocol.exception.AcpRequestException;
import com.devoxx.genie.service.acp.protocol.exception.AcpTimeoutException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.util.EnvironmentUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * JSON-RPC 2.0 transport layer for the Agent Communication Protocol (ACP).
 *
 * <p>Communicates with an ACP agent process over stdio, sending and receiving
 * newline-delimited JSON-RPC messages. Incoming messages are dispatched to:
 * <ul>
 *   <li>A {@code notificationHandler} for server-initiated notifications (no {@code id})</li>
 *   <li>A {@code requestHandler} for server-initiated requests (has {@code id} and {@code method})</li>
 *   <li>Pending request futures for responses to client-initiated requests</li>
 * </ul>
 *
 * <p>This class is thread-safe. Outgoing messages are serialized through a
 * synchronized write path, and pending request tracking uses {@link ConcurrentHashMap}.
 *
 * @see AcpClient
 * @see JsonRpcMessage
 */
@Slf4j
public class AcpTransport implements AutoCloseable {

    /** Shared Jackson ObjectMapper configured for ACP JSON-RPC serialization. */
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    /** Default timeout in seconds for waiting on a JSON-RPC response. */
    public static final long DEFAULT_REQUEST_TIMEOUT_SECONDS = 120;

    /** Maximum time in seconds to wait for the agent process to shut down gracefully. */
    public static final long SHUTDOWN_WAIT_SECONDS = 5;

    /** Upper bound in milliseconds for the reader's idle back-off between polls of the agent's stdout. */
    static final long READER_MAX_IDLE_MILLIS = 20;

    /** How long close() lets the agent's descendants exit after SIGTERM before killing them forcibly. */
    static final long DESCENDANT_GRACE_MILLIS = 500;

    @Getter
    private Process process;
    private BufferedWriter writer;
    private Thread readerThread;
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonRpcMessage>> pendingRequests = new ConcurrentHashMap<>();

    /**
     *  Sets the handler for server-initiated notifications (messages with no).
     */
    @Setter
    private Consumer<JsonRpcMessage> notificationHandler;

    /**
     *  Sets the handler for server-initiated requests (messages with both and).
     */
    @Setter
    private Consumer<JsonRpcMessage> requestHandler;
    private volatile boolean running = true;

    /**
     * Starts the agent subprocess and begins reading its stdout on a daemon thread.
     *
     * @param cwd     working directory for the process, or {@code null} to inherit
     * @param command the command and arguments to launch (e.g. {@code "claude", "--acp"})
     * @throws IOException if the process cannot be started
     */
    public void start(File cwd, String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        if (cwd != null) {
            pb.directory(cwd);
        }
        pb.environment().putAll(EnvironmentUtil.getEnvironmentMap());
        process = pb.start();

        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        Process started = process;
        this.readerThread = new Thread(() -> readLoop(started), "acp-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    /**
     * Reads newline-delimited JSON-RPC messages from the agent's stdout until {@link #close()} is called
     * or the agent process has exited and its output is drained.
     *
     * <p>The loop polls {@link InputStream#available()} rather than blocking in {@code read()}. A thread
     * blocked in a native pipe read can't be woken by closing the stream or by interrupting it: the read
     * returns only once every process holding the pipe's write end has exited. A grandchild the agent
     * spawned can outlive the kill in {@link #close()} and keep the pipe open: one that was detached, or
     * one forked after the {@code descendants()} snapshot, gets reparented and can't be found any more.
     * A blocking reader would then leak until that process exits on its own.
     */
    private void readLoop(Process proc) {
        InputStream in = proc.getInputStream();
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long idleMillis = 1;
        try {
            while (running) {
                int available = in.available();
                if (available > 0) {
                    int read = in.read(buffer, 0, Math.min(available, buffer.length));
                    if (read < 0) {
                        break;
                    }
                    dispatchLines(buffer, read, pending);
                    idleMillis = 1;
                } else if (!proc.isAlive()) {
                    // The agent has exited and everything it wrote has been read; don't wait for EOF,
                    // which never comes while an orphaned descendant still holds the pipe.
                    break;
                } else {
                    Thread.sleep(idleMillis);
                    idleMillis = Math.min(idleMillis * 2, READER_MAX_IDLE_MILLIS);
                }
            }
            if (running && pending.size() > 0) {
                // Like BufferedReader.readLine(), deliver a final line that has no trailing newline.
                handleLine(pending.toString(StandardCharsets.UTF_8));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            handleReadError(e);
        }
    }

    /** Splits {@code buffer[0, length)} on '\n', dispatching each complete line and keeping the remainder. */
    private void dispatchLines(byte[] buffer, int length, ByteArrayOutputStream pending) {
        int lineStart = 0;
        for (int i = 0; i < length; i++) {
            if (buffer[i] == '\n') {
                pending.write(buffer, lineStart, i - lineStart);
                handleLine(pending.toString(StandardCharsets.UTF_8));
                pending.reset();
                lineStart = i + 1;
            }
        }
        pending.write(buffer, lineStart, length - lineStart);
    }

    private void handleLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        JsonRpcMessage msg = parseMessage(trimmed);
        if (msg == null) {
            return;
        }

        dispatchMessage(msg);
    }

    private JsonRpcMessage parseMessage(String line) {
        try {
            return MAPPER.readValue(line, JsonRpcMessage.class);
        } catch (Exception e) {
            log.warn("[ACP] Failed to parse: {}", line);
            return null;
        }
    }

    private void dispatchMessage(JsonRpcMessage msg) {
        if (msg.isResponse()) {
            completePendingRequest(msg);
            return;
        }

        if (msg.isNotification()) {
            handleNotification(msg);
            return;
        }

        if (msg.isRequest() && requestHandler != null) {
            requestHandler.accept(msg);
        }
    }

    private void completePendingRequest(JsonRpcMessage msg) {
        CompletableFuture<JsonRpcMessage> future = pendingRequests.remove(msg.getId());
        if (future != null) {
            future.complete(msg);
        }
    }

    private void handleNotification(JsonRpcMessage msg) {
        if (notificationHandler != null) {
            notificationHandler.accept(msg);
        }
    }

    private void handleReadError(IOException e) {
        if (running) {
            log.warn("[ACP] Reader error: {}", e.getMessage());
        }
    }

    /**
     * Sends a JSON-RPC request using the {@link #DEFAULT_REQUEST_TIMEOUT_SECONDS default timeout}.
     *
     * @param method the JSON-RPC method name (e.g. {@code "initialize"}, {@code "session/new"})
     * @param params the request parameters, serialized to JSON via Jackson
     * @return the response message (may contain either {@code result} or {@code error})
     * @throws IOException           if writing to the process stdin fails
     * @throws InterruptedException  if the calling thread is interrupted while waiting
     * @throws AcpTimeoutException   if no response is received within the default timeout
     * @throws AcpRequestException   if the response future completes exceptionally
     */
    public JsonRpcMessage sendRequest(String method, Object params)
            throws IOException, InterruptedException, AcpTimeoutException, AcpRequestException {
        return sendRequest(method, params, DEFAULT_REQUEST_TIMEOUT_SECONDS);
    }

    /**
     * Sends a JSON-RPC request and blocks until a response is received or the timeout expires.
     *
     * @param method         the JSON-RPC method name (e.g. {@code "initialize"}, {@code "session/new"})
     * @param params         the request parameters, serialized to JSON via Jackson
     * @param timeoutSeconds maximum time in seconds to wait for a response
     * @return the response message (may contain either {@code result} or {@code error})
     * @throws IOException           if writing to the process stdin fails
     * @throws InterruptedException  if the calling thread is interrupted while waiting
     * @throws AcpTimeoutException   if no response is received within the specified timeout
     * @throws AcpRequestException   if the response future completes exceptionally
     */
    public JsonRpcMessage sendRequest(String method, Object params, long timeoutSeconds)
            throws IOException, InterruptedException, AcpTimeoutException, AcpRequestException {
        int id = idCounter.getAndIncrement();
        JsonRpcMessage msg = JsonRpcMessage.request(id, method, params);
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            sendRaw(msg);
        } catch (IOException e) {
            pendingRequests.remove(id);
            throw e;
        }

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            pendingRequests.remove(id);
            Thread.currentThread().interrupt();
            throw e;
        } catch (TimeoutException e) {
            pendingRequests.remove(id);
            throw new AcpTimeoutException("Request timed out after " + timeoutSeconds + "s for method: " + method, e);
        } catch (ExecutionException e) {
            pendingRequests.remove(id);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new AcpRequestException("Request failed for method: " + method, cause);
        }
    }

    /**
     * Sends a successful JSON-RPC response for the given request ID.
     *
     * @param id     the request ID to respond to
     * @param result the result object, serialized to JSON via Jackson
     * @throws IOException if writing to the process stdin fails
     */
    public void sendResponse(int id, Object result) throws IOException {
        sendRaw(JsonRpcMessage.response(id, result));
    }

    /**
     * Sends a JSON-RPC error response for the given request ID.
     *
     * @param id      the request ID to respond to
     * @param code    the JSON-RPC error code
     * @param message a human-readable error description
     * @throws IOException if writing to the process stdin fails
     */
    public void sendErrorResponse(int id, int code, String message) throws IOException {
        sendRaw(JsonRpcMessage.errorResponse(id, code, message));
    }

    private synchronized void sendRaw(JsonRpcMessage msg) throws IOException {
        String json = MAPPER.writeValueAsString(msg);
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    /**
     * Stops the transport: cancels all pending requests, terminates the agent process (waiting up to
     * {@link #SHUTDOWN_WAIT_SECONDS} for a graceful exit before killing it forcibly) and its descendants
     * (allowed {@link #DESCENDANT_GRACE_MILLIS} after the agent exits), and stops the reader thread.
     */
    @Override
    public void close() {
        running = false;
        pendingRequests.forEach((id, future) -> future.cancel(true));
        pendingRequests.clear();
        if (process != null) {
            terminateProcessTree(process);
        }
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(TimeUnit.SECONDS.toMillis(SHUTDOWN_WAIT_SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            readerThread = null;
        }
    }

    private static void terminateProcessTree(Process root) {
        // Snapshot the descendants before killing the root: once it dies they are reparented and no longer
        // show up in root.descendants(), so a child that outlives SIGTERM could not be found again.
        List<ProcessHandle> descendants = snapshotDescendants(root);
        descendants.forEach(ph -> destroyQuietly(ph, false));
        root.destroy();
        // Release our end of the agent's stdout; the reader thread polls and does not depend on this.
        try {
            root.getInputStream().close();
        } catch (IOException ignored) {
            // best-effort
        }

        try {
            boolean rootExited = root.waitFor(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!rootExited) {
                descendants.addAll(snapshotDescendants(root));
            }
            // Give descendants a short grace rather than the full shutdown wait: one that has exited can
            // still read as alive (a zombie) until init reaps it, which in a container without an init
            // never happens, and close() must not stall on it.
            long graceDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DESCENDANT_GRACE_MILLIS);
            for (ProcessHandle ph : descendants) {
                waitForExit(ph, graceDeadline);
            }
            descendants.forEach(ph -> destroyQuietly(ph, true));
            if (!rootExited) {
                root.destroyForcibly();
                root.waitFor(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            descendants.addAll(snapshotDescendants(root));
            descendants.forEach(ph -> destroyQuietly(ph, true));
            root.destroyForcibly();
        }
    }

    private static List<ProcessHandle> snapshotDescendants(Process root) {
        try {
            return new ArrayList<>(root.descendants().toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Waits until {@code ph} has exited or {@code deadline} passes. Polls {@link ProcessHandle#isAlive()}
     * because {@link ProcessHandle#onExit()} on a process that is not our direct child polls at 300 ms+
     * intervals, which would make every close() take that long.
     */
    private static void waitForExit(ProcessHandle ph, long deadline) throws InterruptedException {
        while (ph.isAlive() && deadline - System.nanoTime() > 0) {
            Thread.sleep(10);
        }
    }

    /** Destroys {@code ph} if it is still alive; a no-op for a process that has exited or whose PID was reused. */
    private static void destroyQuietly(ProcessHandle ph, boolean forcibly) {
        try {
            if (ph.isAlive()) {
                if (forcibly) {
                    ph.destroyForcibly();
                } else {
                    ph.destroy();
                }
            }
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
