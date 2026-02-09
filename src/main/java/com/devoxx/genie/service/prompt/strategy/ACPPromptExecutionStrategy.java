package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.acp.ACPSessionUpdate;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.acp.ACPSessionManager;
import com.devoxx.genie.service.acp.ACPTransport;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Execution strategy for ACP (Agent Client Protocol) agents.
 * Bypasses the LangChain4J ChatModel/StreamingChatModel flow entirely.
 * Instead, communicates with an ACP agent subprocess via JSON-RPC over stdio.
 */
@Slf4j
public class ACPPromptExecutionStrategy extends AbstractPromptExecutionStrategy {

    private final AtomicReference<String> activeSessionId = new AtomicReference<>();
    private final AtomicReference<Consumer<ACPSessionUpdate>> activeListener = new AtomicReference<>();

    public ACPPromptExecutionStrategy(@NotNull Project project) {
        super(project);
    }

    @Override
    protected String getStrategyName() {
        return "ACP agent prompt";
    }

    @Override
    protected void executeStrategySpecific(@NotNull ChatMessageContext context,
                                           @NotNull PromptOutputPanel panel,
                                           @NotNull PromptTask<PromptResult> resultTask) {
        ACPSessionManager sessionManager = ACPSessionManager.getInstance();

        threadPoolManager.getPromptExecutionPool().execute(() -> {
            try {
                // Ensure agent is initialized
                sessionManager.ensureInitialized();

                // Create or reuse session
                String sessionId = sessionManager.getCurrentSessionId();
                if (sessionId == null) {
                    String cwd = project.getBasePath() != null ? project.getBasePath() : System.getProperty("user.home");
                    sessionId = sessionManager.newSession(cwd);
                }
                activeSessionId.set(sessionId);

                // Accumulator for streamed response
                StringBuilder accumulatedResponse = new StringBuilder();
                StringBuilder accumulatedThoughts = new StringBuilder();

                // Register update listener for this prompt turn
                Consumer<ACPSessionUpdate> updateListener = update -> {
                    switch (update.getType()) {
                        case AGENT_MESSAGE_CHUNK -> {
                            if (update.getTextContent() != null) {
                                accumulatedResponse.append(update.getTextContent());
                                updateChatPanel(context, panel, accumulatedResponse.toString());
                            }
                        }
                        case AGENT_THOUGHT_CHUNK -> {
                            if (update.getTextContent() != null) {
                                accumulatedThoughts.append(update.getTextContent());
                                // Optionally render thoughts — for now just log
                                log.debug("Agent thinking: {}", update.getTextContent());
                            }
                        }
                        case TOOL_CALL -> log.info("ACP tool call: {} ({})",
                                update.getToolCallTitle(), update.getToolCallStatus());
                        case TOOL_CALL_UPDATE -> log.info("ACP tool update: {} -> {}",
                                update.getToolCallTitle(), update.getToolCallStatus());
                        case PLAN -> log.info("ACP plan update: {}", update.getRawUpdate());
                        default -> log.debug("ACP update ({}): {}", update.getType(), update.getRawUpdate());
                    }
                };

                activeListener.set(updateListener);
                sessionManager.addUpdateListener(updateListener);

                // Send the prompt
                CompletableFuture<String> promptFuture = sessionManager.sendPrompt(sessionId, context.getUserPrompt());

                // Wait for prompt to complete
                String stopReason = promptFuture.get();
                log.info("ACP prompt completed with stop reason: {}", stopReason);

                // Set final AI message
                String finalResponse = accumulatedResponse.toString();
                if (finalResponse.isEmpty()) {
                    finalResponse = "(No response from ACP agent)";
                }
                context.setAiMessage(AiMessage.from(finalResponse));

                // Final UI update
                updateChatPanel(context, panel, finalResponse);

                // Complete the task
                resultTask.complete(PromptResult.success(context));

            } catch (ACPTransport.ACPException e) {
                log.error("ACP execution error", e);
                handleExecutionError(e, context, resultTask, panel);
            } catch (Exception e) {
                log.error("Error in ACP prompt execution", e);
                handleExecutionError(e, context, resultTask, panel);
            } finally {
                // Remove update listener
                Consumer<ACPSessionUpdate> listener = activeListener.getAndSet(null);
                if (listener != null) {
                    sessionManager.removeUpdateListener(listener);
                }
            }
        });
    }

    @Override
    public void cancel() {
        String sessionId = activeSessionId.get();
        if (sessionId != null) {
            ACPSessionManager.getInstance().cancelSession(sessionId);
        }
    }

    /**
     * Update the chat panel with accumulated ACP response text on the EDT.
     */
    private void updateChatPanel(@NotNull ChatMessageContext context,
                                 @NotNull PromptOutputPanel panel,
                                 @NotNull String content) {
        ApplicationManager.getApplication().invokeLater(() -> {
            context.setAiMessage(AiMessage.from(content));
            if (panel.getConversationPanel() != null
                    && panel.getConversationPanel().webViewController != null) {
                panel.getConversationPanel().webViewController.updateAiMessageContent(context);
            }
        });
    }
}
