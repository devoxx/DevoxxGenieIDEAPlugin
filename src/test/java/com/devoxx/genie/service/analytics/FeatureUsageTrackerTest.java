package com.devoxx.genie.service.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FeatureUsageTracker} (task-209, ACs #18, #26).
 *
 * <p>The tracker is a thin facade and its main behavior (emit at most one event per
 * activated feature, with the right bucket) is covered indirectly by
 * {@code AnalyticsSessionSnapshotServiceTest} + {@code AnalyticsEventBuilderTest}. This
 * file adds focused rule checks that don't require mocking the full analytics pipeline.
 */
class FeatureUsageTrackerTest {

    @Test
    void allFeatureIdsHaveWireValuesMatchingSchemaDoc() {
        // Guardrail: the closed enum must always match docs/analytics-schema.md.
        assertThat(FeatureId.RAG.wireValue()).isEqualTo("rag");
        assertThat(FeatureId.SEMANTIC_SEARCH.wireValue()).isEqualTo("semantic_search");
        assertThat(FeatureId.WEB_SEARCH_GOOGLE.wireValue()).isEqualTo("web_search_google");
        assertThat(FeatureId.WEB_SEARCH_TAVILY.wireValue()).isEqualTo("web_search_tavily");
        assertThat(FeatureId.AGENT.wireValue()).isEqualTo("agent");
        assertThat(FeatureId.MCP.wireValue()).isEqualTo("mcp");
        assertThat(FeatureId.STREAMING.wireValue()).isEqualTo("streaming");
        assertThat(FeatureId.PROJECT_CONTEXT_FULL.wireValue()).isEqualTo("project_context_full");
        assertThat(FeatureId.PROJECT_CONTEXT_SELECTED.wireValue()).isEqualTo("project_context_selected");
        assertThat(FeatureId.DEVOXXGENIE_MD.wireValue()).isEqualTo("devoxxgenie_md");
        assertThat(FeatureId.CUSTOM_PROMPT.wireValue()).isEqualTo("custom_prompt");
    }

    @Test
    void exactlyTheRightFeaturesAreFlaggedUsageOnly() {
        // Task-209 AC #21: usage-only feature_ids must never appear in feature_enabled snapshots.
        assertThat(FeatureId.SEMANTIC_SEARCH.isUsageOnly()).isTrue();
        assertThat(FeatureId.PROJECT_CONTEXT_FULL.isUsageOnly()).isTrue();
        assertThat(FeatureId.PROJECT_CONTEXT_SELECTED.isUsageOnly()).isTrue();
        assertThat(FeatureId.DEVOXXGENIE_MD.isUsageOnly()).isTrue();

        // Everything else is snapshot-eligible.
        assertThat(FeatureId.RAG.isUsageOnly()).isFalse();
        assertThat(FeatureId.WEB_SEARCH_GOOGLE.isUsageOnly()).isFalse();
        assertThat(FeatureId.WEB_SEARCH_TAVILY.isUsageOnly()).isFalse();
        assertThat(FeatureId.AGENT.isUsageOnly()).isFalse();
        assertThat(FeatureId.MCP.isUsageOnly()).isFalse();
        assertThat(FeatureId.STREAMING.isUsageOnly()).isFalse();
        assertThat(FeatureId.CUSTOM_PROMPT.isUsageOnly()).isFalse();
    }

    @Test
    void fromWireValueRoundTrips() {
        for (FeatureId id : FeatureId.values()) {
            assertThat(FeatureId.fromWireValue(id.wireValue())).contains(id);
        }
    }

    @Test
    void fromWireValueRejectsUnknown() {
        assertThat(FeatureId.fromWireValue("not_a_feature")).isEmpty();
    }

    @Test
    void semanticSearchUsedWithNullModelDoesNotThrow() {
        // Fail-silent: null model → provider_type=none, event still goes through the allowlist.
        // No observable assertion here — test passes if no exception bubbles.
        FeatureUsageTracker.semanticSearchUsed(null);
    }
}
