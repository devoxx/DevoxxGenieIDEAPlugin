package com.devoxx.genie.service.acp.protocol;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.util.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class AcpTransport implements AutoCloseable {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private Process process;
    private BufferedWriter writer;
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonRpcMessage>> pendingRequests = new ConcurrentHashMap<>();

    private Consumer<JsonRpcMessage> notificationHandler;
    private Consumer<JsonRpcMessage> requestHandler;
    private volatile boolean running = true;

    public void setNotificationHandler(Consumer<JsonRpcMessage> handler) {
        this.notificationHandler = handler;
    }

    public void setRequestHandler(Consumer<JsonRpcMessage> handler) {
        this.requestHandler = handler;
    }

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
        Thread readerThread = new Thread(() -> readLoop(reader), "acp-reader");
        readerThread.setDaemon(true);
        readerThread.start();
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
        CompletableFuture<JsonRpcMessage> future = pendingRequests.remove(msg.id);
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

    public JsonRpcMessage sendRequest(String method, Object params)
            throws IOException, InterruptedException, TimeoutException, AcpRequestException {
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
            return future.get(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            pendingRequests.remove(id);
            Thread.currentThread().interrupt();
            throw e;
        } catch (TimeoutException e) {
            pendingRequests.remove(id);
            throw e;
        } catch (ExecutionException e) {
            pendingRequests.remove(id);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new AcpRequestException("Request failed for method: " + method, cause);
        }
    }

    public void sendResponse(int id, Object result) throws IOException {
        sendRaw(JsonRpcMessage.response(id, result));
    }

    public void sendErrorResponse(int id, int code, String message) throws IOException {
        sendRaw(JsonRpcMessage.errorResponse(id, code, message));
    }

    private synchronized void sendRaw(JsonRpcMessage msg) throws IOException {
        String json = MAPPER.writeValueAsString(msg);
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() {
        running = false;
        pendingRequests.forEach((id, future) -> future.cancel(true));
        pendingRequests.clear();
        if (process != null) {
            process.destroy();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
