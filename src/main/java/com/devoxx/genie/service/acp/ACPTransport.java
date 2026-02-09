package com.devoxx.genie.service.acp;

import com.devoxx.genie.model.acp.ACPMessage;
import com.google.gson.JsonElement;
import com.intellij.openapi.util.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Low-level JSON-RPC 2.0 transport over stdio for ACP communication.
 * Spawns an agent subprocess, writes requests to stdin, reads responses/notifications from stdout.
 */
@Slf4j
public class ACPTransport implements Closeable {

    private final AtomicInteger messageIdCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, CompletableFuture<ACPMessage>> pendingRequests = new ConcurrentHashMap<>();
    private final List<Consumer<ACPMessage>> notificationListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<ACPMessage>> requestListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Process process;
    private BufferedWriter writer;
    private Thread readerThread;

    /**
     * Start the agent subprocess and begin reading its output.
     *
     * @param command     The agent command (e.g., "/usr/local/bin/gemini")
     * @param args        Command arguments (e.g., ["--experimental-acp"])
     * @param env         Additional environment variables
     * @param workingDir  Working directory for the process (can be null)
     */
    public void start(@NotNull String command,
                      @NotNull List<String> args,
                      @NotNull Map<String, String> env,
                      @Nullable String workingDir) throws IOException {
        if (running.get()) {
            throw new IllegalStateException("Transport is already running");
        }

        List<String> fullCommand = buildCommand(command, args);
        log.info("Starting ACP agent: {}", fullCommand);

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.redirectErrorStream(false);

        // Merge environment
        Map<String, String> processEnv = pb.environment();
        processEnv.putAll(env);

        // Add command directory to PATH
        String commandDir = new File(command).getParent();
        if (commandDir != null) {
            String pathSep = SystemInfo.isWindows ? ";" : ":";
            String currentPath = processEnv.getOrDefault("PATH", "");
            processEnv.put("PATH", commandDir + pathSep + currentPath);
        }

        if (workingDir != null) {
            pb.directory(new File(workingDir));
        }

        process = pb.start();
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        running.set(true);

        // Start background reader thread for stdout
        readerThread = new Thread(this::readLoop, "ACP-Transport-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        // Start stderr reader to log agent errors
        Thread stderrThread = new Thread(() -> readStderr(process.getErrorStream()), "ACP-Transport-Stderr");
        stderrThread.setDaemon(true);
        stderrThread.start();

        log.info("ACP transport started successfully");
    }

    /**
     * Send a JSON-RPC request and return a future for the response.
     */
    public CompletableFuture<ACPMessage> sendRequest(@NotNull String method, @Nullable JsonElement params) {
        int id = messageIdCounter.incrementAndGet();
        ACPMessage request = ACPMessage.request(id, method, params);

        CompletableFuture<ACPMessage> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            writeMessage(request);
        } catch (IOException e) {
            pendingRequests.remove(id);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Send a JSON-RPC notification (no response expected).
     */
    public void sendNotification(@NotNull String method, @Nullable JsonElement params) throws IOException {
        ACPMessage notification = ACPMessage.notification(method, params);
        writeMessage(notification);
    }

    /**
     * Send a JSON-RPC response (for answering agent requests like request_permission).
     */
    public void sendResponse(int id, @Nullable JsonElement result) throws IOException {
        ACPMessage response = ACPMessage.response(id, result);
        writeMessage(response);
    }

    /**
     * Register a listener for incoming notifications from the agent.
     */
    public void onNotification(@NotNull Consumer<ACPMessage> listener) {
        notificationListeners.add(listener);
    }

    /**
     * Register a listener for incoming requests from the agent (e.g., request_permission).
     */
    public void onRequest(@NotNull Consumer<ACPMessage> listener) {
        requestListeners.add(listener);
    }

    public boolean isRunning() {
        return running.get() && process != null && process.isAlive();
    }

    @Override
    public void close() {
        running.set(false);

        // Complete all pending requests with cancellation
        pendingRequests.forEach((id, future) -> future.cancel(true));
        pendingRequests.clear();

        // Close writer
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                log.debug("Error closing ACP transport writer", e);
            }
        }

        // Destroy process
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }

        // Interrupt reader thread
        if (readerThread != null) {
            readerThread.interrupt();
        }

        log.info("ACP transport closed");
    }

    private synchronized void writeMessage(@NotNull ACPMessage message) throws IOException {
        if (!running.get()) {
            throw new IOException("Transport is not running");
        }
        String json = message.toJson();
        log.debug("ACP → {}", json);
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                log.debug("ACP ← {}", line);
                try {
                    ACPMessage message = ACPMessage.fromJson(line);
                    dispatchMessage(message);
                } catch (Exception e) {
                    log.warn("Failed to parse ACP message: {}", line, e);
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                log.error("Error reading from ACP agent stdout", e);
            }
        } finally {
            running.set(false);
            // Complete all pending requests with error
            pendingRequests.forEach((id, future) ->
                    future.completeExceptionally(new IOException("ACP transport closed")));
            pendingRequests.clear();
        }
    }

    private void dispatchMessage(@NotNull ACPMessage message) {
        if (message.isResponse()) {
            // Response to a pending request
            CompletableFuture<ACPMessage> future = pendingRequests.remove(message.getId());
            if (future != null) {
                if (message.isError()) {
                    future.completeExceptionally(
                            new ACPException("ACP error: " + message.getError()));
                } else {
                    future.complete(message);
                }
            } else {
                log.warn("Received response for unknown request id: {}", message.getId());
            }
        } else if (message.isNotification()) {
            // Notification from agent
            for (Consumer<ACPMessage> listener : notificationListeners) {
                try {
                    listener.accept(message);
                } catch (Exception e) {
                    log.error("Error in ACP notification listener", e);
                }
            }
        } else if (message.isRequest()) {
            // Request from agent (e.g., session/request_permission)
            for (Consumer<ACPMessage> listener : requestListeners) {
                try {
                    listener.accept(message);
                } catch (Exception e) {
                    log.error("Error in ACP request listener", e);
                }
            }
        }
    }

    private void readStderr(InputStream stderr) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                log.warn("ACP agent stderr: {}", line);
            }
        } catch (IOException e) {
            if (running.get()) {
                log.debug("Error reading ACP agent stderr", e);
            }
        }
    }

    /**
     * Build platform-specific command list.
     * Mirrors MCPExecutionService.createMCPCommand() pattern.
     */
    private static @NotNull List<String> buildCommand(@NotNull String command, @NotNull List<String> args) {
        List<String> fullArgs = new ArrayList<>();
        fullArgs.add(command);
        fullArgs.addAll(args);

        List<String> platformCommand = new ArrayList<>();
        if (SystemInfo.isWindows) {
            platformCommand.add("cmd.exe");
            platformCommand.add("/c");
        } else {
            platformCommand.add("/bin/bash");
            platformCommand.add("-c");
        }

        String cmdString = fullArgs.stream()
                .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                .collect(java.util.stream.Collectors.joining(" "));
        platformCommand.add(cmdString);

        return platformCommand;
    }

    /**
     * Custom exception for ACP protocol errors.
     */
    public static class ACPException extends RuntimeException {
        public ACPException(String message) {
            super(message);
        }

        public ACPException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
