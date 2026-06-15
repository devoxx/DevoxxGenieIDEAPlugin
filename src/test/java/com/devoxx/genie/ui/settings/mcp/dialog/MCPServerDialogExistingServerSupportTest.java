package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MCPServerDialogExistingServerSupportTest {

    private MCPServerDialog.ExistingServerConfigurationSupport support;

    @BeforeEach
    void setUp() {
        support = new MCPServerDialog.ExistingServerConfigurationSupport();
    }

    @Test
    void headersForConnectionReturnsEmptyMapWhenServerIsNull() {
        assertThat(support.headersForConnection(null)).isEmpty();
    }

    @Test
    void headersForConnectionReturnsEmptyMapWhenServerHasNoHeaders() {
        MCPServer server = MCPServer.builder().name("demo").build();

        assertThat(support.headersForConnection(server)).isEmpty();
    }

    @Test
    void headersForConnectionReturnsDefensiveCopy() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        MCPServer server = MCPServer.builder()
                .name("demo")
                .headers(headers)
                .build();

        Map<String, String> result = support.headersForConnection(server);
        result.put("X-Test", "value");

        assertThat(result).containsEntry("Authorization", "Bearer token");
        assertThat(server.getHeaders()).containsOnlyKeys("Authorization");
        assertThat(server.getHeaders()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    void applyPreservedSettingsCopiesEnvAndHeadersOntoBuilder() {
        MCPServer existingServer = MCPServer.builder()
                .name("demo")
                .env(new HashMap<>(Map.of("API_KEY", "secret")))
                .headers(new HashMap<>(Map.of("Authorization", "Bearer token")))
                .build();
        MCPServer.MCPServerBuilder builder = MCPServer.builder().name("updated");

        support.applyPreservedSettings(existingServer, builder);
        MCPServer rebuilt = builder.build();

        assertThat(rebuilt.getEnv()).containsOnlyKeys("API_KEY");
        assertThat(rebuilt.getEnv()).containsEntry("API_KEY", "secret");
        assertThat(rebuilt.getHeaders()).containsOnlyKeys("Authorization");
        assertThat(rebuilt.getHeaders()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    void applyPreservedSettingsSkipsHeadersWhenExistingServerHasNone() {
        MCPServer existingServer = MCPServer.builder()
                .name("demo")
                .env(new HashMap<>(Map.of("API_KEY", "secret")))
                .build();
        MCPServer.MCPServerBuilder builder = MCPServer.builder().name("updated");

        support.applyPreservedSettings(existingServer, builder);
        MCPServer rebuilt = builder.build();

        assertThat(rebuilt.getEnv()).containsOnlyKeys("API_KEY");
        assertThat(rebuilt.getEnv()).containsEntry("API_KEY", "secret");
        assertThat(rebuilt.getHeaders()).isEmpty();
    }

    @Test
    void applyPreservedHeadersDoesNotOverrideBuilderEnv() {
        MCPServer existingServer = MCPServer.builder()
                .name("demo")
                .env(new HashMap<>(Map.of("API_KEY", "secret")))
                .headers(new HashMap<>(Map.of("Authorization", "Bearer token")))
                .build();
        MCPServer.MCPServerBuilder builder = MCPServer.builder()
                .name("updated")
                .env(new HashMap<>(Map.of("FROM_UI", "value")));

        support.applyPreservedHeaders(existingServer, builder);
        MCPServer rebuilt = builder.build();

        assertThat(rebuilt.getEnv()).containsOnlyKeys("FROM_UI");
        assertThat(rebuilt.getEnv()).containsEntry("FROM_UI", "value");
        assertThat(rebuilt.getHeaders()).containsOnlyKeys("Authorization");
        assertThat(rebuilt.getHeaders()).containsEntry("Authorization", "Bearer token");
    }
}
