package com.devoxx.genie.service.acp.protocol;

import com.devoxx.genie.service.acp.model.*;
import com.devoxx.genie.service.acp.protocol.exception.AcpConnectionException;
import com.devoxx.genie.service.acp.protocol.exception.AcpSessionException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * High-level client for the Agent Communication Protocol (ACP).
 *
 * <p>Manages the full lifecycle of an ACP agent connection: starting the transport process,
 * performing the protocol handshake, creating sessions, and sending prompts. Agent responses
 * are delivered asynchronously via text chunks to the provided {@code outputConsumer}.
 *
 * <p>Typical usage:
 * <pre>{@code
 * try (AcpClient client = AcpClient.builder()
 *         .outputConsumer(System.out::print)
 *         .build()) {
 *     client.start(projectDir, "claude", "--acp");
 *     client.initialize();
 *     client.createSession(projectDir.getAbsolutePath());
 *     client.sendPrompt("Explain this project");
 * }
 * }</pre>
 *
 * @see AcpTransport
 */
@Slf4j
public class AcpClient implements AutoCloseable {

    /** ACP protocol version used during the initialize handshake. */
    static final int PROTOCOL_VERSION = 1;

    /** Client name sent during the initialize handshake. */
    static final String CLIENT_NAME = "DevoxxGenie";

    /** Client version sent during the initialize handshake. */
    static final String CLIENT_VERSION = "1.0.0";

    /**
     *  Returns the underlying transport.
     */
    @Getter
    private final AcpTransport transport;
    private Consumer<String> outputConsumer;
    private BiConsumer<String, String> notificationListener;
    private final long requestTimeoutSeconds;

    /**
     *  Returns whether the ACP handshake has completed successfully.
     */
    @Getter
    private boolean initialized;
    private boolean closed;
    private String sessionId;

