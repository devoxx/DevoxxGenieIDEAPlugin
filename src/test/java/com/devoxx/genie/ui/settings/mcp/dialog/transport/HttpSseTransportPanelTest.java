package com.devoxx.genie.ui.settings.mcp.dialog.transport;

import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for issue #1151.
 * <p>
 * The "HTTP SSE" transport panel must build an SSE-based {@link HttpMcpTransport}
 * (which GETs the {@code /sse} endpoint to open the event stream), NOT a
 * {@code StreamableHttpMcpTransport} (which POSTs to the single URL). Using the
 * streamable transport against a legacy SSE endpoint such as the JetBrains MCP
 * server's {@code /sse} endpoint results in an HTTP 405 (Method Not Allowed).
 */
class HttpSseTransportPanelTest {

    private HttpSseTransportPanel panel;

    @BeforeEach
    void setUp() throws Exception {
        panel = new HttpSseTransportPanel();

        Field urlField = HttpSseTransportPanel.class.getDeclaredField("sseUrlField");
        urlField.setAccessible(true);
        ((JTextField) urlField.get(panel)).setText("http://127.0.0.1:64342/sse");
    }

    @Test
    void createTransportUsesSseTransport() {
        McpTransport transport = panel.createTransport(Map.of());

        assertThat(transport)
                .as("HTTP SSE panel must use the SSE-based HttpMcpTransport, not the streamable HTTP transport (issue #1151)")
                .isInstanceOf(HttpMcpTransport.class);
    }
}
