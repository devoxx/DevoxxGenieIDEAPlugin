package com.devoxx.genie.service.trace;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.AiServiceCancelledEvent;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Records an agent turn from LangChain4j AiServices events (task-279). One recorder is
 * registered on each assistant DevoxxGenie builds, so it knows which chat tab (memory key) and
 * which sub-agent the events belong to — information a {@code ChatModelListener} does not have.
 * <p>
 * Events are buffered in {@link AgentTraceBuffer}; nothing is written to disk here. A listener
 * failure is logged and swallowed so recording can never break a chat.
 */
@Slf4j
public final class AgentTraceRecorder {

    private final AgentTraceBuffer buffer;
    private final String memoryKey;
    private final @Nullable String subAgentId;
    private final TraceSettings settings;
    private final Clock clock;

    AgentTraceRecorder(@NotNull AgentTraceBuffer buffer, @NotNull String memoryKey, @Nullable String subAgentId,
                       @NotNull TraceSettings settings, @NotNull Clock clock) {
        this.buffer = buffer;
        this.memoryKey = memoryKey;
        this.subAgentId = subAgentId;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * Registers a recorder for a main-agent turn when trace recording is enabled; does nothing
     * when it is off (the default), so no listener is ever attached for users who didn't opt in.
     */
    public static <T> @NotNull AiServices<T> registerForTurn(@NotNull AiServices<T> builder, @NotNull String memoryKey) {
        TraceSettings settings = TraceSettings.current();
        if (!settings.enabled()) {
            return builder;
        }
        return builder.registerListeners(new AgentTraceRecorder(AgentTraceBuffer.getInstance(), memoryKey, null,
                settings, Clock.systemUTC()).listeners());
    }

    /**
     * Registers a recorder for a sub-agent whose events go into its parent's turn. Does nothing
     * when recording is off or the parent turn isn't known (parent not being recorded).
     */
    public static <T> @NotNull AiServices<T> registerForSubAgent(@NotNull AiServices<T> builder,
                                                                 @Nullable String parentMemoryKey,
                                                                 @NotNull String subAgentId) {
        TraceSettings settings = TraceSettings.current();
        if (!settings.enabled() || parentMemoryKey == null) {
            return builder;
        }
        return builder.registerListeners(new AgentTraceRecorder(AgentTraceBuffer.getInstance(), parentMemoryKey,
                subAgentId, settings, Clock.systemUTC()).listeners());
    }

    boolean isSubAgent() {
        return subAgentId != null;
    }

    @NotNull List<AiServiceListener<?>> listeners() {
        return List.of(
                listener(AiServiceStartedEvent.class, this::onStarted),
                listener(AiServiceResponseReceivedEvent.class, this::onResponse),
                listener(ToolExecutedEvent.class, this::onToolExecuted),
                listener(AiServiceCompletedEvent.class, e -> onEnded(e)),
                listener(AiServiceErrorEvent.class, e -> onEnded(e)),
                listener(AiServiceCancelledEvent.class, e -> onEnded(e)));
    }

    void onStarted(@NotNull AiServiceStartedEvent event) {
        if (!isSubAgent()) {
            UUID invocationId = event.invocationContext() == null ? UUID.randomUUID() : event.invocationContext().invocationId();
            buffer.startTurn(memoryKey, invocationId, settings);
        }
        buffer.append(memoryKey, TraceEvent.user(text(event.userMessage()), subAgentId, clock.millis()));
    }

    void onResponse(@NotNull AiServiceResponseReceivedEvent event) {
        ChatResponse response = event.response();
        AiMessage ai = response == null ? null : response.aiMessage();
        if (ai == null) {
            return;
        }
        List<TraceEvent.ToolCall> calls = ai.hasToolExecutionRequests()
                ? ai.toolExecutionRequests().stream()
                    .map(r -> new TraceEvent.ToolCall(r.id(), r.name(), r.arguments()))
                    .toList()
                : List.of();
        buffer.append(memoryKey, TraceEvent.assistant(ai.text(), blankToNull(ai.thinking()), calls,
                response.modelName(), subAgentId, clock.millis()));
    }

    void onToolExecuted(@NotNull ToolExecutedEvent event) {
        ToolExecutionRequest request = event.request();
        if (request == null) {
            return;
        }
        buffer.append(memoryKey, TraceEvent.tool(request.id(), request.name(), event.resultText(),
                event.isError(), subAgentId, clock.millis()));
    }

    void onEnded(@NotNull AiServiceEvent event) {
        if (!isSubAgent() && event.invocationContext() != null) {
            buffer.endInvocation(event.invocationContext().invocationId());
        }
    }

    private static @Nullable String text(@Nullable UserMessage message) {
        if (message == null) {
            return null;
        }
        if (message.hasSingleText()) {
            return message.singleText();
        }
        // Multi-part messages (e.g. text plus an image): keep the text parts, note the others.
        return message.contents().stream()
                .map(AgentTraceRecorder::contentText)
                .collect(Collectors.joining("\n"));
    }

    private static @NotNull String contentText(@NotNull Content content) {
        return content instanceof TextContent t ? t.text() : "[" + content.type() + " content]";
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static <E extends AiServiceEvent> @NotNull AiServiceListener<E> listener(@NotNull Class<E> type,
                                                                                    @NotNull Consumer<E> handler) {
        return new AiServiceListener<>() {
            @Override
            public Class<E> getEventClass() {
                return type;
            }

            @Override
            public void onEvent(E event) {
                try {
                    handler.accept(event);
                } catch (RuntimeException e) {
                    log.warn("Agent trace recorder failed on {}: {}", type.getSimpleName(), e.toString());
                }
            }
        };
    }
}
