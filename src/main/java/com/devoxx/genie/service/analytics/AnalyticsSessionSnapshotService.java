package com.devoxx.genie.service.analytics;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Emits the per-IDE-session feature-enablement snapshot exactly once per session (task-209,
 * ACs #1, #19, #22, #28).
 *
 * <p>For each tracked feature toggled ON in {@link DevoxxGenieStateService}, publishes a
 * {@code feature_enabled} event via {@link AnalyticsService#trackFeatureEnabled(FeatureId)}.
 * It also publishes a single {@code feature_counts} event with bucketed MCP server / custom
 * prompt / chat memory counts.
 *
 * <p>Guarded by an {@link AtomicBoolean} so repeated {@link #snapshotIfNeeded()} calls (e.g.
 * from {@code PostStartupActivity} on every project open) only fire once. On settings change
 * — delivered via the {@link DevoxxGenieSettingsChangedTopic} MessageBus topic — the guard
 * clears and the next {@code snapshotIfNeeded()} re-emits.
 */
@Slf4j
@Service(Service.Level.APP)
public final class AnalyticsSessionSnapshotService implements DevoxxGenieSettingsChangedTopic {

    /**
     * Narrow sink interface — lets tests inject a recording fake without subclassing the
     * (intentionally {@code final}) {@link AnalyticsService}.
     */
    interface FeatureEventSink {
        void trackFeatureEnabled(@NotNull FeatureId featureId);
        void trackFeatureCounts(@NotNull String mcpServerCountBucket,
                                @NotNull String customPromptCountBucket,
                                @NotNull String chatMemoryBucket);
    }

    private final AtomicBoolean sent = new AtomicBoolean(false);
    private final FeatureEventSink sink;

    @SuppressWarnings("unused") // APP-level @Service instantiation
    public AnalyticsSessionSnapshotService() {
        this(defaultSink());
        subscribeToSettingsChanges();
    }

    @TestOnly
    AnalyticsSessionSnapshotService(@NotNull FeatureEventSink sink) {
        this.sink = sink;
    }

    @NotNull
    private static FeatureEventSink defaultSink() {
        AnalyticsService svc = AnalyticsService.getInstance();
        return new FeatureEventSink() {
            @Override
            public void trackFeatureEnabled(@NotNull FeatureId featureId) {
                svc.trackFeatureEnabled(featureId);
            }

            @Override
            public void trackFeatureCounts(@NotNull String mcp,
                                           @NotNull String custom,
                                           @NotNull String memory) {
                svc.trackFeatureCounts(mcp, custom, memory);
            }
        };
    }

    @NotNull
    public static AnalyticsSessionSnapshotService getInstance() {
        return ApplicationManager.getApplication().getService(AnalyticsSessionSnapshotService.class);
    }

    /**
     * Emits the feature-enablement snapshot if it hasn't been sent yet in this IDE session
     * (or has been re-armed by a settings change). Safe to call repeatedly; fire-and-forget.
     */
    public void snapshotIfNeeded() {
        if (!sent.compareAndSet(false, true)) {
            return;
        }
        try {
            emitSnapshot();
        } catch (Exception e) {
            log.debug("Analytics session snapshot skipped: {}", e.getMessage());
        }
    }

    /** Re-arms the snapshot on the next {@link #snapshotIfNeeded()} call. */
    @Override
    public void settingsChanged() {
        sent.set(false);
    }

    private void emitSnapshot() {
        DevoxxGenieStateService state;
        try {
            state = DevoxxGenieStateService.getInstance();
        } catch (Exception e) {
            log.debug("Analytics session snapshot skipped: state service unavailable ({})", e.getMessage());
            return;
        }

        emitIfEnabled(state.getRagEnabled(), FeatureId.RAG);
        emitIfEnabled(state.getMcpEnabled(), FeatureId.MCP);
        emitIfEnabled(state.getAgentModeEnabled(), FeatureId.AGENT);
        emitIfEnabled(state.getStreamMode(), FeatureId.STREAMING);
        emitIfEnabled(state.isGoogleSearchEnabled(), FeatureId.WEB_SEARCH_GOOGLE);
        emitIfEnabled(state.isTavilySearchEnabled(), FeatureId.WEB_SEARCH_TAVILY);
        emitIfEnabled(hasAnyCustomPrompt(state), FeatureId.CUSTOM_PROMPT);

        sink.trackFeatureCounts(
                Buckets.standard(mcpServerCount(state)),
                Buckets.standard(customPromptCount(state)),
                Buckets.chatMemory(chatMemorySize(state))
        );
    }

    private void emitIfEnabled(boolean enabled, @NotNull FeatureId featureId) {
        if (enabled) {
            sink.trackFeatureEnabled(featureId);
        }
    }

    private void emitIfEnabled(Boolean enabled, @NotNull FeatureId featureId) {
        emitIfEnabled(Boolean.TRUE.equals(enabled), featureId);
    }

    private boolean hasAnyCustomPrompt(@NotNull DevoxxGenieStateService state) {
        return customPromptCount(state) > 0;
    }

    private int customPromptCount(@NotNull DevoxxGenieStateService state) {
        List<CustomPrompt> prompts = state.getCustomPrompts();
        return prompts == null ? 0 : prompts.size();
    }

    private int mcpServerCount(@NotNull DevoxxGenieStateService state) {
        if (state.getMcpSettings() == null) return 0;
        Map<String, MCPServer> servers = state.getMcpSettings().getMcpServers();
        return servers == null ? 0 : servers.size();
    }

    private int chatMemorySize(@NotNull DevoxxGenieStateService state) {
        Integer size = state.getChatMemorySize();
        return size == null ? 0 : size;
    }

    private void subscribeToSettingsChanges() {
        try {
            ApplicationManager.getApplication().getMessageBus()
                    .connect()
                    .subscribe(DevoxxGenieSettingsChangedTopic.TOPIC, this);
        } catch (Exception e) {
            log.debug("Analytics session snapshot subscriber not attached: {}", e.getMessage());
        }
    }

    @TestOnly
    boolean isSentForTest() {
        return sent.get();
    }

    @TestOnly
    void resetForTest() {
        sent.set(false);
    }
}
