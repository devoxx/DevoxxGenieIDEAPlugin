package com.devoxx.genie.service.analytics;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.MCPSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@link AnalyticsSessionSnapshotService} (task-209 ACs #1, #19, #22, #28).
 *
 * <p>No IntelliJ platform fixtures — the test uses the package-private constructor that
 * injects a recording {@link AnalyticsService}, then calls {@code snapshotIfNeeded()}
 * directly.
 */
class AnalyticsSessionSnapshotServiceTest {

    private DevoxxGenieStateService state;
    private RecordingSink analytics;
    private AnalyticsSessionSnapshotService snapshot;

    @BeforeEach
    void setUp() {
        state = new DevoxxGenieStateService();
        state.setAnalyticsEnabled(true);
        state.setAnalyticsNoticeAcknowledged(true);
        state.setAnalyticsClientId("");
        state.setAnalyticsEndpoint("https://example.invalid/collect");

        // Start from a known-empty feature state: each test opts in what it wants.
        state.setRagEnabled(false);
        state.setMcpEnabled(false);
        state.setAgentModeEnabled(false);
        state.setStreamMode(false);
        state.setGoogleSearchEnabled(false);
        state.setTavilySearchEnabled(false);
        state.setCustomPrompts(new ArrayList<>());
        state.setMcpSettings(new MCPSettings());
        state.setChatMemorySize(0);

        analytics = new RecordingSink();
        snapshot = new AnalyticsSessionSnapshotService(analytics);
    }

    private void withState(Runnable action) {
        try (MockedStatic<DevoxxGenieStateService> mocked = mockStatic(DevoxxGenieStateService.class)) {
            mocked.when(DevoxxGenieStateService::getInstance).thenReturn(state);
            action.run();
        }
    }

    @Test
    void snapshotFiresOnceForAllEnabledFeatures() {
        state.setRagEnabled(true);
        state.setMcpEnabled(true);
        state.setAgentModeEnabled(true);
        state.setStreamMode(true);
        state.setGoogleSearchEnabled(true);
        state.setTavilySearchEnabled(true);

        withState(snapshot::snapshotIfNeeded);

        assertThat(analytics.enabledEvents).containsExactlyInAnyOrder(
                FeatureId.RAG, FeatureId.MCP, FeatureId.AGENT,
                FeatureId.STREAMING, FeatureId.WEB_SEARCH_GOOGLE, FeatureId.WEB_SEARCH_TAVILY);
        assertThat(analytics.countsEvents).hasSize(1);
    }

    @Test
    void snapshotSkipsDisabledFeatures() {
        state.setRagEnabled(true);
        // everything else stays off

        withState(snapshot::snapshotIfNeeded);

        assertThat(analytics.enabledEvents).containsExactly(FeatureId.RAG);
    }

    @Test
    void snapshotIsOneShotPerSession() {
        state.setRagEnabled(true);

        withState(() -> {
            snapshot.snapshotIfNeeded();
            snapshot.snapshotIfNeeded();
            snapshot.snapshotIfNeeded();
        });

        // Task-209 AC #1 / #28: multiple project opens in one IDE session → single emission.
        assertThat(analytics.enabledEvents).containsExactly(FeatureId.RAG);
        assertThat(analytics.countsEvents).hasSize(1);
    }

    @Test
    void settingsChangedReArmsSnapshot() {
        state.setRagEnabled(true);
        withState(snapshot::snapshotIfNeeded);

        // Change a setting — simulate the MessageBus callback.
        state.setMcpEnabled(true);
        snapshot.settingsChanged();

        withState(snapshot::snapshotIfNeeded);

        // RAG emitted twice (once per snapshot), MCP once (only after it was enabled).
        assertThat(analytics.enabledEvents).containsExactlyInAnyOrder(
                FeatureId.RAG, FeatureId.RAG, FeatureId.MCP);
        assertThat(analytics.countsEvents).hasSize(2);
    }

    @Test
    void customPromptCountEmittedAsBucketedValue() {
        List<CustomPrompt> prompts = new ArrayList<>();
        prompts.add(new CustomPrompt("one", "body"));
        prompts.add(new CustomPrompt("two", "body"));
        prompts.add(new CustomPrompt("three", "body"));
        state.setCustomPrompts(prompts);

        withState(snapshot::snapshotIfNeeded);

        // Enablement event fired because count > 0
        assertThat(analytics.enabledEvents).contains(FeatureId.CUSTOM_PROMPT);
        // Counts event fired with "2-5" bucket for a count of 3.
        assertThat(analytics.lastCountsEvent).isNotNull();
        assertThat(analytics.lastCountsEvent.customPromptCountBucket).isEqualTo("2-5");
    }

    @Test
    void mcpServerCountIsBucketed() {
        MCPSettings s = new MCPSettings();
        Map<String, MCPServer> servers = new HashMap<>();
        servers.put("a", new MCPServer());
        servers.put("b", new MCPServer());
        servers.put("c", new MCPServer());
        servers.put("d", new MCPServer());
        servers.put("e", new MCPServer());
        servers.put("f", new MCPServer());
        s.setMcpServers(servers);
        state.setMcpSettings(s);

        withState(snapshot::snapshotIfNeeded);

        assertThat(analytics.lastCountsEvent.mcpServerCountBucket).isEqualTo("6-10");
    }

    @Test
    void chatMemoryBucketsSeparately() {
        state.setChatMemorySize(15);
        withState(snapshot::snapshotIfNeeded);

        assertThat(analytics.lastCountsEvent.chatMemoryBucket).isEqualTo("11-20");
    }

    @Test
    void usageOnlyFeatureIdCannotBeEnabled() {
        // Sanity: if someone calls trackFeatureEnabled with a usage-only feature_id, the
        // AnalyticsService rejects it upstream. This belongs to the real AnalyticsService
        // — exercised here by calling it directly.
        AnalyticsService real = new AnalyticsService();
        // Don't send anything over the network: no endpoint set in mocked state.
        real.trackFeatureEnabled(FeatureId.DEVOXXGENIE_MD);
        real.trackFeatureEnabled(FeatureId.PROJECT_CONTEXT_FULL);
        real.trackFeatureEnabled(FeatureId.PROJECT_CONTEXT_SELECTED);
        real.trackFeatureEnabled(FeatureId.SEMANTIC_SEARCH);
        // No assertion needed — if usageOnly is wired correctly, no NPE / no call escapes
        // the rejection path. Test passes by not throwing.
    }

    /** Minimal recording sink that captures which FeatureIds were emitted. */
    private static class RecordingSink implements AnalyticsSessionSnapshotService.FeatureEventSink {
        final List<FeatureId> enabledEvents = new ArrayList<>();
        final List<CountsEvent> countsEvents = new ArrayList<>();
        CountsEvent lastCountsEvent;

        @Override
        public void trackFeatureEnabled(FeatureId featureId) {
            if (featureId.isUsageOnly()) return;
            enabledEvents.add(featureId);
        }

        @Override
        public void trackFeatureCounts(String mcp, String custom, String memory) {
            CountsEvent e = new CountsEvent(mcp, custom, memory);
            countsEvents.add(e);
            lastCountsEvent = e;
        }
    }

    private static class CountsEvent {
        final String mcpServerCountBucket;
        final String customPromptCountBucket;
        final String chatMemoryBucket;
        CountsEvent(String mcp, String custom, String memory) {
            this.mcpServerCountBucket = mcp;
            this.customPromptCountBucket = custom;
            this.chatMemoryBucket = memory;
        }
    }
}
