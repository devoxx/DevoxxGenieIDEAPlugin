package com.devoxx.genie.service.agent.loop;

import com.devoxx.genie.service.agent.AgentApprovalProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static com.devoxx.genie.model.Constant.AGENT_COMPACT_HEAD_CHARS;
import static com.devoxx.genie.model.Constant.AGENT_COMPACT_KEEP_RECENT_RESULTS;
import static com.devoxx.genie.model.Constant.AGENT_COMPACT_MIN_CHARS;
import static com.devoxx.genie.model.Constant.AGENT_DEFER_MCP_TOOLS_THRESHOLD;

/**
 * State shared by the efficiency features of one agent run (one prompt): metrics, the
 * read-only call cache, the deferred-tool registry and the tool-result compactor. Features
 * that are switched off in settings are {@code null} (cache, compactor) or inactive
 * (registry with nothing deferred).
 */
public class AgentRunContext {

    /**
     * Tools that neither modify anything nor should be cached: meta tools, skill activation
     * and sub-agent exploration (whose results depend on a non-deterministic LLM).
     */
    public static final Set<String> NEUTRAL_TOOLS = Set.of(
            SearchToolsToolExecutor.TOOL_NAME, "activate_skill", "read_skill_resource", "parallel_explore");

    private final AgentRunMetrics metrics = new AgentRunMetrics();
    private final DeferredToolRegistry deferredTools = new DeferredToolRegistry();
    private final @Nullable ToolCallCache callCache;
    private final @Nullable ToolResultCompactor compactor;
    private final boolean deferMcpTools;
    private final int deferMcpToolsThreshold;

    public AgentRunContext(boolean deduplicate, boolean compact, boolean deferMcpTools, int deferMcpToolsThreshold) {
        this.callCache = deduplicate
                ? new ToolCallCache(AgentApprovalProvider.READ_ONLY_TOOLS, NEUTRAL_TOOLS)
                : null;
        this.compactor = compact
                ? new ToolResultCompactor(AGENT_COMPACT_KEEP_RECENT_RESULTS, AGENT_COMPACT_MIN_CHARS, AGENT_COMPACT_HEAD_CHARS)
                : null;
        this.deferMcpTools = deferMcpTools;
        this.deferMcpToolsThreshold = Math.max(0, deferMcpToolsThreshold);
    }

    /** Context with every efficiency feature off; metrics are still collected. */
    public static @NotNull AgentRunContext disabled() {
        return new AgentRunContext(false, false, false, Integer.MAX_VALUE);
    }

    /** Context configured from the user's agent settings. */
    public static @NotNull AgentRunContext fromSettings(@NotNull DevoxxGenieStateService settings) {
        Integer threshold = settings.getAgentDeferMcpToolsThreshold();
        return new AgentRunContext(
                !Boolean.FALSE.equals(settings.getAgentDeduplicateToolCalls()),
                !Boolean.FALSE.equals(settings.getAgentCompactToolResults()),
                !Boolean.FALSE.equals(settings.getAgentDeferMcpTools()),
                threshold != null ? threshold : AGENT_DEFER_MCP_TOOLS_THRESHOLD);
    }

    public @NotNull AgentRunMetrics getMetrics() {
        return metrics;
    }

    public @NotNull DeferredToolRegistry getDeferredTools() {
        return deferredTools;
    }

    public @Nullable ToolCallCache getCallCache() {
        return callCache;
    }

    public @Nullable ToolResultCompactor getCompactor() {
        return compactor;
    }

    public boolean isDeferMcpTools() {
        return deferMcpTools;
    }

    public int getDeferMcpToolsThreshold() {
        return deferMcpToolsThreshold;
    }

    /** The per-round-trip request transformer implementing compaction, deferral and metrics. */
    public @NotNull AgentRequestTransformer requestTransformer() {
        return new AgentRequestTransformer(this);
    }
}
