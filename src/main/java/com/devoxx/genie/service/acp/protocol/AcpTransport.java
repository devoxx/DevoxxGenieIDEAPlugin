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

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.readerThread = new Thread(() -> readLoop(reader), "acp-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    private void readLoop(BufferedReader reader) {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            handleReadError(e);
        }
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
     * Stops the transport by cancelling all pending requests, terminating the
     * agent process, and waiting up to {@link #SHUTDOWN_WAIT_SECONDS} for graceful shutdown.
     */
    @Override
    public void close() {
        running = false;
        pendingRequests.forEach((id, future) -> future.cancel(true));
        pendingRequests.clear();
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        if (readerThread != null) {
            try {
                readerThread.join(TimeUnit.SECONDS.toMillis(SHUTDOWN_WAIT_SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            readerThread = null;
        }
    }
}
