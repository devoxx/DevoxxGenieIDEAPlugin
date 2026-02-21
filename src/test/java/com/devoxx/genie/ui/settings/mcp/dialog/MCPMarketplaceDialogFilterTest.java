package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.registry.MCPRegistryPackage;
import com.devoxx.genie.model.mcp.registry.MCPRegistryRemote;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerEntry;
import com.devoxx.genie.model.mcp.registry.MCPRegistryServerInfo;
import com.devoxx.genie.service.mcp.MCPRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MCPMarketplaceDialog.ServerEntryFilter — the pure filter logic
 * extracted from applyFilters() to reduce cognitive complexity.
 */
class MCPMarketplaceDialogFilterTest {

    private MCPMarketplaceDialog.ServerEntryFilter filter;
    private final MCPRegistryService registryService = new MCPRegistryService();

    @BeforeEach
    void setUp() {
        filter = new MCPMarketplaceDialog.ServerEntryFilter();
    }

    // ─── matchesText ───────────────────────────────────────────

    @Nested
    class MatchesText {

        @Test
        void emptyQueryMatchesAll() {
            MCPRegistryServerInfo info = serverInfo("filesystem", "Manages files");
            assertThat(filter.matchesText(info, "")).isTrue();
        }

        @Test
        void matchesByName() {
            MCPRegistryServerInfo info = serverInfo("filesystem-server", "");
            assertThat(filter.matchesText(info, "filesystem")).isTrue();
        }

        @Test
        void matchesByDescription() {
            MCPRegistryServerInfo info = serverInfo("my-server", "Manages files on disk");
            assertThat(filter.matchesText(info, "files on disk")).isTrue();
        }

        @Test
        void isCaseInsensitive() {
            MCPRegistryServerInfo info = serverInfo("FileSystem", "");
            assertThat(filter.matchesText(info, "filesystem")).isTrue();
        }

        @Test
        void noMatchReturnsFalse() {
            MCPRegistryServerInfo info = serverInfo("database-server", "SQL database");
            assertThat(filter.matchesText(info, "filesystem")).isFalse();
        }

        @Test
        void nullNameHandledGracefully() {
            MCPRegistryServerInfo info = new MCPRegistryServerInfo();
            info.setDescription("some description");
            assertThat(filter.matchesText(info, "some")).isTrue();
        }

        @Test
        void nullDescriptionHandledGracefully() {
            MCPRegistryServerInfo info = new MCPRegistryServerInfo();
            info.setName("my-server");
            assertThat(filter.matchesText(info, "my-server")).isTrue();
        }
    }

    // ─── matchesLocation ──────────────────────────────────────

    @Nested
    class MatchesLocation {

        @Test
        void nullLocationMatchesAll() {
            assertThat(filter.matchesLocation(remoteServerInfo(), null)).isTrue();
            assertThat(filter.matchesLocation(localServerInfo(), null)).isTrue();
        }

        @Test
        void allFilterMatchesBoth() {
            assertThat(filter.matchesLocation(remoteServerInfo(), "All")).isTrue();
            assertThat(filter.matchesLocation(localServerInfo(), "All")).isTrue();
        }

        @Test
        void remoteFilterMatchesRemoteServers() {
            assertThat(filter.matchesLocation(remoteServerInfo(), "Remote")).isTrue();
        }

        @Test
        void remoteFilterExcludesLocalServers() {
            assertThat(filter.matchesLocation(localServerInfo(), "Remote")).isFalse();
        }

        @Test
        void localFilterMatchesLocalServers() {
            assertThat(filter.matchesLocation(localServerInfo(), "Local")).isTrue();
        }

        @Test
        void localFilterExcludesRemoteServers() {
            assertThat(filter.matchesLocation(remoteServerInfo(), "Local")).isFalse();
        }

