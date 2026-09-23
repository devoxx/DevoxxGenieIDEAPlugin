package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

/**
 * AiServices {@code chatRequestTransformer} applied to every agent round trip. It
 * <ol>
 *   <li>compacts large, older tool results ({@link ToolResultCompactor}),</li>
 *   <li>withholds the specifications of deferred tools that are not unlocked yet
 *       ({@link DeferredToolRegistry}), and</li>
 *   <li>records the size of what is actually sent ({@link AgentRunMetrics}).</li>
 * </ol>
 * Chat memory is never modified.
 */
@Slf4j
public class AgentRequestTransformer implements BiFunction<ChatRequest, Object, ChatRequest> {

    private final AgentRunContext context;

    public AgentRequestTransformer(@NotNull AgentRunContext context) {
        this.context = context;
    }

    /**
     * Composes transformers left to right, skipping {@code null}s. AiServices accepts a single
     * transformer, so the steering injector and this one are chained.
     */
    @SafeVarargs
    public static @NotNull BiFunction<ChatRequest, Object, ChatRequest> chain(
            @Nullable BiFunction<ChatRequest, Object, ChatRequest>... transformers) {
        return (request, memoryId) -> {
            ChatRequest current = request;
            for (BiFunction<ChatRequest, Object, ChatRequest> t : transformers) {
                if (t != null) {
                    current = t.apply(current, memoryId);
                }
            }
            return current;
        };
    }

    @Override
    public ChatRequest apply(ChatRequest request, Object memoryId) {
        ChatRequest result = request;
        try {
            List<ChatMessage> messages = request.messages();
            ToolResultCompactor compactor = context.getCompactor();
            if (compactor != null) {
                ToolResultCompactor.Result compacted = compactor.compact(messages);
                if (compacted.compacted() > 0) {
                    context.getMetrics().recordCompaction(compacted.compacted(), compacted.charsSaved());
                    messages = compacted.messages();
                }
            }

            ChatRequestParameters parameters = request.parameters();
            ChatRequestParameters newParameters = withVisibleTools(parameters);

            if (messages != request.messages() || newParameters != parameters) {
                result = ChatRequest.builder()
                        .messages(messages)
                        .parameters(newParameters)
                        .build();
            }
        } catch (Exception e) {
            // Efficiency features must never break a run: fall back to the unmodified request.
            log.warn("Agent request transformation failed; sending the request unchanged", e);
            result = request;
        }
        context.getMetrics().recordRequest(estimateChars(result));
        return result;
    }

    private @Nullable ChatRequestParameters withVisibleTools(@Nullable ChatRequestParameters parameters) {
        DeferredToolRegistry registry = context.getDeferredTools();
        if (parameters == null || !registry.isActive()) {
            return parameters;
        }
        List<ToolSpecification> specs = parameters.toolSpecifications();
        if (specs == null || specs.isEmpty()) {
            return parameters;
        }
        List<ToolSpecification> visible = registry.visible(specs);
        if (visible.size() == specs.size()) {
            return parameters;
        }
        // AiServices always builds DefaultChatRequestParameters. Rebuilding any other
        // (provider-specific) subtype through the default builder would drop its fields, so
        // in that case send every tool rather than risk a changed request.
        if (parameters.getClass() != DefaultChatRequestParameters.class) {
            return parameters;
        }
        context.getMetrics().recordHiddenToolSpecs(specs.size() - visible.size());
        return ChatRequestParameters.builder()
                .overrideWith(parameters)
                .toolSpecifications(visible)
                .build();
    }

    /** Approximate request size in characters: message text plus tool definitions. */
    static long estimateChars(@NotNull ChatRequest request) {
        long chars = 0;
        for (ChatMessage m : request.messages()) {
            chars += messageChars(m);
        }
        List<ToolSpecification> specs = request.toolSpecifications();
        if (specs != null) {
            for (ToolSpecification spec : specs) {
                chars += spec.name().length();
                if (spec.description() != null) chars += spec.description().length();
                if (spec.parameters() != null) chars += spec.parameters().toString().length();
            }
        }
        return chars;
    }

    private static long messageChars(@NotNull ChatMessage m) {
        try {
            if (m instanceof ToolExecutionResultMessage tr) {
                return tr.text() != null ? tr.text().length() : 0;
            }
            if (m instanceof UserMessage um) {
                return um.hasSingleText() ? um.singleText().length() : um.contents().toString().length();
            }
            if (m instanceof AiMessage ai) {
                long n = ai.text() != null ? ai.text().length() : 0;
                if (ai.hasToolExecutionRequests()) {
                    n += ai.toolExecutionRequests().stream()
                            .mapToLong(r -> r.name().length() + (r.arguments() != null ? r.arguments().length() : 0))
                            .sum();
                }
                return n;
            }
            if (m instanceof SystemMessage sm) {
                return sm.text().length();
            }
        } catch (Exception ignored) {
            // Non-text content: fall through to toString.
        }
        return m.toString().length();
    }
}
