package com.devoxx.genie.service.agent;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutionResult;
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

        for (AiServiceTool tool : result.aiServiceTools()) {
            ToolSpecification spec = tool.toolSpecification();
            ToolExecutor original = tool.toolExecutor();

            // Override executeWithContext (not just the legacy execute) so the new
            // langchain4j tool contract is honoured. Some executors — notably the
            // langchain4j-skills tools backing activate_skill — implement
            // executeWithContext and deliberately throw from the legacy execute
            // ("executeWithContext must be called instead"). Wrapping only execute()
            // would break those tools, so we thread executeWithContext through the
            // whole wrapper chain and keep execute() as a legacy fallback.
            ToolExecutor tracked = new ToolExecutor() {
                @Override
                public String execute(ToolExecutionRequest toolRequest, Object memoryId) {
                    return track(toolRequest,
                            () -> original.execute(toolRequest, memoryId));
                }

                @Override
                public ToolExecutionResult executeWithContext(ToolExecutionRequest toolRequest, InvocationContext context) {
                    // Short-circuit results (cancel / loop-limit) are returned as plain text.
                    String[] shortCircuit = new String[1];
                    ToolExecutionResult[] delegated = new ToolExecutionResult[1];
                    String text = track(toolRequest, () -> {
                        ToolExecutionResult result = original.executeWithContext(toolRequest, context);
                        delegated[0] = result;
                        return resultText(result);
                    });
                    // If the wrapper produced its own message (cancel/limit/error) instead of
                    // delegating, wrap that text; otherwise pass the delegate result through
                    // unchanged so non-text content is preserved.
                    if (delegated[0] != null && text != null && text.equals(resultText(delegated[0]))) {
                        return delegated[0];
                    }
                    return ToolExecutionResult.builder().resultText(text).build();
                }
            };

            builder.add(tool.toBuilder().toolExecutor(tracked).build());
        }

        return builder.build();
    }

    /**
     * Shared tracking logic for both the legacy {@link ToolExecutor#execute} and the
     * new {@link ToolExecutor#executeWithContext} paths. Enforces cancellation and the
     * loop limit, publishes debug events and converts thrown exceptions into an error
     * string so the LLM can wrap up gracefully.
     *
     * @param toolRequest the incoming tool request (for name/arguments)
     * @param invoker     performs the actual delegated execution and returns its text result
     * @return the tool result text, or a short-circuit / error message
     */
    private String track(@NotNull ToolExecutionRequest toolRequest, @NotNull ToolInvoker invoker) {
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
            toolResult = invoker.invoke();
        } catch (Exception e) {
            String errorResult = "Error: " + e.getMessage();
            publishLogEvent(AgentType.TOOL_ERROR, toolRequest.name(), toolRequest.arguments(), errorResult, count);
            return errorResult;
        }

        // Tool executors signal failure by returning an "Error: ..." string rather than
        // throwing (e.g. EditFileToolExecutor when old_string is not found). Classify
        // those as TOOL_ERROR so the activity view shows a failure icon instead of a
        // green "valid" check (issue #1144).
        AgentType type = isErrorResult(toolResult) ? AgentType.TOOL_ERROR : AgentType.TOOL_RESPONSE;
        publishLogEvent(type, toolRequest.name(), null, toolResult, count);
        return toolResult;
    }

    /**
     * Returns {@code true} when a tool result string represents an error by convention —
     * i.e. it starts with the "Error:" prefix used across the built-in tool executors.
     */
    static boolean isErrorResult(@Nullable String result) {
        return result != null && result.stripLeading().startsWith("Error:");
    }

    /**
     * Safely extracts the text from a {@link ToolExecutionResult}. {@code resultText()}
     * throws when the result is not a single text content (e.g. image content), so we
     * fall back to {@code toString()} of the raw result in that case.
     */
    @Nullable
    private static String resultText(@Nullable ToolExecutionResult result) {
        if (result == null) {
            return null;
        }
        try {
            return result.resultText();
        } catch (RuntimeException e) {
            Object raw = result.result();
            return raw != null ? raw.toString() : null;
        }
    }

    /** Functional interface for the delegated tool execution inside {@link #track}. */
    @FunctionalInterface
    private interface ToolInvoker {
        String invoke() throws Exception;
    }

    private void publishLogEvent(AgentType type, String toolName, String arguments, String result, int callNumber) {
        if (project == null || project.isDisposed()) {
            return;
        }
        // LOOP_LIMIT is not a debug event: the chat view subscribes to it to render a
        // durable "max tool calls reached" notice (task-234), so it must be published
        // even when agent debug logging is switched off. All other event types remain
        // gated behind the debug-logs setting.
        if (type != AgentType.LOOP_LIMIT
                && !Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())) {
            return;
        }
        try {
            ActivityMessage message = ActivityMessage.builder()
                    .source(ActivitySource.AGENT)
                    .agentType(type)
                    .toolName(toolName)
                    .arguments(arguments)
                    .result(result)
                    .callNumber(callNumber)
                    .maxCalls(maxToolCalls)
                    .subAgentId(subAgentId)
                    .projectLocationHash(project.getLocationHash())
                    .build();

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                    .onActivityMessage(message);
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
