package com.devoxx.genie.service.analytics;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thin static facade over {@link AnalyticsService#trackFeatureUsed} (task-209, ACs #18, #19,
 * #23, #26).
 *
 * <p>Centralizes the "one event per activated feature per prompt" rule and the
 * {@code tool_call_count} convention — only {@link FeatureId#AGENT} and {@link FeatureId#MCP}
 * carry a meaningful count; all other usage events send {@code "0"}.
 *
 * <p>All methods are fail-silent: any exception is logged at debug level and swallowed so
 * analytics never bubble into the user's prompt execution path.
 */
@Slf4j
public final class FeatureUsageTracker {

    private static final String ZERO_BUCKET = "0";

    private FeatureUsageTracker() {
        // utility
    }

    /**
     * Emits {@code feature_used} events for every feature activated during the given prompt.
     * Called from {@code PromptExecutionService} at prompt completion. Reads only the
     * activation flags and per-prompt counters on {@link ChatMessageContext} — never any
     * user content.
     *
     * <p>Does NOT emit the {@code agent} event — the agent orchestrator emits that
     * separately via {@link #agentCompleted}, since the {@code AgentLoopTracker} instance
     * that holds the call count lives in the strategy, not the context.
     */
    public static void emitForPrompt(@NotNull ChatMessageContext context) {
        try {
            ProviderType providerType = resolveProviderType(context);
            DevoxxGenieStateService state = safeState();

            if (state != null && Boolean.TRUE.equals(state.getStreamMode())) {
                emit(FeatureId.STREAMING, providerType, ZERO_BUCKET);
            }
            if (context.isRagActivated()) {
                emit(FeatureId.RAG, providerType, ZERO_BUCKET);
            }
            if (context.isWebSearchActivated() && state != null) {
                if (state.isGoogleSearchEnabled()) {
                    emit(FeatureId.WEB_SEARCH_GOOGLE, providerType, ZERO_BUCKET);
                }
                if (state.isTavilySearchEnabled()) {
                    emit(FeatureId.WEB_SEARCH_TAVILY, providerType, ZERO_BUCKET);
                }
            }
            if (context.getCommandName() != null && !context.getCommandName().isEmpty()) {
                emit(FeatureId.CUSTOM_PROMPT, providerType, ZERO_BUCKET);
            }
            if (context.isProjectContextFullUsed()) {
                emit(FeatureId.PROJECT_CONTEXT_FULL, providerType, ZERO_BUCKET);
            }
            if (context.isProjectContextSelectedUsed()) {
                emit(FeatureId.PROJECT_CONTEXT_SELECTED, providerType, ZERO_BUCKET);
            }
            if (context.isDevoxxGenieMdUsed()) {
                emit(FeatureId.DEVOXXGENIE_MD, providerType, ZERO_BUCKET);
            }

            int mcpCalls = context.getMcpCallCount() != null ? context.getMcpCallCount().get() : 0;
            if (mcpCalls > 0) {
                emit(FeatureId.MCP, providerType, Buckets.standard(mcpCalls));
            }
        } catch (Exception e) {
            log.debug("FeatureUsageTracker.emitForPrompt skipped: {}", e.getMessage());
        }
    }

    /**
     * Emits a single {@code feature_used} event for {@link FeatureId#AGENT} with the
     * bucketed tool-call count. Called from the agent orchestrators
     * ({@code StreamingPromptStrategy}, {@code NonStreamingPromptExecutionService},
     * {@code SubAgentRunner}) after the chat finishes.
     */
    public static void agentCompleted(@NotNull ChatMessageContext context, int toolCallCount) {
        try {
            emit(FeatureId.AGENT, resolveProviderType(context), Buckets.standard(toolCallCount));
        } catch (Exception e) {
            log.debug("FeatureUsageTracker.agentCompleted skipped: {}", e.getMessage());
        }
    }

    /** Fires the feature_used event for {@link FeatureId#SEMANTIC_SEARCH}. */
    public static void semanticSearchUsed(@Nullable LanguageModel model) {
        try {
            emit(FeatureId.SEMANTIC_SEARCH, resolveProviderType(model), ZERO_BUCKET);
        } catch (Exception e) {
            log.debug("FeatureUsageTracker.semanticSearchUsed skipped: {}", e.getMessage());
        }
    }

    private static void emit(@NotNull FeatureId feature, @NotNull ProviderType type, @NotNull String bucket) {
        AnalyticsService.getInstance().trackFeatureUsed(feature, type, bucket);
    }

    @NotNull
    private static ProviderType resolveProviderType(@NotNull ChatMessageContext context) {
        return resolveProviderType(context.getLanguageModel());
    }

    @NotNull
    private static ProviderType resolveProviderType(@Nullable LanguageModel model) {
        if (model == null || model.getProvider() == null) {
            return ProviderType.NONE;
        }
        return ProviderType.fromModelProvider(model.getProvider());
    }

    @Nullable
    private static DevoxxGenieStateService safeState() {
        try {
            return DevoxxGenieStateService.getInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
