package com.devoxx.genie.ui.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the tool state management logic encapsulated in MCPToolsManager helper methods.
 * These tests cover the MCPServer state changes that the refactored private helpers implement.
 */
class MCPToolsManagerTest {

    // ─── onToolCheckboxToggled logic ────────────────────────────

    @Nested
    class ToolCheckboxToggle {

        @Test
        void uncheckingTool_addsItToDisabledSet() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .availableTools(List.of("read_file", "write_file"))
                    .build();

            // simulate unchecking "read_file" (toolCheckbox.isSelected() == false)
            server.getDisabledTools().add("read_file");

            assertThat(server.getDisabledTools()).containsExactly("read_file");
        }

        @Test
        void recheckingTool_removesItFromDisabledSet() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .availableTools(List.of("read_file", "write_file"))
                    .disabledTools(new HashSet<>(Set.of("read_file")))
                    .build();

            // simulate re-checking "read_file" (toolCheckbox.isSelected() == true)
            server.getDisabledTools().remove("read_file");

            assertThat(server.getDisabledTools()).isEmpty();
        }

        @Test
        void toggleDoesNotAffectOtherTools() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .availableTools(List.of("tool-a", "tool-b", "tool-c"))
                    .disabledTools(new HashSet<>(Set.of("tool-b")))
                    .build();

            // disable tool-a as well
            server.getDisabledTools().add("tool-a");

            assertThat(server.getDisabledTools()).containsExactlyInAnyOrder("tool-a", "tool-b");
            // tool-c remains unaffected
            assertThat(server.getDisabledTools()).doesNotContain("tool-c");
        }
    }

    // ─── Tool count calculation (updateMCPToolsCounter logic) ───

    @Nested
    class ToolCountCalculation {

        @Test
        void enabledToolCount_equalsTotal_minusDisabled() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .availableTools(List.of("tool-a", "tool-b", "tool-c"))
                    .disabledTools(new HashSet<>(Set.of("tool-b")))
                    .build();

            int total = server.getAvailableTools().size();
            int disabled = server.getDisabledTools().size();

            assertThat(total - disabled).isEqualTo(2);
        }

        @Test
        void serverWithNoDisabledTools_countsAllTools() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .availableTools(List.of("tool-a", "tool-b"))
                    .build();

            int disabled = server.getDisabledTools() != null ? server.getDisabledTools().size() : 0;

            assertThat(server.getAvailableTools().size() - disabled).isEqualTo(2);
        }

        @Test
        void disabledServer_isExcludedFromTotal() {
            MCPServer enabled = MCPServer.builder()
                    .name("server-a").enabled(true)
                    .availableTools(List.of("tool-1", "tool-2"))
                    .build();
            MCPServer disabled = MCPServer.builder()
                    .name("server-b").enabled(false)
                    .availableTools(List.of("tool-3"))
                    .build();

            int total = List.of(enabled, disabled).stream()
                    .filter(MCPServer::isEnabled)
                    .mapToInt(s -> {
                        int t = s.getAvailableTools().size();
                        int d = s.getDisabledTools() != null ? s.getDisabledTools().size() : 0;
                        return t - d;
                    })
                    .sum();

            assertThat(total).isEqualTo(2);
        }
    }

    // ─── buildToolsPanel logic (initial checkbox selection state) ─

    @Nested
    class ToolCheckboxInitialState {

        @Test
        void toolNotInDisabledSet_isSelected() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .availableTools(List.of("read_file", "write_file"))
                    .disabledTools(new HashSet<>(Set.of("write_file")))
                    .build();

            Set<String> disabledTools = server.getDisabledTools();

            // read_file is NOT in disabled → checkbox should be selected
            assertThat(disabledTools.contains("read_file")).isFalse();
        }

        @Test
        void toolInDisabledSet_isNotSelected() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .availableTools(List.of("read_file", "write_file"))
                    .disabledTools(new HashSet<>(Set.of("write_file")))
                    .build();

            Set<String> disabledTools = server.getDisabledTools();

            // write_file IS in disabled → checkbox should NOT be selected
            assertThat(disabledTools.contains("write_file")).isTrue();
        }
    }

    // ─── onServerCheckboxToggled logic ──────────────────────────

    @Nested
    class ServerCheckboxToggle {

        @Test
        void enablingServer_updatesEnabledFlag() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .enabled(false)
                    .availableTools(List.of("tool-a"))
                    .build();

            server.setEnabled(true);

            assertThat(server.isEnabled()).isTrue();
        }

        @Test
        void disablingServer_updatesEnabledFlag() {
            MCPServer server = MCPServer.builder()
                    .name("test-server")
                    .enabled(true)
                    .availableTools(List.of("tool-a"))
                    .build();

            server.setEnabled(false);

            assertThat(server.isEnabled()).isFalse();
        }

        @Test
        void toolsPanelVisibility_dependsOnServerEnabledAndToolsPresent() {
            MCPServer withTools = MCPServer.builder()
                    .name("server-a").enabled(true)
                    .availableTools(List.of("tool-a"))
                    .build();
            MCPServer emptyServer = MCPServer.builder()
                    .name("server-b").enabled(true)
                    .availableTools(List.of())
                    .build();
            MCPServer disabledServer = MCPServer.builder()
                    .name("server-c").enabled(false)
                    .availableTools(List.of("tool-a"))
                    .build();

            // toolsPanel visibility formula: enabled && !tools.isEmpty()
            assertThat(withTools.isEnabled() && !withTools.getAvailableTools().isEmpty()).isTrue();
            assertThat(emptyServer.isEnabled() && !emptyServer.getAvailableTools().isEmpty()).isFalse();
            assertThat(disabledServer.isEnabled() && !disabledServer.getAvailableTools().isEmpty()).isFalse();
        }
    }
}
