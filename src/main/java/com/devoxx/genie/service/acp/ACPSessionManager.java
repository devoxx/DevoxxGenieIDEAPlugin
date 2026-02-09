package com.devoxx.genie.service.acp;

import com.devoxx.genie.model.acp.ACPMessage;
import com.devoxx.genie.model.acp.ACPSessionUpdate;
import com.devoxx.genie.model.acp.ACPSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages ACP agent session lifecycle: initialization, session creation, prompting, and cancellation.
 * This is an application-level service that maintains the agent subprocess.
 * Registered in plugin.xml as an applicationService.
 */
@Slf4j
public final class ACPSessionManager implements Disposable {

    private static final int INITIALIZE_TIMEOUT_SECONDS = 30;
    private static final int PROTOCOL_VERSION = 1;

    private ACPTransport transport;
    private String currentSessionId;
    private boolean initialized = false;
    private JsonObject agentCapabilities;
    private JsonObject agentInfo;

    private final List<Consumer<ACPSessionUpdate>> updateListeners = new CopyOnWriteArrayList<>();

    public static ACPSessionManager getInstance() {
        return ApplicationManager.getApplication().getService(ACPSessionManager.class);
    }

    /**
     * Ensure the agent is started and initialized. Safe to call multiple times.
     */
    public synchronized void ensureInitialized() throws ACPTransport.ACPException {
        if (transport != null && transport.isRunning() && initialized) {
            return;
        }
        startAndInitialize();
    }

    /**
     * Start the agent subprocess and perform the ACP initialize handshake.
     */
    private void startAndInitialize() throws ACPTransport.ACPException {
        // Close any existing transport
        closeTransport();

        ACPSettings settings = DevoxxGenieStateService.getInstance().getAcpSettings();
        if (settings == null || settings.getAgentCommand().isEmpty()) {
            throw new ACPTransport.ACPException("ACP agent command is not configured");
        }

        transport = new ACPTransport();

        // Register notification handler
        transport.onNotification(this::handleNotification);

        // Register request handler (for agent→client requests like request_permission)
        transport.onRequest(this::handleAgentRequest);

        try {
            transport.start(
                    settings.getAgentCommand(),
                    settings.getAgentArgs(),
                    settings.getEnv(),
                    null
            );
        } catch (IOException e) {
            throw new ACPTransport.ACPException("Failed to start ACP agent: " + e.getMessage(), e);
        }

        // Send initialize request
        JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", PROTOCOL_VERSION);

        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "DevoxxGenie");
        clientInfo.addProperty("version", "1.0");
        params.add("clientInfo", clientInfo);

        // No client capabilities for now (no fs, no terminal)
        params.add("clientCapabilities", new JsonObject());