        @Test
        void emptyRemoteListCountsAsLocal() {
            MCPRegistryServerInfo info = new MCPRegistryServerInfo();
            info.setRemotes(List.of());
            assertThat(filter.matchesLocation(info, "Local")).isTrue();
            assertThat(filter.matchesLocation(info, "Remote")).isFalse();
        }
    }

    // ─── matchesType ──────────────────────────────────────────

    @Nested
    class MatchesType {

        @Test
        void nullTypeMatchesAll() {
            MCPRegistryServerInfo info = npmServerInfo();
            assertThat(filter.matchesType(info, null, registryService)).isTrue();
        }

        @Test
        void allFilterMatchesAnyType() {
            assertThat(filter.matchesType(npmServerInfo(), "All", registryService)).isTrue();
            assertThat(filter.matchesType(remoteServerInfo(), "All", registryService)).isTrue();
        }

        @Test
        void npmTypeMatchesNpmServer() {
            assertThat(filter.matchesType(npmServerInfo(), "npm", registryService)).isTrue();
        }

        @Test
        void npmTypeDoesNotMatchRemoteServer() {
            assertThat(filter.matchesType(remoteServerInfo(), "npm", registryService)).isFalse();
        }

        @Test
        void remoteTypeMatchesRemoteServer() {
            assertThat(filter.matchesType(remoteServerInfo(), "Remote", registryService)).isTrue();
        }
    }

    // ─── matches (combined) ───────────────────────────────────

    @Nested
    class Matches {

        @Test
        void entryWithNullServerInfoReturnsFalse() {
            MCPRegistryServerEntry entry = new MCPRegistryServerEntry();
            assertThat(filter.matches(entry, "", "All", "All", registryService)).isFalse();
        }

        @Test
        void allFiltersPassReturnsTrue() {
            MCPRegistryServerEntry entry = entryOf(serverInfo("filesystem", "Manages files"));
            assertThat(filter.matches(entry, "file", "All", "All", registryService)).isTrue();
        }

        @Test
        void textMismatchReturnsFalse() {
            MCPRegistryServerEntry entry = entryOf(serverInfo("database", "SQL"));
            assertThat(filter.matches(entry, "filesystem", "All", "All", registryService)).isFalse();
        }

        @Test
        void locationMismatchReturnsFalse() {
            MCPRegistryServerEntry entry = entryOf(remoteServerInfo());
            assertThat(filter.matches(entry, "", "Local", "All", registryService)).isFalse();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    private static MCPRegistryServerInfo serverInfo(String name, String description) {
        MCPRegistryServerInfo info = new MCPRegistryServerInfo();
        info.setName(name);
        info.setDescription(description);
        return info;
    }

    private static MCPRegistryServerInfo remoteServerInfo() {
        MCPRegistryRemote remote = new MCPRegistryRemote();
        remote.setUrl("https://mcp.example.com/v1");
        MCPRegistryServerInfo info = new MCPRegistryServerInfo();
        info.setName("remote-server");
        info.setRemotes(List.of(remote));
        return info;
    }

    private static MCPRegistryServerInfo localServerInfo() {
        MCPRegistryPackage pkg = new MCPRegistryPackage();
        pkg.setRegistryType("npm");
        pkg.setIdentifier("@example/server");
        MCPRegistryServerInfo info = new MCPRegistryServerInfo();
        info.setName("local-server");
        info.setPackages(List.of(pkg));
        return info;
    }

    private static MCPRegistryServerInfo npmServerInfo() {
        MCPRegistryPackage pkg = new MCPRegistryPackage();
        pkg.setRegistryType("npm");
        pkg.setIdentifier("@example/mcp");
        MCPRegistryServerInfo info = new MCPRegistryServerInfo();
        info.setName("npm-server");
        info.setPackages(List.of(pkg));
        return info;
    }

    private static MCPRegistryServerEntry entryOf(MCPRegistryServerInfo info) {
        MCPRegistryServerEntry entry = new MCPRegistryServerEntry();
        entry.setServer(info);
        return entry;
    }
}
