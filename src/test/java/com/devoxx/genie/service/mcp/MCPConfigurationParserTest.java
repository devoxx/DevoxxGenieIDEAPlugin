package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class MCPConfigurationParserTest {
    private MCPConfigurationParser parser;

    @BeforeEach
    void setUp() {
        parser = new MCPConfigurationParser();
    }

    @Test
    void testParseStandardMCPJson() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {
                    "filesystem": {
                      "command": "npx",
                      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/files"],
                      "env": {
                        "FILESYSTEM_ROOT": "/home/user/projects"
                      }
                    }
                  }
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);

        assertThat(servers).hasSize(1);
        assertThat(servers).containsKey("filesystem");

        MCPServer server = servers.get("filesystem");
        assertThat(server.getName()).isEqualTo("filesystem");
        assertThat(server.getCommand()).isEqualTo("npx");
        assertThat(server.getArgs()).containsExactly("-y", "@modelcontextprotocol/server-filesystem", "/path/to/files");
        assertThat(server.getEnv()).containsEntry("FILESYSTEM_ROOT", "/home/user/projects");
        assertThat(server.getTransportType()).isEqualTo(MCPServer.TransportType.STDIO);
        assertThat(server.isEnabled()).isTrue(); // Default value
    }

    @Test
    void testParseMultipleServers() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {
                    "filesystem": {
                      "command": "npx",
                      "args": ["-y", "@modelcontextprotocol/server-filesystem"]
                    },
                    "github": {
                      "command": "npx",
                      "args": ["-y", "@modelcontextprotocol/server-github"],
                      "env": {
                        "GITHUB_PERSONAL_ACCESS_TOKEN": "token123"
                      }
                    }
                  }
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);

        assertThat(servers).hasSize(2);
        assertThat(servers).containsKeys("filesystem", "github");

        MCPServer githubServer = servers.get("github");
        assertThat(githubServer.getName()).isEqualTo("github");
        assertThat(githubServer.getEnv()).containsEntry("GITHUB_PERSONAL_ACCESS_TOKEN", "token123");
    }

    @Test
    void testParseWithDevoxxGenieExtensions() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {
                    "custom-server": {
                      "command": "python",
                      "args": ["server.py"],
                      "env": {
                        "API_KEY": "secret"
                      },
                      "enabled": false,
                      "transport": "stdio"
                    }
                  }
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);

        assertThat(servers).hasSize(1);
        MCPServer server = servers.get("custom-server");
        assertThat(server.isEnabled()).isFalse();
        assertThat(server.getTransportType()).isEqualTo(MCPServer.TransportType.STDIO);
    }

    @Test
    void testParseHttpTransport() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {
                    "http-server": {
                      "transport": "http",
                      "url": "http://localhost:8080/mcp",
                      "headers": {
                        "Authorization": "Bearer token123"
                      }
                    }
                  }
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);

        assertThat(servers).hasSize(1);
        MCPServer server = servers.get("http-server");
        assertThat(server.getTransportType()).isEqualTo(MCPServer.TransportType.HTTP);
        assertThat(server.getUrl()).isEqualTo("http://localhost:8080/mcp");
        assertThat(server.getHeaders()).containsEntry("Authorization", "Bearer token123");
    }

    @Test
    void testParseHttpSseTransport() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {
                    "sse-server": {
                      "transport": "http-sse",
                      "url": "http://localhost:9000/events"
                    }
                  }
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);

        assertThat(servers).hasSize(1);
        MCPServer server = servers.get("sse-server");
        assertThat(server.getTransportType()).isEqualTo(MCPServer.TransportType.HTTP_SSE);
        assertThat(server.getUrl()).isEqualTo("http://localhost:9000/events");
    }

    @Test
    void testParseMinimalConfiguration() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {
                    "simple": {
                      "command": "node",
                      "args": ["server.js"]
                    }
                  }
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);

        assertThat(servers).hasSize(1);
        MCPServer server = servers.get("simple");
        assertThat(server.getName()).isEqualTo("simple");
        assertThat(server.getCommand()).isEqualTo("node");
        assertThat(server.getArgs()).containsExactly("server.js");
        assertThat(server.getEnv()).isEmpty();
        assertThat(server.isEnabled()).isTrue();
    }

    @Test
    void testParseMissingMcpServersRoot() {
        String json = """
                {
                  "servers": {
                    "filesystem": {
                      "command": "npx"
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parseFromJson(json))
                .isInstanceOf(MCPConfigurationParser.MCPConfigurationException.class)
                .hasMessageContaining("Missing 'mcpServers' root object");
    }

    @Test
    void testParseMissingCommand() {
        String json = """
                {
                  "mcpServers": {
                    "filesystem": {
                      "args": ["-y", "@modelcontextprotocol/server-filesystem"]
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parseFromJson(json))
                .isInstanceOf(MCPConfigurationParser.MCPConfigurationException.class)
                .hasMessageContaining("requires 'command' field");
    }

    @Test
    void testParseHttpTransportMissingUrl() {
        String json = """
                {
                  "mcpServers": {
                    "http-server": {
                      "transport": "http",
                      "headers": {}
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parseFromJson(json))
                .isInstanceOf(MCPConfigurationParser.MCPConfigurationException.class)
                .hasMessageContaining("requires 'url' field");
    }

    @Test
    void testParseInvalidJson() {
        String json = "{ invalid json }";

        assertThatThrownBy(() -> parser.parseFromJson(json))
                .isInstanceOf(MCPConfigurationParser.MCPConfigurationException.class)
                .hasMessageContaining("Invalid JSON syntax");
    }

    @Test
    void testExportToJsonStandardFormat() throws MCPConfigurationParser.MCPConfigurationException {
        MCPServer server1 = MCPServer.builder()
                .name("filesystem")
                .transportType(MCPServer.TransportType.STDIO)
                .command("npx")
                .args(List.of("-y", "@modelcontextprotocol/server-filesystem"))
                .env(Map.of("FILESYSTEM_ROOT", "/home/user"))
                .enabled(true)
                .build();

        MCPServer server2 = MCPServer.builder()
                .name("github")
                .transportType(MCPServer.TransportType.STDIO)
                .command("npx")
                .args(List.of("-y", "@modelcontextprotocol/server-github"))
                .env(Map.of("GITHUB_TOKEN", "token123"))
                .enabled(true)
                .build();

        Map<String, MCPServer> servers = Map.of(
                "filesystem", server1,
                "github", server2
        );

        String json = parser.exportToJson(servers, false);

        // Verify the exported JSON can be parsed back
        Map<String, MCPServer> parsedServers = parser.parseFromJson(json);
        assertThat(parsedServers).hasSize(2);
        assertThat(parsedServers).containsKeys("filesystem", "github");

        MCPServer parsedFs = parsedServers.get("filesystem");
        assertThat(parsedFs.getCommand()).isEqualTo("npx");
        assertThat(parsedFs.getArgs()).containsExactly("-y", "@modelcontextprotocol/server-filesystem");
        assertThat(parsedFs.getEnv()).containsEntry("FILESYSTEM_ROOT", "/home/user");
    }

    @Test
    void testExportToJsonWithExtensions() throws MCPConfigurationParser.MCPConfigurationException {
        MCPServer server = MCPServer.builder()
                .name("disabled-server")
                .transportType(MCPServer.TransportType.HTTP)
                .url("http://localhost:8080")
                .headers(Map.of("Authorization", "Bearer token"))
                .enabled(false)
                .build();

        Map<String, MCPServer> servers = Map.of("disabled-server", server);

        String json = parser.exportToJson(servers, true);

        // Verify the exported JSON includes extensions
        Map<String, MCPServer> parsedServers = parser.parseFromJson(json);
        assertThat(parsedServers).hasSize(1);

        MCPServer parsed = parsedServers.get("disabled-server");
        assertThat(parsed.isEnabled()).isFalse();
        assertThat(parsed.getTransportType()).isEqualTo(MCPServer.TransportType.HTTP);
        assertThat(parsed.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(parsed.getHeaders()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    void testExportToJsonWithoutExtensionsOmitsDisabledFlag() throws MCPConfigurationParser.MCPConfigurationException {
        MCPServer server = MCPServer.builder()
                .name("server")
                .transportType(MCPServer.TransportType.STDIO)
                .command("node")
                .args(List.of("server.js"))
                .enabled(false)
                .build();

        Map<String, MCPServer> servers = Map.of("server", server);

        String json = parser.exportToJson(servers, false);

        // When parsing back without extensions, enabled should default to true
        Map<String, MCPServer> parsedServers = parser.parseFromJson(json);
        MCPServer parsed = parsedServers.get("server");

        // The disabled flag is not exported when includeExtensions=false,
        // so it defaults to true when parsing
        assertThat(parsed.isEnabled()).isTrue();
    }

    @Test
    void testRoundTripConversion() throws MCPConfigurationParser.MCPConfigurationException {
        String originalJson = """
                {
                  "mcpServers": {
                    "filesystem": {
                      "command": "npx",
                      "args": ["-y", "@modelcontextprotocol/server-filesystem"],
                      "env": {
                        "ROOT": "/home"
                      }
                    },
                    "http-server": {
                      "transport": "http",
                      "url": "http://localhost:8080",
                      "enabled": false
                    }
                  }
                }
                """;

        // Parse
        Map<String, MCPServer> servers = parser.parseFromJson(originalJson);

        // Export with extensions
        String exportedJson = parser.exportToJson(servers, true);

        // Parse again
        Map<String, MCPServer> reparsedServers = parser.parseFromJson(exportedJson);

        // Verify all data is preserved
        assertThat(reparsedServers).hasSize(2);

        MCPServer fs = reparsedServers.get("filesystem");
        assertThat(fs.getCommand()).isEqualTo("npx");
        assertThat(fs.getArgs()).containsExactly("-y", "@modelcontextprotocol/server-filesystem");
        assertThat(fs.getEnv()).containsEntry("ROOT", "/home");
        assertThat(fs.isEnabled()).isTrue();

        MCPServer http = reparsedServers.get("http-server");
        assertThat(http.getTransportType()).isEqualTo(MCPServer.TransportType.HTTP);
        assertThat(http.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(http.isEnabled()).isFalse();
    }

    @Test
    void testParseEmptyMcpServers() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {}
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);
        assertThat(servers).isEmpty();
    }

    @Test
    void testExportEmptyServers() {
        Map<String, MCPServer> servers = Map.of();

        String json = parser.exportToJson(servers, false);

        assertThat(json).contains("\"mcpServers\"");
        assertThat(json).contains("{}");
    }

    @Test
    void testParseUnknownTransportDefaultsToStdio() throws MCPConfigurationParser.MCPConfigurationException {
        String json = """
                {
                  "mcpServers": {
                    "server": {
                      "transport": "unknown-transport",
                      "command": "node",
                      "args": ["server.js"]
                    }
                  }
                }
                """;

        Map<String, MCPServer> servers = parser.parseFromJson(json);
        MCPServer server = servers.get("server");

        // Unknown transport should default to STDIO
        assertThat(server.getTransportType()).isEqualTo(MCPServer.TransportType.STDIO);
    }
}
