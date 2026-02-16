package com.devoxx.genie.service.acp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.devoxx.genie.service.acp.protocol.exception.AcpException;
import com.devoxx.genie.service.acp.protocol.exception.FsReadException;
import com.devoxx.genie.service.acp.protocol.exception.FsWriteException;
import com.devoxx.genie.service.acp.protocol.exception.TerminalCreateException;

/**
 * Handles agent-initiated JSON-RPC requests by dispatching them to the appropriate
 * local operation (filesystem access, permission grants, or terminal management).
 *
 * <p>Supported methods:
 * <ul>
 *   <li>{@code fs/read_text_file} — reads a file from the local filesystem</li>
 *   <li>{@code fs/write_text_file} — writes content to a file</li>
 *   <li>{@code session/request_permission} — auto-approves permission requests</li>
 *   <li>{@code terminal/create} — spawns a managed subprocess</li>
 *   <li>{@code terminal/output} — retrieves buffered terminal output</li>
 *   <li>{@code terminal/wait_for_exit} — waits for a terminal process to finish</li>
 *   <li>{@code terminal/release} — releases and destroys a terminal process</li>
 *   <li>{@code terminal/kill} — forcibly kills a terminal process</li>
 * </ul>
 *
 * @see AcpTransport
 */
@Slf4j
public class AgentRequestHandler {

    public static final String TERMINAL_ID = "terminalId";
    public static final String SUCCESS = "success";
    public static final String OUTPUT = "output";
    public static final String EXIT_CODE = "exitCode";
    public static final String TIMEOUT = "timeout";
    public static final String TIMED_OUT = "timedOut";

    private final AcpTransport transport;
    private final ConcurrentHashMap<String, ManagedProcess> terminals = new ConcurrentHashMap<>();

    /**
     * Creates a handler that sends responses through the given transport.
     *
     * @param transport the ACP transport used to send responses back to the agent
     */
    public AgentRequestHandler(AcpTransport transport) {
        this.transport = transport;
    }

    /**
     * Handles an incoming JSON-RPC request by dispatching to the appropriate method
     * handler and sending the result (or error) back through the transport.
     *
     * @param msg the incoming JSON-RPC request message
     */
    public void handle(JsonRpcMessage msg) {
        try {
            Object result = dispatch(msg.getMethod(), msg.getParams());
            transport.sendResponse(msg.getId(), result);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            try {
                transport.sendErrorResponse(msg.getId(), -32603, e.getMessage());
            } catch (IOException ex) {
                log.error("[ACP] Failed to send error response: {}", ex.getMessage(), ex);
            }
        }
    }

    private Object dispatch(String method, JsonNode params) throws AcpException, InterruptedException {
        return switch (method) {
            case "fs/read_text_file" -> handleFsRead(params);
            case "fs/write_text_file" -> handleFsWrite(params);
            case "session/request_permission" -> handleRequestPermission();
            case "terminal/create" -> handleTerminalCreate(params);
            case "terminal/output" -> handleTerminalOutput(params);
            case "terminal/wait_for_exit" -> handleTerminalWaitForExit(params);
            case "terminal/release" -> handleTerminalRelease(params);
            case "terminal/kill" -> handleTerminalKill(params);
            default -> throw new UnsupportedOperationException("Unknown method: " + method);
        };
    }

    private Object handleFsRead(JsonNode params) throws FsReadException {
        String path = params.get("path").asText();
        try {
            String content = Files.readString(Path.of(path));
            return Map.of("content", content);
        } catch (IOException e) {
            throw new FsReadException("Failed to read file: " + path, e);
        }
    }

    private Object handleFsWrite(JsonNode params) throws FsWriteException {
        String path = params.get("path").asText();
        String content = params.get("content").asText();
        Path filePath = Path.of(path);
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            return Map.of(SUCCESS, true);
        } catch (IOException e) {
            throw new FsWriteException("Failed to write file: " + path, e);
        }
    }

    private Object handleRequestPermission() {
        // Auto-approve all permissions
        return Map.of("granted", true);
    }

    private Object handleTerminalCreate(JsonNode params) throws TerminalCreateException {
        String terminalId = params.has(TERMINAL_ID) ? params.get(TERMINAL_ID).asText()
                : "term-" + System.currentTimeMillis();
        String command = params.get("command").asText();
        String cwd = params.has("cwd") ? params.get("cwd").asText() : System.getProperty("user.dir");

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(new File(cwd));
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            ManagedProcess mp = new ManagedProcess(proc, terminalId);
            terminals.put(terminalId, mp);

            Thread captureThread = new Thread(() -> captureOutput(mp), "term-" + terminalId);
            captureThread.setDaemon(true);
            captureThread.start();

            return Map.of(TERMINAL_ID, terminalId);
        } catch (IOException e) {
            throw new TerminalCreateException("Failed to create terminal: " + terminalId, e);
        }
    }

    private void captureOutput(ManagedProcess mp) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mp.process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                mp.appendOutput(line + "\n");
            }
        } catch (IOException e) {
            // Process ended
        }
    }

    private Object handleTerminalOutput(JsonNode params) {
        String terminalId = params.get(TERMINAL_ID).asText();
        ManagedProcess mp = terminals.get(terminalId);
        if (mp == null) {
            return Map.of(OUTPUT, "", "error", "Unknown terminal: " + terminalId);
        }
        String output = mp.consumeOutput();
        return Map.of(OUTPUT, output);
    }

    private Object handleTerminalWaitForExit(JsonNode params) throws InterruptedException {
        String terminalId = params.get(TERMINAL_ID).asText();
        int timeoutMs = params.has(TIMEOUT) ? params.get(TIMEOUT).asInt() : 30000;
        ManagedProcess mp = terminals.get(terminalId);
        if (mp == null) {
            return Map.of(EXIT_CODE, -1, "error", "Unknown terminal: " + terminalId);
        }
        boolean finished = mp.process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (finished) {
            int exitCode = mp.process.exitValue();
            return Map.of(EXIT_CODE, exitCode, OUTPUT, mp.consumeOutput());
        } else {
            return Map.of(EXIT_CODE, -1, TIMED_OUT, true, OUTPUT, mp.consumeOutput());
        }
    }

    private Object handleTerminalRelease(JsonNode params) {
        String terminalId = params.get(TERMINAL_ID).asText();
        ManagedProcess mp = terminals.remove(terminalId);
        if (mp != null && mp.process.isAlive()) {
            mp.process.destroy();
        }
        return Map.of(SUCCESS, true);
    }

    private Object handleTerminalKill(JsonNode params) {
        String terminalId = params.get(TERMINAL_ID).asText();
        ManagedProcess mp = terminals.remove(terminalId);
        if (mp != null) {
            mp.process.destroyForcibly();
        }
        return Map.of(SUCCESS, true);
    }

    private static class ManagedProcess {
        final Process process;
        final String terminalId;
        private final StringBuilder outputBuffer = new StringBuilder();

        ManagedProcess(Process process, String terminalId) {
            this.process = process;
            this.terminalId = terminalId;
        }

        synchronized void appendOutput(String text) {
            outputBuffer.append(text);
        }

        synchronized String consumeOutput() {
            String out = outputBuffer.toString();
            outputBuffer.setLength(0);
            return out;
        }
    }
}
