package com.devoxx.genie.service.acp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AgentRequestHandler {

    private final AcpTransport transport;
    private final ConcurrentHashMap<String, ManagedProcess> terminals = new ConcurrentHashMap<>();

    public AgentRequestHandler(AcpTransport transport) {
        this.transport = transport;
    }

    public void handle(JsonRpcMessage msg) {
        try {
            Object result = dispatch(msg.method, msg.params);
            transport.sendResponse(msg.id, result);
        } catch (Exception e) {
            try {
                transport.sendErrorResponse(msg.id, -32603, e.getMessage());
            } catch (IOException ex) {
                log.error("[ACP] Failed to send error response: {}", ex.getMessage(), ex);
            }
        }
    }

    private Object dispatch(String method, JsonNode params) throws Exception {
        return switch (method) {
            case "fs/read_text_file" -> handleFsRead(params);
            case "fs/write_text_file" -> handleFsWrite(params);
            case "session/request_permission" -> handleRequestPermission(params);
            case "terminal/create" -> handleTerminalCreate(params);
            case "terminal/output" -> handleTerminalOutput(params);
            case "terminal/wait_for_exit" -> handleTerminalWaitForExit(params);
            case "terminal/release" -> handleTerminalRelease(params);
            case "terminal/kill" -> handleTerminalKill(params);
            default -> throw new UnsupportedOperationException("Unknown method: " + method);
        };
    }

    private Object handleFsRead(JsonNode params) throws IOException {
        String path = params.get("path").asText();
        String content = Files.readString(Path.of(path));
        return Map.of("content", content);
    }

    private Object handleFsWrite(JsonNode params) throws IOException {
        String path = params.get("path").asText();
        String content = params.get("content").asText();
        Path filePath = Path.of(path);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
        return Map.of("success", true);
    }

    private Object handleRequestPermission(JsonNode params) {
        // Auto-approve all permissions
        return Map.of("granted", true);
    }

    private Object handleTerminalCreate(JsonNode params) throws IOException {
        String terminalId = params.has("terminalId") ? params.get("terminalId").asText()
                : "term-" + System.currentTimeMillis();
        String command = params.get("command").asText();
        String cwd = params.has("cwd") ? params.get("cwd").asText() : System.getProperty("user.dir");

        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.directory(new File(cwd));
        pb.redirectErrorStream(true);

        Process proc = pb.start();
        ManagedProcess mp = new ManagedProcess(proc, terminalId);
        terminals.put(terminalId, mp);

        Thread captureThread = new Thread(() -> captureOutput(mp), "term-" + terminalId);
        captureThread.setDaemon(true);
        captureThread.start();

        return Map.of("terminalId", terminalId);
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
        String terminalId = params.get("terminalId").asText();
        ManagedProcess mp = terminals.get(terminalId);
        if (mp == null) {
            return Map.of("output", "", "error", "Unknown terminal: " + terminalId);
        }
        String output = mp.consumeOutput();
        return Map.of("output", output);
    }

    private Object handleTerminalWaitForExit(JsonNode params) throws InterruptedException {
        String terminalId = params.get("terminalId").asText();
        int timeoutMs = params.has("timeout") ? params.get("timeout").asInt() : 30000;
        ManagedProcess mp = terminals.get(terminalId);
        if (mp == null) {
            return Map.of("exitCode", -1, "error", "Unknown terminal: " + terminalId);
        }
        boolean finished = mp.process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (finished) {
            int exitCode = mp.process.exitValue();
            return Map.of("exitCode", exitCode, "output", mp.consumeOutput());
        } else {
            return Map.of("exitCode", -1, "timedOut", true, "output", mp.consumeOutput());
        }
    }

    private Object handleTerminalRelease(JsonNode params) {
        String terminalId = params.get("terminalId").asText();
        ManagedProcess mp = terminals.remove(terminalId);
        if (mp != null && mp.process.isAlive()) {
            mp.process.destroy();
        }
        return Map.of("success", true);
    }

    private Object handleTerminalKill(JsonNode params) {
        String terminalId = params.get("terminalId").asText();
        ManagedProcess mp = terminals.remove(terminalId);
        if (mp != null) {
            mp.process.destroyForcibly();
        }
        return Map.of("success", true);
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
