package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.registry.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MCPRegistryServiceTest {

    private final MCPRegistryService service = new MCPRegistryService();

    @Test
    void testConvertRemoteServer_SetsStreamableHttpTransportAndUrl() {
        MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                "test-server",
                "https://mcp.example.com/v1",
                List.of()
        );

        MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

        assertEquals("test-server", result.getName());
        assertEquals(MCPServer.TransportType.HTTP, result.getTransportType());
        assertEquals("https://mcp.example.com/v1", result.getUrl());
    }

    @Test
    void testConvertRemoteServer_SetsHeadersFromUserValues() {
        MCPRegistryHeader authHeader = createHeader("Authorization", "Bearer {{token}}", true, true);
        MCPRegistryHeader customHeader = createHeader("X-Custom", null, false, false);

        MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                "auth-server",
                "https://mcp.example.com/v1",
                List.of(authHeader, customHeader)
        );

        Map<String, String> userValues = Map.of(
                "Authorization", "Bearer my-secret-key",
                "X-Custom", "custom-value"
        );

        MCPServer result = service.convertToMCPServer(serverInfo, userValues);

        assertNotNull(result.getHeaders());
        assertEquals(2, result.getHeaders().size());
        assertEquals("Bearer my-secret-key", result.getHeaders().get("Authorization"));
        assertEquals("custom-value", result.getHeaders().get("X-Custom"));
    }

    @Test
    void testConvertRemoteServer_SkipsBlankHeaderValues() {
        MCPRegistryHeader authHeader = createHeader("Authorization", null, true, true);
        MCPRegistryHeader optionalHeader = createHeader("X-Optional", null, false, false);

        MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                "partial-server",
                "https://mcp.example.com/v1",
                List.of(authHeader, optionalHeader)
        );

        // Only provide the auth header, leave optional blank
        Map<String, String> userValues = Map.of(
                "Authorization", "Bearer my-key",
                "X-Optional", ""
        );

        MCPServer result = service.convertToMCPServer(serverInfo, userValues);

        assertNotNull(result.getHeaders());
        assertEquals(1, result.getHeaders().size());
        assertEquals("Bearer my-key", result.getHeaders().get("Authorization"));
        assertNull(result.getHeaders().get("X-Optional"));
    }

    @Test
    void testConvertRemoteServer_FallsBackToDefaultHeaderValue() {
        MCPRegistryHeader header = createHeader("X-Api-Version", "2024-01-01", false, false);

        MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                "versioned-server",
                "https://mcp.example.com/v1",
                List.of(header)
        );

        // User provides no override for this header
        Map<String, String> userValues = Map.of();

        MCPServer result = service.convertToMCPServer(serverInfo, userValues);

        assertNotNull(result.getHeaders());
        assertEquals(1, result.getHeaders().size());
        assertEquals("2024-01-01", result.getHeaders().get("X-Api-Version"));
    }

    @Test
    void testConvertRemoteServer_HeadersPreservedInMCPServer() {
        MCPRegistryHeader authHeader = createHeader("Authorization", null, true, true);

        MCPRegistryServerInfo serverInfo = createRemoteServerInfo(
                "remote-mcp",
                "https://api.example.com/mcp",
                List.of(authHeader)
        );

        Map<String, String> userValues = Map.of("Authorization", "Bearer test-token");

        MCPServer result = service.convertToMCPServer(serverInfo, userValues);

        // Verify the full MCPServer is correctly configured for remote HTTP
        assertEquals("remote-mcp", result.getName());
        assertEquals(MCPServer.TransportType.HTTP, result.getTransportType());
        assertEquals("https://api.example.com/mcp", result.getUrl());
        assertEquals("Bearer test-token", result.getHeaders().get("Authorization"));
    }

    @Test
    void testConvertPackageServer_SetsStdioTransport() {
        MCPRegistryPackage pkg = new MCPRegistryPackage();
        pkg.setRegistryType("npm");
        pkg.setIdentifier("@modelcontextprotocol/server-filesystem");

        MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
        serverInfo.setName("filesystem-server");
        serverInfo.setPackages(List.of(pkg));

        MCPServer result = service.convertToMCPServer(serverInfo, Map.of());

        assertEquals("filesystem-server", result.getName());
        assertEquals(MCPServer.TransportType.STDIO, result.getTransportType());
        assertEquals("npx", result.getCommand());
        assertEquals(List.of("-y", "@modelcontextprotocol/server-filesystem"), result.getArgs());
    }

    @Test
    void testConvertPackageServer_SetsEnvVars() {
        MCPRegistryEnvVar envVar = new MCPRegistryEnvVar();
        envVar.setName("API_KEY");
        envVar.setDescription("API key for the service");
        envVar.setIsRequired(true);

        MCPRegistryPackage pkg = new MCPRegistryPackage();
        pkg.setRegistryType("npm");
        pkg.setIdentifier("@example/mcp-server");
        pkg.setEnvironmentVariables(List.of(envVar));

        MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
        serverInfo.setName("env-server");
        serverInfo.setPackages(List.of(pkg));

        Map<String, String> userValues = Map.of("API_KEY", "secret-123");

        MCPServer result = service.convertToMCPServer(serverInfo, userValues);

        assertEquals("secret-123", result.getEnv().get("API_KEY"));
    }

    @Test
    void testGetServerType_Remote() {
        MCPRegistryServerInfo serverInfo = createRemoteServerInfo("srv", "https://x.com", List.of());
        assertEquals("Remote", service.getServerType(serverInfo));
    }

    @Test
    void testGetServerType_Npm() {
        MCPRegistryPackage pkg = new MCPRegistryPackage();
        pkg.setRegistryType("npm");

        MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
        serverInfo.setPackages(List.of(pkg));

        assertEquals("npm", service.getServerType(serverInfo));
    }

    @Test
    void testGetServerType_Docker() {
        MCPRegistryPackage pkg = new MCPRegistryPackage();
        pkg.setRegistryType("oci");

        MCPRegistryServerInfo serverInfo = new MCPRegistryServerInfo();
        serverInfo.setPackages(List.of(pkg));

        assertEquals("Docker", service.getServerType(serverInfo));
    }

    // -- helper methods --

    private static MCPRegistryServerInfo createRemoteServerInfo(String name, String url,
                                                                 List<MCPRegistryHeader> headers) {
        MCPRegistryRemote remote = new MCPRegistryRemote();
        remote.setUrl(url);
        remote.setHeaders(headers);

        MCPRegistryServerInfo info = new MCPRegistryServerInfo();
        info.setName(name);
        info.setRemotes(List.of(remote));
        return info;
    }

    private static MCPRegistryHeader createHeader(String name, String defaultValue,
                                                   boolean required, boolean secret) {
        MCPRegistryHeader header = new MCPRegistryHeader();
        header.setName(name);
        header.setValue(defaultValue);
        header.setIsRequired(required);
        header.setIsSecret(secret);
        return header;
    }
}
