package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilteredMcpToolProviderTest {

    @Mock
    private ToolProvider delegate;

    @Mock
    private ToolProviderRequest request;

    @Mock
    private ToolExecutor executor1;

    @Mock
    private ToolExecutor executor2;

    @Mock
    private ToolExecutor executor3;

    private MockedStatic<MCPService> mockedMCPService;

    @BeforeEach
    void setUp() {
        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(() -> MCPService.logDebug(any(String.class))).thenAnswer(inv -> null);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        mockedMCPService.close();
    }

    // -- provideTools tests --

    @Test
    void provideTools_noDisabledTools_returnsAllToolsFromDelegate() {
        ToolProviderResult delegateResult = buildResult(
                Map.of(toolSpec("read_file"), executor1, toolSpec("write_file"), executor2)
        );
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        FilteredMcpToolProvider provider = new FilteredMcpToolProvider(delegate, Set::of);

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(2);
        assertThat(toolNames(result)).containsExactlyInAnyOrder("read_file", "write_file");
    }

    @Test
    void provideTools_filtersOutDisabledToolsByName() {
        ToolProviderResult delegateResult = buildResult(
                Map.of(toolSpec("read_file"), executor1,
                       toolSpec("write_file"), executor2,
                       toolSpec("delete_file"), executor3)
        );
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        Supplier<Set<String>> disabledSupplier = () -> Set.of("write_file");
        FilteredMcpToolProvider provider = new FilteredMcpToolProvider(delegate, disabledSupplier);

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(2);
        assertThat(toolNames(result)).containsExactlyInAnyOrder("read_file", "delete_file");
    }

    @Test
    void provideTools_allToolsDisabled_returnsEmptyResult() {
        ToolProviderResult delegateResult = buildResult(
                Map.of(toolSpec("read_file"), executor1, toolSpec("write_file"), executor2)
        );
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        Supplier<Set<String>> disabledSupplier = () -> Set.of("read_file", "write_file");
        FilteredMcpToolProvider provider = new FilteredMcpToolProvider(delegate, disabledSupplier);

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).isEmpty();
    }

    @Test
    void provideTools_preservesToolExecutorsForEnabledTools() {
        ToolSpecification spec1 = toolSpec("read_file");
        ToolSpecification spec2 = toolSpec("write_file");
        ToolProviderResult delegateResult = buildResult(Map.of(spec1, executor1, spec2, executor2));
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        Supplier<Set<String>> disabledSupplier = () -> Set.of("write_file");
        FilteredMcpToolProvider provider = new FilteredMcpToolProvider(delegate, disabledSupplier);

        ToolProviderResult result = provider.provideTools(request);

        // Verify the executor for read_file is preserved
        assertThat(result.tools().values()).containsExactly(executor1);
    }

    @Test
    void provideTools_emptyDelegateResult_returnsEmpty() {
        ToolProviderResult delegateResult = buildResult(Map.of());
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        Supplier<Set<String>> disabledSupplier = () -> Set.of("some_tool");
        FilteredMcpToolProvider provider = new FilteredMcpToolProvider(delegate, disabledSupplier);

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).isEmpty();
    }

    @Test
    void provideTools_disabledToolNotInDelegate_returnsAllTools() {
        ToolProviderResult delegateResult = buildResult(
                Map.of(toolSpec("read_file"), executor1)
        );
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        // Disabled tool doesn't match any delegate tool
        Supplier<Set<String>> disabledSupplier = () -> Set.of("nonexistent_tool");
        FilteredMcpToolProvider provider = new FilteredMcpToolProvider(delegate, disabledSupplier);

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).hasSize(1);
        assertThat(toolNames(result)).containsExactly("read_file");
    }

    // -- collectDisabledTools static method tests --

    @Test
    void collectDisabledTools_emptyServersMap_returnsEmptySet() {
        Set<String> result = FilteredMcpToolProvider.collectDisabledTools(Map.of());
        assertThat(result).isEmpty();
    }

    @Test
    void collectDisabledTools_enabledServerWithDisabledTools_returnsThoseTools() {
        MCPServer server = MCPServer.builder()
                .name("server1")
                .enabled(true)
                .disabledTools(Set.of("tool_a", "tool_b"))
                .build();

        Set<String> result = FilteredMcpToolProvider.collectDisabledTools(Map.of("server1", server));

        assertThat(result).containsExactlyInAnyOrder("tool_a", "tool_b");
    }

    @Test
    void collectDisabledTools_disabledServerIsSkipped() {
        MCPServer server = MCPServer.builder()
                .name("server1")
                .enabled(false)
                .disabledTools(Set.of("tool_a"))
                .build();

        Set<String> result = FilteredMcpToolProvider.collectDisabledTools(Map.of("server1", server));

        assertThat(result).isEmpty();
    }

    @Test
    void collectDisabledTools_nullDisabledToolsIsSkipped() {
        MCPServer server = MCPServer.builder()
                .name("server1")
                .enabled(true)
                .build();
        server.setDisabledTools(null);

        Set<String> result = FilteredMcpToolProvider.collectDisabledTools(Map.of("server1", server));

        assertThat(result).isEmpty();
    }

    @Test
    void collectDisabledTools_multipleServers_mergesDisabledTools() {
        MCPServer server1 = MCPServer.builder()
                .name("server1")
                .enabled(true)
                .disabledTools(Set.of("tool_a", "tool_b"))
                .build();

        MCPServer server2 = MCPServer.builder()
                .name("server2")
                .enabled(true)
                .disabledTools(Set.of("tool_c"))
                .build();

        MCPServer disabledServer = MCPServer.builder()
                .name("server3")
                .enabled(false)
                .disabledTools(Set.of("tool_d"))
                .build();

        Map<String, MCPServer> servers = Map.of(
                "server1", server1,
                "server2", server2,
                "server3", disabledServer
        );

        Set<String> result = FilteredMcpToolProvider.collectDisabledTools(servers);

        assertThat(result).containsExactlyInAnyOrder("tool_a", "tool_b", "tool_c");
        assertThat(result).doesNotContain("tool_d");
    }

    @Test
    void collectDisabledTools_enabledServerWithEmptyDisabledTools_returnsEmptySet() {
        MCPServer server = MCPServer.builder()
                .name("server1")
                .enabled(true)
                .disabledTools(Set.of())
                .build();

        Set<String> result = FilteredMcpToolProvider.collectDisabledTools(Map.of("server1", server));

        assertThat(result).isEmpty();
    }

    @Test
    void collectDisabledTools_duplicateToolsAcrossServers_deduplicates() {
        MCPServer server1 = MCPServer.builder()
                .name("server1")
                .enabled(true)
                .disabledTools(Set.of("tool_a", "tool_b"))
                .build();

        MCPServer server2 = MCPServer.builder()
                .name("server2")
                .enabled(true)
                .disabledTools(Set.of("tool_b", "tool_c"))
                .build();

        Map<String, MCPServer> servers = Map.of("server1", server1, "server2", server2);

        Set<String> result = FilteredMcpToolProvider.collectDisabledTools(servers);

        assertThat(result).containsExactlyInAnyOrder("tool_a", "tool_b", "tool_c");
    }

    // -- Helpers --

    private static ToolSpecification toolSpec(String name) {
        return ToolSpecification.builder().name(name).build();
    }

    private static Set<String> toolNames(ToolProviderResult result) {
        Set<String> names = new HashSet<>();
        for (ToolSpecification spec : result.tools().keySet()) {
            names.add(spec.name());
        }
        return names;
    }

    private static ToolProviderResult buildResult(Map<ToolSpecification, ToolExecutor> tools) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
