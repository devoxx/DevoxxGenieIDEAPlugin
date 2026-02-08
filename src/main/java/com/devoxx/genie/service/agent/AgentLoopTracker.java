package com.devoxx.genie.service.agent;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a ToolProvider and enforces a maximum number of tool calls.
 * Uses a shared counter across all tools to prevent infinite loops.
 * Returns an error string (not an exception) when the limit is reached,
 * so the LLM can gracefully wrap up the conversation.
 * Also publishes agent debug log events to the message bus.
 */
@Slf4j
public class AgentLoopTracker implements ToolProvider {

    private final ToolProvider delegate;
    private final int maxToolCalls;
    private final AtomicInteger callCount = new AtomicInteger(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final @Nullable Project project;
    private final @Nullable String subAgentId;
    private final List<Cancellable> children = new CopyOnWriteArrayList<>();

    public AgentLoopTracker(@NotNull ToolProvider delegate, int maxToolCalls) {
        this(delegate, maxToolCalls, null, null);
    }

    public AgentLoopTracker(@NotNull ToolProvider delegate, int maxToolCalls, @Nullable Project project) {
        this(delegate, maxToolCalls, project, null);
    }

    public AgentLoopTracker(@NotNull ToolProvider delegate, int maxToolCalls,
                            @Nullable Project project, @Nullable String subAgentId) {
        this.delegate = delegate;
        this.maxToolCalls = maxToolCalls;
        this.project = project;
        this.subAgentId = subAgentId;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult result = delegate.provideTools(request);
        ToolProviderResult.Builder builder = ToolProviderResult.builder();

        for (var entry : result.tools().entrySet()) {
            ToolSpecification spec = entry.getKey();
            ToolExecutor original = entry.getValue();

            ToolExecutor tracked = (toolRequest, memoryId) -> {
                if (cancelled.get()) {
                    String cancelMsg = "Execution cancelled by user. Stop calling tools and provide your best answer now.";
                    log.debug("Tool call '{}' skipped — agent cancelled", toolRequest.name());
                    return cancelMsg;
                }

                int count = callCount.incrementAndGet();
                if (count > maxToolCalls) {
                    String errorMsg = "Error: Agent loop limit reached (" + maxToolCalls
                            + " tool calls). Provide your best answer with the information gathered so far.";
                    publishLogEvent(AgentType.LOOP_LIMIT, toolRequest.name(), toolRequest.arguments(), errorMsg, count);
                    return errorMsg;
                }

                publishLogEvent(AgentType.TOOL_REQUEST, toolRequest.name(), toolRequest.arguments(), null, count);

                String toolResult;
                try {
                    toolResult = original.execute(toolRequest, memoryId);
                } catch (Exception e) {
                    String errorResult = "Error: " + e.getMessage();
                    publishLogEvent(AgentType.TOOL_ERROR, toolRequest.name(), toolRequest.arguments(), errorResult, count);
                    return errorResult;
                }

                publishLogEvent(AgentType.TOOL_RESPONSE, toolRequest.name(), null, toolResult, count);
                return toolResult;
            };

            builder.add(spec, tracked);
        }

        return builder.build();
    }

    private void publishLogEvent(AgentType type, String toolName, String arguments, String result, int callNumber) {
        if (project == null || project.isDisposed()) {
            return;
        }
        if (!Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())) {
            return;
        }
        try {
            AgentMessage message = AgentMessage.builder()
                    .type(type)
                    .toolName(toolName)
                    .arguments(arguments)
                    .result(result)
                    .callNumber(callNumber)
                    .maxCalls(maxToolCalls)
                    .subAgentId(subAgentId)
                    .build();

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.AGENT_LOG_MSG)
                    .onAgentLoggingMessage(message);
        } catch (Exception e) {
            log.debug("Failed to publish agent log event", e);
        }
    }

    /**
     * Registers a child cancellable that will be cancelled when this tracker is cancelled.
     * Used to propagate cancellation to sub-agents running inside parallel_explore.
     */
    public void registerChild(@NotNull Cancellable child) {
        children.add(child);
        // If already cancelled, cancel the child immediately
        if (cancelled.get()) {
            child.cancel();
        }
    }

    /**
     * Cancels the agent loop. Any subsequent tool calls will be short-circuited
     * with an error message telling the LLM to stop.
     * Also cancels all registered child cancellables (e.g. sub-agent runners).
     */
    public void cancel() {
        cancelled.set(true);
        for (Cancellable child : children) {
            child.cancel();
        }
        children.clear();
        log.info("Agent loop tracker cancelled");
    }

    /**
     * Interface for cancellable children of this tracker.
     */
    public interface Cancellable {
        void cancel();
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void reset() {
        callCount.set(0);
        cancelled.set(false);
    }
}
