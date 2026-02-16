package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.MCPSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MCPExecutionServiceTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private MCPSettings mcpSettings;

    @Mock
    private McpClient mockClient1;

    @Mock
    private McpClient mockClient2;

    @Mock
    private Project project;

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;

    private MCPExecutionService.McpClientCreator mockCreator;
    private MCPExecutionService service;

    @BeforeEach
    void setUp() {
        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(() -> MCPService.logDebug(any(String.class))).thenAnswer(inv -> null);
        mockedMCPService.when(MCPService::isDebugLogsEnabled).thenReturn(false);

        when(stateService.getMcpSettings()).thenReturn(mcpSettings);
        when(mcpSettings.getMcpServers()).thenReturn(new HashMap<>());

        mockCreator = mock(MCPExecutionService.McpClientCreator.class);
        service = new MCPExecutionService(mockCreator);
    }

    @AfterEach
    void tearDown() {
        mockedStateService.close();
        mockedMCPService.close();
    }

    // ─── Helper methods ────────────────────────────────────────

    private static MCPServer stdioServer(String name) {
        return MCPServer.builder()
                .name(name)
                .enabled(true)
                .transportType(MCPServer.TransportType.STDIO)
                .command("/usr/local/bin/npx")
                .args(List.of("-y", "some-package"))
                .build();
    }

    private static MCPServer httpSseServer(String name, String url) {
        return MCPServer.builder()
                .name(name)
                .enabled(true)
                .transportType(MCPServer.TransportType.HTTP_SSE)
                .url(url)
                .build();
    }

    private static MCPServer httpServer(String name, String url) {
        return MCPServer.builder()
                .name(name)
                .enabled(true)
                .transportType(MCPServer.TransportType.HTTP)
                .url(url)
                .build();
    }

    private static MCPServer disabledServer(String name) {
        return MCPServer.builder()
                .name(name)
                .enabled(false)
                .transportType(MCPServer.TransportType.STDIO)
                .command("/usr/local/bin/npx")
                .build();
    }

    // ─── clearClientCache ──────────────────────────────────────

    @Nested
    class ClearClientCache {

        @Test
        void closesAllCachedClients() throws Exception {
            MCPServer server1 = stdioServer("server1");
            MCPServer server2 = stdioServer("server2");
            when(mockCreator.create(server1)).thenReturn(mockClient1);
            when(mockCreator.create(server2)).thenReturn(mockClient2);

            // Populate cache
            service.createMcpClient(server1);
            service.createMcpClient(server2);
            assertThat(service.getCacheSize()).isEqualTo(2);

            service.clearClientCache();

            verify(mockClient1).close();
            verify(mockClient2).close();
            assertThat(service.getCacheSize()).isZero();
        }

        @Test
        void handlesExceptionDuringClose() throws Exception {
            MCPServer server = stdioServer("failing-server");
            when(mockCreator.create(server)).thenReturn(mockClient1);
            doThrow(new RuntimeException("close failed")).when(mockClient1).close();

            service.createMcpClient(server);

            // Should not throw
            service.clearClientCache();
            assertThat(service.getCacheSize()).isZero();
        }

        @Test
        void emptyCacheDoesNothing() {
            assertThat(service.getCacheSize()).isZero();
            service.clearClientCache();
            assertThat(service.getCacheSize()).isZero();
        }
    }

    // ─── dispose ───────────────────────────────────────────────

    @Nested
    class Dispose {

        @Test
        void clearsCacheAndClosesClients() throws Exception {
            MCPServer server = stdioServer("server1");
            when(mockCreator.create(server)).thenReturn(mockClient1);

            service.createMcpClient(server);
            assertThat(service.getCacheSize()).isEqualTo(1);

            service.dispose();

            verify(mockClient1).close();
            assertThat(service.getCacheSize()).isZero();
        }
    }

    // ─── createMcpClient (caching) ────────────────────────────

    @Nested
    class CreateMcpClient {

        @Test
        void cachesNewClient() {
            MCPServer server = stdioServer("test-server");
            when(mockCreator.create(server)).thenReturn(mockClient1);

            McpClient result = service.createMcpClient(server);

            assertThat(result).isSameAs(mockClient1);
            assertThat(service.getCacheSize()).isEqualTo(1);
        }

        @Test
        void returnsCachedClientOnSecondCall() {
            MCPServer server = stdioServer("cached-server");
            when(mockCreator.create(server)).thenReturn(mockClient1);

            McpClient first = service.createMcpClient(server);
            McpClient second = service.createMcpClient(server);

            assertThat(first).isSameAs(second);
            // Creator should only be called once
            verify(mockCreator, times(1)).create(server);
        }

        @Test
        void doesNotCacheNull() {
            MCPServer server = stdioServer("null-server");
            when(mockCreator.create(server)).thenReturn(null);

            McpClient result = service.createMcpClient(server);

            assertThat(result).isNull();
            assertThat(service.getCacheSize()).isZero();
        }

        @Test
        void returnsNullWhenCreatorThrows() {
            MCPServer server = stdioServer("error-server");
            when(mockCreator.create(server)).thenThrow(new RuntimeException("creation failed"));

            McpClient result = service.createMcpClient(server);

            assertThat(result).isNull();
            assertThat(service.getCacheSize()).isZero();
        }
    }

    // ─── createMCPToolProvider ─────────────────────────────────

    @Nested
    class CreateMCPToolProvider {

        @Test
        void returnsNullWhenNoServers() {
            when(mcpSettings.getMcpServers()).thenReturn(Map.of());

            ToolProvider result = service.createMCPToolProvider(project);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullWhenNoEnabledServers() {
            MCPServer disabled = disabledServer("disabled-server");
            when(mcpSettings.getMcpServers()).thenReturn(Map.of("disabled-server", disabled));

            ToolProvider result = service.createMCPToolProvider(project);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullWhenCreatorReturnsNull() {
            MCPServer server = stdioServer("server");
            when(mcpSettings.getMcpServers()).thenReturn(Map.of("server", server));
            when(mockCreator.create(server)).thenReturn(null);

            ToolProvider result = service.createMCPToolProvider(project);

            assertThat(result).isNull();
        }

        @Test
        void wrapsWithApprovalRequiredProvider() {
            MCPServer server = stdioServer("server");
            when(mcpSettings.getMcpServers()).thenReturn(Map.of("server", server));
            when(mockCreator.create(server)).thenReturn(mockClient1);

            ToolProvider result = service.createMCPToolProvider(project);

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(ApprovalRequiredToolProvider.class);
        }
    }

    // ─── createRawMCPToolProvider ──────────────────────────────

    @Nested
    class CreateRawMCPToolProvider {

        @Test
        void returnsNullWhenNoServersConfigured() {
            when(mcpSettings.getMcpServers()).thenReturn(Map.of());

            ToolProvider result = service.createRawMCPToolProvider();

            assertThat(result).isNull();
        }

        @Test
        void returnsNullWhenNoEnabledServers() {
            MCPServer disabled = disabledServer("disabled");
            when(mcpSettings.getMcpServers()).thenReturn(Map.of("disabled", disabled));

            ToolProvider result = service.createRawMCPToolProvider();

            assertThat(result).isNull();
            verifyNoInteractions(mockCreator);
        }

        @Test
        void returnsNullWhenAllClientsFailToCreate() {
            MCPServer server = stdioServer("failing");
            when(mcpSettings.getMcpServers()).thenReturn(Map.of("failing", server));
            when(mockCreator.create(server)).thenReturn(null);

            ToolProvider result = service.createRawMCPToolProvider();

            assertThat(result).isNull();
        }

        @Test
        void returnsFilteredProviderForEnabledServers() {
            MCPServer server = stdioServer("my-server");
            when(mcpSettings.getMcpServers()).thenReturn(Map.of("my-server", server));
            when(mockCreator.create(server)).thenReturn(mockClient1);

            ToolProvider result = service.createRawMCPToolProvider();

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(FilteredMcpToolProvider.class);
        }

        @Test
        void processesMultipleEnabledServers() {
            MCPServer server1 = stdioServer("s1");
            MCPServer server2 = stdioServer("s2");
            MCPServer disabled = disabledServer("s3");
            Map<String, MCPServer> servers = new LinkedHashMap<>();
            servers.put("s1", server1);
            servers.put("s2", server2);
            servers.put("s3", disabled);
            when(mcpSettings.getMcpServers()).thenReturn(servers);
            when(mockCreator.create(server1)).thenReturn(mockClient1);
            when(mockCreator.create(server2)).thenReturn(mockClient2);

            ToolProvider result = service.createRawMCPToolProvider();

            assertThat(result).isNotNull();
            verify(mockCreator).create(server1);
            verify(mockCreator).create(server2);
            verify(mockCreator, never()).create(disabled);
        }
    }

    // ─── createNewClient (transport routing) ───────────────────

    @Nested
    class CreateNewClient {

        @Test
        void routesToHttpSseForHttpSseTransport() {
            MCPServer server = httpSseServer("sse-server", null);

            // With null URL, initHttpSseClient returns null (URL validation)
            McpClient result = MCPExecutionService.createNewClient(server);

            assertThat(result).isNull();
        }

        @Test
        void routesToStreamableHttpForHttpTransport() {
            MCPServer server = httpServer("http-server", null);

            // With null URL, initStreamableHttpClient returns null (URL validation)
            McpClient result = MCPExecutionService.createNewClient(server);

            assertThat(result).isNull();
        }

        @Test
        void routesToStdioForStdioTransport() {
            MCPServer server = stdioServer("stdio-server");

            // initStdioClient will fail (no real binary) but it proves routing
            McpClient result = MCPExecutionService.createNewClient(server);

            // Returns null because the actual stdio process can't start in test env
            assertThat(result).isNull();
        }
    }

    // ─── initHttpSseClient ─────────────────────────────────────

    @Nested
    class InitHttpSseClient {

        @Test
        void returnsNullForNullUrl() {
            MCPServer server = httpSseServer("sse", null);

            McpClient result = MCPExecutionService.initHttpSseClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForEmptyUrl() {
            MCPServer server = httpSseServer("sse", "");

            McpClient result = MCPExecutionService.initHttpSseClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForBlankUrl() {
            MCPServer server = httpSseServer("sse", "   ");

            McpClient result = MCPExecutionService.initHttpSseClient(server);

            assertThat(result).isNull();
        }
    }

    // ─── initStreamableHttpClient ──────────────────────────────

    @Nested
    class InitStreamableHttpClient {

        @Test
        void returnsNullForNullUrl() {
            MCPServer server = httpServer("http", null);

            McpClient result = MCPExecutionService.initStreamableHttpClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForEmptyUrl() {
            MCPServer server = httpServer("http", "");

            McpClient result = MCPExecutionService.initStreamableHttpClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForBlankUrl() {
            MCPServer server = httpServer("http", "   ");

            McpClient result = MCPExecutionService.initStreamableHttpClient(server);

            assertThat(result).isNull();
        }
    }

    // ─── createMCPCommand ──────────────────────────────────────

    @Nested
    class CreateMCPCommand {

        @Test
        void withValidCommand() {
            List<String> command = Arrays.asList("/usr/local/bin/npx", "-y",
                    "@modelcontextprotocol/server-filesystem", "/path/to/project");

            List<String> result = MCPExecutionService.createMCPCommand(command);

            assertThat(result).isNotNull().hasSize(3);

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                assertThat(result.get(0)).isEqualTo("cmd.exe");
                assertThat(result.get(1)).isEqualTo("/c");
            } else {
                assertThat(result.get(0)).isEqualTo("/bin/bash");
                assertThat(result.get(1)).isEqualTo("-c");
            }

            String commandStr = result.get(2);
            assertThat(commandStr).contains("/usr/local/bin/npx")
                    .contains("-y")
                    .contains("@modelcontextprotocol/server-filesystem")
                    .contains("/path/to/project");
        }

        @Test
        void withNullCommandThrowsException() {
            assertThatThrownBy(() -> MCPExecutionService.createMCPCommand(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void withEmptyCommandThrowsException() {
            List<String> emptyCommand = new ArrayList<>();

            assertThatThrownBy(() -> MCPExecutionService.createMCPCommand(emptyCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        void withNullArgumentsFiltersOut() {
            List<String> commandWithNulls = Arrays.asList(
                    "/usr/local/bin/npx", null, "-y", null,
                    "@modelcontextprotocol/server-filesystem");

            List<String> result = MCPExecutionService.createMCPCommand(commandWithNulls);

            assertThat(result).isNotNull();
            String commandStr = result.get(2);

            assertThat(commandStr)
                    .contains("/usr/local/bin/npx")
                    .contains("-y")
                    .contains("@modelcontextprotocol/server-filesystem")
                    .doesNotContain("null");
        }

        @Test
        void withArgumentsContainingSpaces() {
            List<String> command = Arrays.asList("/usr/local/bin/npx", "-y",
                    "package with spaces", "/path/with spaces/project");

            List<String> result = MCPExecutionService.createMCPCommand(command);

            assertThat(result).isNotNull();
            String commandStr = result.get(2);

            assertThat(commandStr)
                    .contains("\"package with spaces\"")
                    .contains("\"/path/with spaces/project\"");
        }

        @Test
        void withSingleCommand() {
            List<String> command = List.of("/usr/bin/node");

            List<String> result = MCPExecutionService.createMCPCommand(command);

            assertThat(result).hasSize(3);
            assertThat(result.get(2)).isEqualTo("/usr/bin/node");
        }
    }

    // ─── createTrafficConsumer ─────────────────────────────────

    @Nested
    class CreateTrafficConsumer {

        @Test
        void returnsNonNullConsumer() {
            var consumer = MCPExecutionService.createTrafficConsumer();
            assertThat(consumer).isNotNull();
        }

        @Test
        void consumerDoesNothingWhenDebugDisabled() {
            mockedMCPService.when(MCPService::isDebugLogsEnabled).thenReturn(false);

            var consumer = MCPExecutionService.createTrafficConsumer();

            // Should not throw
            consumer.accept("> some request");
            consumer.accept("< some response");
        }
    }
}
