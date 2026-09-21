package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.MCPSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpOperationHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.protocol.McpClientMessage;
import dev.langchain4j.mcp.protocol.McpClientMethod;
import dev.langchain4j.mcp.protocol.McpInitializeParams;
import dev.langchain4j.mcp.protocol.McpInitializeRequest;
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
import java.util.concurrent.CompletableFuture;

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
        void routesLegacyHttpSseToStreamableHttp() {
            MCPServer server = httpSseServer("sse-server", null);

            // HTTP_SSE is an alias for HTTP; with null URL, initHttpClient returns null (URL validation)
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

    // ─── initHttpClient (legacy HTTP_SSE alias) ────────────────

    @Nested
    class InitHttpClientForLegacyHttpSse {

        @Test
        void returnsNullForNullUrl() {
            MCPServer server = httpSseServer("sse", null);

            McpClient result = MCPExecutionService.initHttpClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForEmptyUrl() {
            MCPServer server = httpSseServer("sse", "");

            McpClient result = MCPExecutionService.initHttpClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForBlankUrl() {
            MCPServer server = httpSseServer("sse", "   ");

            McpClient result = MCPExecutionService.initHttpClient(server);

            assertThat(result).isNull();
        }
    }

    // ─── initHttpClient ────────────────────────────────────────

    @Nested
    class InitHttpClient {

        @Test
        void returnsNullForNullUrl() {
            MCPServer server = httpServer("http", null);

            McpClient result = MCPExecutionService.initHttpClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForEmptyUrl() {
            MCPServer server = httpServer("http", "");

            McpClient result = MCPExecutionService.initHttpClient(server);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForBlankUrl() {
            MCPServer server = httpServer("http", "   ");

            McpClient result = MCPExecutionService.initHttpClient(server);

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

    // ─── Transport selection ───────────────────────────────────────────────

    /**
     * langchain4j-mcp 1.19+ removed the legacy 2024-11-05 HTTP+SSE transport ({@code HttpMcpTransport})
     * with no replacement. {@code HTTP_SSE} is therefore kept only as an alias for persisted
     * configurations and is served by the streamable HTTP transport. SSE-only servers (issue #1151)
     * are no longer supported.
     */
    @Nested
    class TransportSelection {

        @BeforeEach
        void stubTimeout() {
            when(stateService.getTimeout()).thenReturn(60);
        }

        @Test
        void legacyHttpSseServerIsAliasedToStreamableTransport() throws Exception {
            try (McpTransport built = MCPExecutionService.buildHttpTransport(
                    httpSseServer("jetbrains", "http://127.0.0.1:64342/sse"))) {

                assertThat(built)
                        .as("HTTP_SSE is a legacy alias: langchain4j 1.19+ has no SSE transport, "
                            + "so it must be served by the streamable HTTP transport")
                        .isInstanceOf(StreamableHttpMcpTransport.class);
            }
        }

        @Test
        void httpServerUsesStreamableTransport() throws Exception {
            try (McpTransport built = MCPExecutionService.buildHttpTransport(
                    httpServer("remote", "https://example.com/mcp"))) {

                assertThat(built)
                        .as("HTTP servers use the streamable HTTP transport")
                        .isInstanceOf(StreamableHttpMcpTransport.class);
            }
        }

        @Test
        void httpTransportCarriesCustomHeaders() throws Exception {
            MCPServer server = MCPServer.builder()
                    .name("remote")
                    .enabled(true)
                    .transportType(MCPServer.TransportType.HTTP)
                    .url("https://example.com/mcp")
                    .headers(Map.of("Authorization", "Bearer token"))
                    .build();

            try (McpTransport built = MCPExecutionService.buildHttpTransport(server)) {
                assertThat(built).isInstanceOf(StreamableHttpMcpTransport.class);
            }
        }
    }

    // ─── Protocol negotiation ──────────────────────────────────────────────

    /**
     * The client must not pin the protocol version: langchain4j 1.20 auto-detects the MCP
     * protocol by probing {@code server/discover} (2026-07-28) and falling back to the legacy
     * {@code initialize} handshake (2025-11-25). Pinning {@code 2024-11-05} skips both.
     */
    @Nested
    class ProtocolNegotiation {

        @BeforeEach
        void stubTimeout() {
            when(stateService.getTimeout()).thenReturn(60);
        }

        @Test
        void clientProbesModernProtocolThenFallsBackToLegacyInitialize() {
            RecordingTransport transport = new RecordingTransport();

            assertThatThrownBy(() -> MCPExecutionService.newClientBuilder(transport).build())
                    .isInstanceOf(RuntimeException.class);

            assertThat(transport.sent)
                    .as("first request must be the 2026-07-28 server/discover probe")
                    .isNotEmpty();
            assertThat(transport.sent.get(0).method).isEqualTo(McpClientMethod.SERVER_DISCOVER);

            assertThat(transport.sent)
                    .as("after the probe fails the client falls back to a legacy initialize")
                    .hasSize(2);
            McpClientMessage fallback = transport.sent.get(1);
            assertThat(fallback.method).isEqualTo(McpClientMethod.INITIALIZE);
            McpInitializeParams params = (McpInitializeParams) ((McpInitializeRequest) fallback).getParams();
            assertThat(params.getProtocolVersion()).isEqualTo("2025-11-25");
        }

        @Test
        void protocolDetectionTimeoutFollowsUserTimeout() {
            when(stateService.getTimeout()).thenReturn(7);
            RecordingTransport transport = new RecordingTransport();

            assertThatThrownBy(() -> MCPExecutionService.newClientBuilder(transport).build())
                    .isInstanceOf(RuntimeException.class);

            // A short user timeout still yields both probe and fallback — i.e. detection ran and
            // did not hang on the (never answered) probe for the default duration.
            assertThat(transport.sent).hasSize(2);
        }
    }

    /**
     * Transport that records every outgoing request and answers each with a failed future,
     * so the client walks the whole auto-detection path without a network.
     */
    private static final class RecordingTransport implements McpTransport {
        final List<McpClientMessage> sent = new ArrayList<>();

        @Override
        public void start(McpOperationHandler handler) {
        }

        @Override
        public CompletableFuture<String> sendInitializeRequest(McpInitializeRequest request) {
            sent.add(request);
            return CompletableFuture.failedFuture(new RuntimeException("recording transport"));
        }

        @Override
        public CompletableFuture<String> sendRequest(McpCallContext context) {
            sent.add(context.message());
            return CompletableFuture.failedFuture(new RuntimeException("recording transport"));
        }

        @Override
        public CompletableFuture<String> sendRequest(McpClientMessage message) {
            sent.add(message);
            return CompletableFuture.failedFuture(new RuntimeException("recording transport"));
        }

        @Override
        public void sendMessage(McpCallContext context) {
            sent.add(context.message());
        }

        @Override
        public void sendMessage(McpClientMessage message) {
            sent.add(message);
        }

        @Override
        public void checkHealth() {
        }

        @Override
        public void onFailure(Runnable runnable) {
        }

        @Override
        public void close() {
        }
    }
}
