package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.registry.MCPRegistryMetadata;
import com.devoxx.genie.model.mcp.registry.MCPRegistryResponse;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerEntry;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerInfo;
import com.devoxx.genie.service.mcp.MCPRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MCPMarketplaceDialog.PagingState — the accumulate/replace/cursor logic
 * extracted from the dialog so it can be tested without the IntelliJ platform.
 */
class MCPMarketplaceDialogPagingTest {

    private MCPMarketplaceDialog.PagingState state;

    @BeforeEach
    void setUp() {
        state = new MCPMarketplaceDialog.PagingState();
    }

    @Test
    void newStateIsEmptyWithNoMorePages() {
        assertThat(state.size()).isZero();
        assertThat(state.getLoadedServers()).isEmpty();
        assertThat(state.getNextCursor()).isNull();
        assertThat(state.hasMorePages()).isFalse();
        assertThat(state.getQuery()).isEmpty();
    }

    @Test
    void replaceLoadsFirstPageAndTracksCursor() {
        state.apply(page(List.of(entry("a"), entry("b")), "cursor-2"), false);

        assertThat(state.size()).isEqualTo(2);
        assertThat(state.getNextCursor()).isEqualTo("cursor-2");
        assertThat(state.hasMorePages()).isTrue();
    }

    @Test
    void appendAccumulatesAcrossPages() {
        state.apply(page(List.of(entry("a"), entry("b")), "cursor-2"), false);
        state.apply(page(List.of(entry("c")), null), true);

        assertThat(state.size()).isEqualTo(3);
        assertThat(serverNames(state)).containsExactly("a", "b", "c");
        assertThat(state.hasMorePages()).isFalse();
    }

    @Test
    void replaceClearsPreviouslyLoadedServers() {
        state.apply(page(List.of(entry("old-1"), entry("old-2")), "cursor-2"), false);

        // A new search replaces, rather than appends.
        state.apply(page(List.of(entry("new-1")), null), false);

        assertThat(state.size()).isEqualTo(1);
        assertThat(serverNames(state)).containsExactly("new-1");
        assertThat(state.getNextCursor()).isNull();
    }

    @Test
    void blankCursorMeansNoMorePages() {
        state.apply(page(List.of(entry("a")), "   "), false);
        assertThat(state.hasMorePages()).isFalse();
    }

    @Test
    void nullResponseClearsOnReplaceAndIsHandledGracefully() {
        state.apply(page(List.of(entry("a")), "cursor-2"), false);

        state.apply(null, false);

        assertThat(state.size()).isZero();
        assertThat(state.getNextCursor()).isNull();
        assertThat(state.hasMorePages()).isFalse();
    }

    @Test
    void responseWithNullServerListIsHandledGracefully() {
        MCPRegistryResponse response = new MCPRegistryResponse(); // servers == null
        state.apply(response, false);

        assertThat(state.size()).isZero();
        assertThat(state.hasMorePages()).isFalse();
    }

    @Test
    void setQueryTrackedAndNullCoercedToEmpty() {
        state.setQuery("github");
        assertThat(state.getQuery()).isEqualTo("github");

        state.setQuery(null);
        assertThat(state.getQuery()).isEmpty();
    }

    @Test
    void visibleServersAppliesCurrentQueryToLoadedPagesBeforeServerResponds() {
        // First page loaded without a query...
        state.apply(page(List.of(entry("ac.inference.sh/mcp"), entry("io.github.upstash/context7")), "cursor-2"), false);

        // ...then the user types a query. The registry search is slow, so until its response
        // replaces the loaded page the already-loaded rows must be narrowed locally.
        state.setQuery("context7");

        List<MCPRegistryServerEntry> visible = state.visibleServers("All", "All", new MCPRegistryService());

        assertThat(visible).extracting(e -> e.getServer().getName())
                .containsExactly("io.github.upstash/context7");
    }

    @Test
    void visibleServersMatchesQueryCaseInsensitivelyOnNameOrDescription() {
        MCPRegistryServerEntry byDescription = entry("com.example/docs");
        byDescription.getServer().setDescription("Up-to-date Context7 style documentation");
        state.apply(page(List.of(entry("other"), byDescription), null), false);

        state.setQuery("CONTEXT7");

        assertThat(state.visibleServers("All", "All", new MCPRegistryService()))
                .extracting(e -> e.getServer().getName())
                .containsExactly("com.example/docs");
    }

    // ─── Helpers ──────────────────────────────────────────────

    private static MCPRegistryServerEntry entry(String name) {
        MCPRegistryServerInfo info = new MCPRegistryServerInfo();
        info.setName(name);
        MCPRegistryServerEntry entry = new MCPRegistryServerEntry();
        entry.setServer(info);
        return entry;
    }

    private static MCPRegistryResponse page(List<MCPRegistryServerEntry> servers, String nextCursor) {
        MCPRegistryResponse response = new MCPRegistryResponse();
        response.setServers(servers);
        if (nextCursor != null) {
            MCPRegistryMetadata metadata = new MCPRegistryMetadata();
            metadata.setNextCursor(nextCursor);
            response.setMetadata(metadata);
        }
        return response;
    }

    private static List<String> serverNames(MCPMarketplaceDialog.PagingState state) {
        return state.getLoadedServers().stream()
                .map(e -> e.getServer().getName())
                .toList();
    }
}