    /**
     * Creates a builder for configuring and creating an {@link AcpClient}.
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new ACP client with the {@link AcpTransport#DEFAULT_REQUEST_TIMEOUT_SECONDS default timeout}.
     *
     * @param outputConsumer callback that receives text chunks from the agent's responses
     */
    public AcpClient(Consumer<String> outputConsumer) {
        this(outputConsumer, AcpTransport.DEFAULT_REQUEST_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new ACP client with a custom request timeout.
     *
     * @param outputConsumer        callback that receives text chunks from the agent's responses
     * @param requestTimeoutSeconds maximum time in seconds to wait for each JSON-RPC response
     */
    public AcpClient(Consumer<String> outputConsumer, long requestTimeoutSeconds) {
        this(new AcpTransport(), outputConsumer, null, requestTimeoutSeconds);
    }

    AcpClient(AcpTransport transport, Consumer<String> outputConsumer,
              BiConsumer<String, String> notificationListener, long requestTimeoutSeconds) {
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        this.outputConsumer = Objects.requireNonNull(outputConsumer, "outputConsumer cannot be null");
        this.notificationListener = notificationListener;
        if (requestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("requestTimeoutSeconds must be greater than 0");
        }
        AgentRequestHandler requestHandler = new AgentRequestHandler(transport);
        this.requestTimeoutSeconds = requestTimeoutSeconds;

        transport.setNotificationHandler(this::handleNotification);
        transport.setRequestHandler(requestHandler::handle);
    }

    public static final class Builder {
        private AcpTransport transport;
        private Consumer<String> outputConsumer;
        private BiConsumer<String, String> notificationListener;
        private long requestTimeoutSeconds = AcpTransport.DEFAULT_REQUEST_TIMEOUT_SECONDS;

        private Builder() {
            this.transport = new AcpTransport();
            this.outputConsumer = text -> {};
        }

        /**
         * Sets a custom ACP transport.
         * Intended primarily for tests and advanced integrations.
         *
         * @param transport custom transport instance
         * @return this builder
         */
        public Builder transport(AcpTransport transport) {
            this.transport = Objects.requireNonNull(transport, "transport cannot be null");
            return this;
        }

        /**
         * Sets the output consumer that receives streamed text chunks from the agent.
         *
         * @param outputConsumer consumer receiving text chunks
         * @return this builder
         */
        public Builder outputConsumer(Consumer<String> outputConsumer) {
            this.outputConsumer = Objects.requireNonNull(outputConsumer, "outputConsumer cannot be null");
            return this;
        }

        /**
         * Sets the timeout in seconds used for ACP JSON-RPC requests.
         *
         * @param requestTimeoutSeconds timeout in seconds, must be greater than 0
         * @return this builder
         */
        public Builder requestTimeoutSeconds(long requestTimeoutSeconds) {
            if (requestTimeoutSeconds <= 0) {
                throw new IllegalArgumentException("requestTimeoutSeconds must be greater than 0");
            }
            this.requestTimeoutSeconds = requestTimeoutSeconds;
            return this;
        }

        /**
         * Builds a configured {@link AcpClient}.
         *
         * @return a new ACP client instance
         */
        public AcpClient build() {
            return new AcpClient(transport, outputConsumer, notificationListener, requestTimeoutSeconds);
        }
    }

    /**
     * Starts the ACP agent subprocess.
     *
     * @param cwd     working directory for the agent process, or {@code null} to inherit
     * @param command the command and arguments to launch the agent (e.g. {@code "claude", "--acp"})
     * @throws AcpConnectionException if the process cannot be started
     */
    public void start(File cwd, String... command) throws AcpConnectionException {
        log.info("[ACP] Starting transport (cwd={}, timeout={}s, command={})",
                formatCwd(cwd), requestTimeoutSeconds, summarizeCommand(command));
        try {
            transport.start(cwd, command);
            log.info("[ACP] Transport started (pid={})", resolvePid());
        } catch (Exception e) {
            log.error("[ACP] Failed to start transport (cwd={}, command={}): {}",
                    formatCwd(cwd), summarizeCommand(command), e.getMessage(), e);
            throw new AcpConnectionException("Failed to start ACP transport: " + e.getMessage(), e);
        }
    }

    /**
     * Performs the ACP protocol handshake by sending an {@code initialize} request.
     *
     * <p>Must be called after {@link #start(File, String...)} and before
     * {@link #createSession(String)}.
     *
     * @throws AcpConnectionException if the handshake fails or the agent returns an error
     */
    public void initialize() throws AcpConnectionException {
        if (initialized) {
            throw new AcpConnectionException("Already initialized. Call close() before reinitializing.");
        }

        InitializeParams params = new InitializeParams(
                PROTOCOL_VERSION,
                ClientCapabilities.full(),
                new ClientInfo(CLIENT_NAME, CLIENT_VERSION)
        );

        log.debug("[ACP] Sending initialize request (protocolVersion={}, client={}/{}, timeout={}s)",
                PROTOCOL_VERSION, CLIENT_NAME, CLIENT_VERSION, requestTimeoutSeconds);
        try {
            JsonRpcMessage response = transport.sendRequest("initialize", params, requestTimeoutSeconds);
            if (response.getError() != null) {
                log.warn("[ACP] initialize returned error (code={}, message={})",
                        response.getError().getCode(), response.getError().getMessage());
                throw new AcpConnectionException("Initialize failed: " + response.getError().getMessage());
            }

            InitializeResult result = AcpTransport.MAPPER.treeToValue(response.getResult(), InitializeResult.class);
            initialized = true;
            log.info("[ACP] Connected to agent (protocolVersion={}, timeout={}s)",
                    result.getProtocolVersion(), requestTimeoutSeconds);
        } catch (AcpConnectionException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[ACP] initialize interrupted");
            throw new AcpConnectionException("Initialize interrupted", e);
        } catch (IOException e) {
            log.error("[ACP] initialize failed due to I/O error: {}", e.getMessage(), e);
            throw new AcpConnectionException("Initialize failed due to I/O error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[ACP] initialize failed unexpectedly: {}", e.getMessage(), e);
            throw new AcpConnectionException("Initialize failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new agent session scoped to the given working directory,
     * without additional MCP servers.
     */
    public void createSession(String cwd) throws AcpSessionException {
        createSession(cwd, false);
    }

    /**
     * Creates a new agent session scoped to the given working directory.
     *
     * <p>Must be called after {@link #initialize()} and before {@link #sendPrompt(String)}.
     *
     * @param cwd                the working directory path for the session
     * @param includeBacklogMcp  whether to include the Backlog MCP server
     * @throws AcpSessionException if the session cannot be created
     */
    public void createSession(String cwd, boolean includeBacklogMcp) throws AcpSessionException {
        if (!initialized) {
            throw new AcpSessionException("Not initialized. Call initialize() before creating a session.");
        }

        SessionNewParams params = new SessionNewParams(cwd, includeBacklogMcp);

        log.debug("[ACP] Creating session (cwd={}, timeout={}s)", cwd, requestTimeoutSeconds);
        try {
            JsonRpcMessage response = transport.sendRequest("session/new", params, requestTimeoutSeconds);
            if (response.getError() != null) {
                log.warn("[ACP] session/new returned error (code={}, message={})",
                        response.getError().getCode(), response.getError().getMessage());
                throw new AcpSessionException("session/new failed: " + response.getError().getMessage());
            }

            SessionNewResult result = AcpTransport.MAPPER.treeToValue(response.getResult(), SessionNewResult.class);
            this.sessionId = result.getSessionId();
            log.info("[ACP] Session created (sessionId={}, cwd={})", sessionId, cwd);
        } catch (AcpSessionException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[ACP] session/new interrupted");
            throw new AcpSessionException("Session creation interrupted", e);
        } catch (IOException e) {
            log.error("[ACP] session/new failed due to I/O error: {}", e.getMessage(), e);
            throw new AcpSessionException("session/new failed due to I/O error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[ACP] session/new failed unexpectedly: {}", e.getMessage(), e);
            throw new AcpSessionException("session/new failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a user prompt to the agent within the current session.
     *
     * <p>The agent's response text is delivered asynchronously via the
     * {@code outputConsumer} provided at construction time.
     *
     * @param text the prompt text to send
     * @throws AcpSessionException if no session is active or the request fails
     */
    public void sendPrompt(String text) throws AcpSessionException {
        if (sessionId == null) {
            throw new AcpSessionException("No active session. Call createSession() first.");
        }

        SessionPromptParams params = new SessionPromptParams(sessionId, text);

        log.debug("[ACP] Sending prompt (sessionId={}, chars={}, timeout={}s)",
                sessionId, safeLength(text), requestTimeoutSeconds);
        try {
            JsonRpcMessage response = transport.sendRequest("session/prompt", params, requestTimeoutSeconds);
            if (response.getError() != null) {
                log.warn("[ACP] session/prompt returned error (sessionId={}, code={}, message={})",
                        sessionId, response.getError().getCode(), response.getError().getMessage());
                throw new AcpSessionException("session/prompt failed: " + response.getError().getMessage());
            }
            log.info("[ACP] Prompt completed (sessionId={}, result={})", sessionId,
                    response.getResult() != null ? response.getResult().toString() : "null");
        } catch (AcpSessionException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[ACP] session/prompt interrupted (sessionId={})", sessionId);
            throw new AcpSessionException("Prompt sending interrupted", e);
        } catch (IOException e) {
            log.error("[ACP] session/prompt failed due to I/O error (sessionId={}): {}",
                    sessionId, e.getMessage(), e);
            throw new AcpSessionException("session/prompt failed due to I/O error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[ACP] session/prompt failed unexpectedly (sessionId={}): {}",
                    sessionId, e.getMessage(), e);
            throw new AcpSessionException("session/prompt failed: " + e.getMessage(), e);
        }
    }

    private void handleNotification(JsonRpcMessage msg) {
        if (!"session/update".equals(msg.getMethod())) {
            log.debug("[ACP] Notification method: {}", msg.getMethod());
            return;
        }
        try {
            SessionUpdateParams updateParams = AcpTransport.MAPPER.treeToValue(msg.getParams(), SessionUpdateParams.class);
            if (updateParams.update == null) {
                log.debug("[ACP] session/update without payload");
                return;
            }

            String updateType = updateParams.update.has("sessionUpdate")
                    ? updateParams.update.get("sessionUpdate").asText() : "";
            log.debug("[ACP] session/update type: {}", updateType);

            if ("agent_message_chunk".equals(updateType)) {
                handleAgentMessageChunk(updateParams.update);
            } else if ("tool_call".equals(updateType)) {
                handleToolCall(updateParams.update);
            } else if ("tool_call_update".equals(updateType)) {
                handleToolCallUpdate(updateParams.update);
            } else {
                fireNotificationListener(updateType, "");
                log.debug("[ACP] Ignoring session/update type: {}", updateType);
            }
        } catch (Exception e) {
            log.warn("[ACP] Failed to parse session/update: {}", e.getMessage(), e);
        }
    }

    private void handleAgentMessageChunk(JsonNode update) throws IOException {
        JsonNode content = update.get("content");
        if (content == null) {
            log.debug("[ACP] agent_message_chunk without content");
            return;
        }
        ContentBlock block = AcpTransport.MAPPER.treeToValue(content, ContentBlock.class);
        if (!"text".equals(block.type) || block.text == null) {
            log.debug("[ACP] Non-text or null content block: type={}", block.type);
            return;
        }
        Consumer<String> consumer = outputConsumer;
        if (consumer != null) {
            consumer.accept(block.text);
        }
        fireNotificationListener("agent_message_chunk", block.text);
        log.debug("[ACP] Text chunk ({}ch)", block.text.length());
    }

    private void handleToolCall(JsonNode update) {
        String toolName = findTextField(update);
        String args = findNodeText(update, "input", "arguments", "params");
        if (toolName == null) {
            // Log full JSON so we can discover the actual field names
            log.info("[ACP] tool_call fields: {}", truncate(update.toString()));
            toolName = "unknown";
        }
        fireNotificationListener("tool_call", toolName + ": " + args);
        log.debug("[ACP] Tool call: {} (args={}ch)", toolName, args.length());
    }

    private void handleToolCallUpdate(JsonNode update) {
        String toolName = findTextField(update);
        String output = findNodeText(update, "output", "result", "content");
        if (toolName == null) {
            log.info("[ACP] tool_call_update fields: {}", truncate(update.toString()));
            toolName = "unknown";
        }
        fireNotificationListener("tool_call_update", toolName + ": " + output);
        log.debug("[ACP] Tool result: {} (output={}ch)", toolName, output.length());
    }

    /** Searches for the first matching text field name in the node or one level deep. */
    private static String findTextField(JsonNode node) {
        for (String name : new String[]{"toolName", "name", "tool_name"}) {
            if (node.has(name) && node.get(name).isTextual()) {
                return node.get(name).asText();
            }
        }
        // Try one level deep (e.g. "toolCall": { "name": "..." })
        for (var entry : node.properties()) {
            if (entry.getValue().isObject()) {
                for (String name : new String[]{"toolName", "name", "tool_name"}) {
                    JsonNode child = entry.getValue().get(name);
                    if (child != null && child.isTextual()) {
                        return child.asText();
                    }
                }
            }
        }
        return null;
    }

    /** Searches for the first matching node and returns its string representation. */
    private static String findNodeText(JsonNode node, String... candidates) {
        // First, check the node itself
        String result = findTextInNode(node, candidates);
        if (result != null) {
            return result;
        }

        // Then check one level deep
        return findTextInNestedNode(node, candidates);
    }

    /** Helper method to search for text in a node. */
    private static String findTextInNode(JsonNode node, String... candidates) {
        for (String name : candidates) {
            if (node.has(name)) {
                JsonNode child = node.get(name);
                return child.isTextual() ? child.asText() : child.toString();
            }
        }
        return null;
    }

    /** Helper method to search for text in nested nodes. */
    private static String findTextInNestedNode(JsonNode node, String... candidates) {
        for (var entry : node.properties()) {
            if (entry.getValue().isObject()) {
                String result = findTextInNode(entry.getValue(), candidates);
                if (result != null) {
                    return result;
                }
            }
        }
        return "";
    }

    private static String truncate(String text) {
        return text.length() <= 500 ? text : text.substring(0, 500) + "...";
    }

    private void fireNotificationListener(String updateType, String detail) {
        BiConsumer<String, String> listener = notificationListener;
        if (listener != null) {
            try {
                listener.accept(updateType, detail);
            } catch (Exception e) {
                log.debug("[ACP] Notification listener threw: {}", e.getMessage());
            }
        }
    }

    /**
     * Returns whether the underlying ACP transport process is currently running.
     *
     * @return {@code true} if the transport process exists and is alive; {@code false} otherwise
     */
    public boolean isRunning() {
        Process process = transport.getProcess();
        return process != null && process.isAlive();
    }

    /**
     * Performs a lightweight ACP connectivity health check.
     *
     * <p>Sends a JSON-RPC {@code ping} request and returns {@code true} if any valid response
     * (result or error) is received before the configured timeout. Returns {@code false} when
     * the transport is not running or if the request fails/times out.
     *
     * @return {@code true} when the ACP agent responds; {@code false} otherwise
     */
    public boolean ping() {
        if (!isRunning()) {
            log.debug("[ACP] Skipping ping because transport is not running");
            return false;
        }

        log.debug("[ACP] Sending ping (timeout={}s)", requestTimeoutSeconds);
        try {
            JsonRpcMessage response = transport.sendRequest("ping", null, requestTimeoutSeconds);
            log.debug("[ACP] Ping response received (hasError={})", response != null && response.getError() != null);
            return response != null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[ACP] Ping interrupted");
            return false;
        } catch (Exception e) {
            log.warn("[ACP] Ping failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /** Closes the underlying transport and terminates the agent process. */
    @Override
    public synchronized void close() {
        if (closed) {
            log.debug("[ACP] close() called on an already closed client");
            return;
        }
        closed = true;
        log.info("[ACP] Closing client (initialized={}, hasSession={}, running={})",
                initialized, sessionId != null, isRunning());
        try {
            transport.close();
            log.info("[ACP] Transport closed");
        } catch (Exception e) {
            log.warn("[ACP] Error while closing transport: {}", e.getMessage(), e);
        } finally {
            initialized = false;
            sessionId = null;
            outputConsumer = null;
            notificationListener = null;
            log.debug("[ACP] Client state cleared");
        }
    }

    private static String formatCwd(File cwd) {
        return cwd == null ? "<inherit>" : cwd.getAbsolutePath();
    }

    private static String summarizeCommand(String... command) {
        if (command == null || command.length == 0) {
            return "<empty>";
        }
        return String.join(" ", Arrays.stream(command)
                .map(part -> part == null ? "<null>" : part)
                .toList());
    }

    private String resolvePid() {
        Process process = transport.getProcess();
        if (process == null) {
            return "<unknown>";
        }
        return String.valueOf(process.pid());
    }

    private static int safeLength(String text) {
        return text == null ? 0 : text.length();
    }
}
