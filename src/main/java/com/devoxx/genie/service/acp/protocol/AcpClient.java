package com.devoxx.genie.service.acp.protocol;

import com.devoxx.genie.service.acp.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.function.Consumer;

@Slf4j
public class AcpClient implements AutoCloseable {

    private final AcpTransport transport;
    private final Consumer<String> outputConsumer;
    private String sessionId;

    public AcpClient(Consumer<String> outputConsumer) {
        this.transport = new AcpTransport();
        AgentRequestHandler requestHandler = new AgentRequestHandler(transport);
        this.outputConsumer = outputConsumer;

        transport.setNotificationHandler(this::handleNotification);
        transport.setRequestHandler(requestHandler::handle);
    }

    public void start(File cwd, String... command) throws Exception {
        transport.start(cwd, command);
    }

    public void initialize() throws Exception {
        InitializeParams params = new InitializeParams(
                1,
                ClientCapabilities.full(),
                new ClientInfo("DevoxxGenie", "1.0.0")
        );

        JsonRpcMessage response = transport.sendRequest("initialize", params);
        if (response.error != null) {
            throw new RuntimeException("Initialize failed: " + response.error.message);
        }

        InitializeResult result = AcpTransport.MAPPER.treeToValue(response.result, InitializeResult.class);
        log.info("[ACP] Connected to agent, protocol version: {}", result.protocolVersion);
    }

    public void createSession(String cwd) throws Exception {
        SessionNewParams params = new SessionNewParams(cwd);

        JsonRpcMessage response = transport.sendRequest("session/new", params);
        if (response.error != null) {
            throw new RuntimeException("session/new failed: " + response.error.message);
        }

        SessionNewResult result = AcpTransport.MAPPER.treeToValue(response.result, SessionNewResult.class);
        this.sessionId = result.sessionId;
        log.info("[ACP] Session created: {}", sessionId);
    }

    public void sendPrompt(String text) throws Exception {
        if (sessionId == null) {
            throw new IllegalStateException("No active session. Call createSession() first.");
        }

        SessionPromptParams params = new SessionPromptParams(sessionId, text);

        JsonRpcMessage response = transport.sendRequest("session/prompt", params);
        if (response.error != null) {
            throw new RuntimeException("session/prompt failed: " + response.error.message);
        }
    }

    private void handleNotification(JsonRpcMessage msg) {
        if ("session/update".equals(msg.method)) {
            try {
                SessionUpdateParams updateParams = AcpTransport.MAPPER.treeToValue(msg.params, SessionUpdateParams.class);
                if (updateParams.update == null) return;

                String updateType = updateParams.update.has("sessionUpdate")
                        ? updateParams.update.get("sessionUpdate").asText() : "";

                // Print text from agent message chunks only (skip thought chunks)
                if ("agent_message_chunk".equals(updateType)) {
                    JsonNode content = updateParams.update.get("content");
                    if (content != null) {
                        ContentBlock block = AcpTransport.MAPPER.treeToValue(content, ContentBlock.class);
                        if ("text".equals(block.type) && block.text != null) {
                            outputConsumer.accept(block.text);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[ACP] Failed to parse session/update: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() {
        transport.close();
    }
}