        try {
            CompletableFuture<ACPMessage> future = transport.sendRequest("initialize", params);
            ACPMessage response = future.get(INITIALIZE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getResult() != null && response.getResult().isJsonObject()) {
                JsonObject result = response.getResult().getAsJsonObject();
                if (result.has("agentCapabilities")) {
                    agentCapabilities = result.getAsJsonObject("agentCapabilities");
                }
                if (result.has("agentInfo")) {
                    agentInfo = result.getAsJsonObject("agentInfo");
                }
                log.info("ACP agent initialized: {}", agentInfo);
            }

            initialized = true;
        } catch (Exception e) {
            closeTransport();
            throw new ACPTransport.ACPException("ACP initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new session with the agent.
     *
     * @param cwd The working directory for the session
     * @return The session ID
     */
    public String newSession(@NotNull String cwd) throws ACPTransport.ACPException {
        ensureInitialized();

        JsonObject params = new JsonObject();
        params.addProperty("cwd", cwd);

        try {
            CompletableFuture<ACPMessage> future = transport.sendRequest("session/new", params);
            ACPMessage response = future.get(INITIALIZE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getResult() != null && response.getResult().isJsonObject()) {
                JsonObject result = response.getResult().getAsJsonObject();
                if (result.has("sessionId")) {
                    currentSessionId = result.get("sessionId").getAsString();
                    log.info("ACP session created: {}", currentSessionId);
                    return currentSessionId;
                }
            }
            throw new ACPTransport.ACPException("No sessionId in session/new response");
        } catch (ACPTransport.ACPException e) {
            throw e;
        } catch (Exception e) {
            throw new ACPTransport.ACPException("Failed to create ACP session: " + e.getMessage(), e);
        }
    }

    /**
     * Send a prompt to the agent and get a future for the completion response.
     * Updates are delivered via registered update listeners.
     *
     * @param sessionId The session ID
     * @param prompt    The user prompt text
     * @return Future that completes when the prompt turn is done, with the stop reason
     */
    public CompletableFuture<String> sendPrompt(@NotNull String sessionId, @NotNull String prompt) {
        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);

        // Build prompt as ContentBlock array
        JsonArray promptBlocks = new JsonArray();
        JsonObject textBlock = new JsonObject();
        textBlock.addProperty("type", "text");
        textBlock.addProperty("text", prompt);
        promptBlocks.add(textBlock);
        params.add("prompt", promptBlocks);

        CompletableFuture<ACPMessage> responseFuture = transport.sendRequest("session/prompt", params);

        return responseFuture.thenApply(response -> {
            if (response.getResult() != null && response.getResult().isJsonObject()) {
                JsonObject result = response.getResult().getAsJsonObject();
                if (result.has("stopReason")) {
                    return result.get("stopReason").getAsString();
                }
            }
            return "end_turn";
        });
    }

    /**
     * Cancel the current prompt turn.
     */
    public void cancelSession(@NotNull String sessionId) {
        if (transport == null || !transport.isRunning()) {
            return;
        }
        try {
            JsonObject params = new JsonObject();
            params.addProperty("sessionId", sessionId);
            transport.sendNotification("session/cancel", params);
            log.info("ACP session cancelled: {}", sessionId);
        } catch (IOException e) {
            log.error("Failed to cancel ACP session", e);
        }
    }

    /**
     * Register a listener for session update notifications.
     */
    public void addUpdateListener(@NotNull Consumer<ACPSessionUpdate> listener) {
        updateListeners.add(listener);
    }

    /**
     * Remove an update listener.
     */
    public void removeUpdateListener(@NotNull Consumer<ACPSessionUpdate> listener) {
        updateListeners.remove(listener);
    }

    @Nullable
    public String getCurrentSessionId() {
        return currentSessionId;
    }

    @Nullable
    public JsonObject getAgentInfo() {
        return agentInfo;
    }

    public boolean isInitialized() {
        return initialized && transport != null && transport.isRunning();
    }

    /**
     * Shut down the transport and reset state.
     */
    public synchronized void shutdown() {
        closeTransport();
        currentSessionId = null;
        initialized = false;
        agentCapabilities = null;
        agentInfo = null;
    }

    @Override
    public void dispose() {
        shutdown();
    }

    private void handleNotification(@NotNull ACPMessage message) {
        if ("session/update".equals(message.getMethod()) && message.getParams() != null) {
            ACPSessionUpdate update = ACPSessionUpdate.fromParams(message.getParams());
            for (Consumer<ACPSessionUpdate> listener : updateListeners) {
                try {
                    listener.accept(update);
                } catch (Exception e) {
                    log.error("Error in ACP update listener", e);
                }
            }
        }
    }

    private void handleAgentRequest(@NotNull ACPMessage message) {
        if ("session/request_permission".equals(message.getMethod()) && message.getId() != null) {
            // For now, auto-approve all permission requests
            // TODO: Show approval dialog like MCPApprovalService
            log.info("ACP agent requested permission: {}", message.getParams());
            JsonObject result = new JsonObject();
            result.addProperty("outcome", "approved");
            try {
                transport.sendResponse(message.getId(), result);
            } catch (IOException e) {
                log.error("Failed to respond to permission request", e);
            }
        } else if (message.getId() != null) {
            // Unknown agent request — respond with empty result
            log.warn("Unknown ACP agent request: {}", message.getMethod());
            try {
                transport.sendResponse(message.getId(), new JsonObject());
            } catch (IOException e) {
                log.error("Failed to respond to unknown ACP request", e);
            }
        }
    }

    private void closeTransport() {
        if (transport != null) {
            try {
                transport.close();
            } catch (Exception e) {
                log.debug("Error closing ACP transport", e);
            }
            transport = null;
        }
    }
}
