package com.devoxx.genie.service.analytics;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

/**
 * Closed allowlist of feature identifiers emitted by the analytics pipeline (task-209).
 *
 * <p>The string value is the GA4 event parameter — never the enum name — and is the ONLY
 * acceptable value for the {@code feature_id} param. All instrumentation must go through
 * this enum; no free-form strings.
 *
 * <p>Some features are <strong>usage-only</strong>: they are per-prompt or per-project
 * signals (e.g., full project context attached, DEVOXXGENIE.md injected) that do not have
 * a meaningful session-scoped enablement story. Passing a usage-only feature to
 * {@code trackFeatureEnabled} is a programming error and is rejected.
 */
public enum FeatureId {

    RAG("rag", false),
    SEMANTIC_SEARCH("semantic_search", true),
    WEB_SEARCH_GOOGLE("web_search_google", false),
    WEB_SEARCH_TAVILY("web_search_tavily", false),
    AGENT("agent", false),
    MCP("mcp", false),
    STREAMING("streaming", false),
    PROJECT_CONTEXT_FULL("project_context_full", true),
    PROJECT_CONTEXT_SELECTED("project_context_selected", true),
    DEVOXXGENIE_MD("devoxxgenie_md", true),
    CUSTOM_PROMPT("custom_prompt", false);

    private final String wireValue;
    private final boolean usageOnly;

    FeatureId(@NotNull String wireValue, boolean usageOnly) {
        this.wireValue = wireValue;
        this.usageOnly = usageOnly;
    }

    @NotNull
    public String wireValue() {
        return wireValue;
    }

    public boolean isUsageOnly() {
        return usageOnly;
    }

    @NotNull
    public static Optional<FeatureId> fromWireValue(@NotNull String value) {
        return Arrays.stream(values())
                .filter(f -> f.wireValue.equals(value))
                .findFirst();
    }
}
