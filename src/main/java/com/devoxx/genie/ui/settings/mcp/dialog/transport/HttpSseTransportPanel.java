package com.devoxx.genie.ui.settings.mcp.dialog.transport;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.devoxx.genie.model.mcp.MCPServer;

import lombok.extern.slf4j.Slf4j;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;

/**
 * Panel for configuring HTTP SSE MCP transport
 */
@Slf4j
public class HttpSseTransportPanel implements TransportPanel {
    private final JPanel panel;
    private final JTextField sseUrlField = new JTextField();
    
    public HttpSseTransportPanel() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 1;
        int row = 0;
        
        // SSE URL field
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("SSE URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(sseUrlField, gbc);

        // Help text
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("<html>Enter the SSE endpoint URL for the MCP server.</html>"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("For example: http://localhost:3000/mcp/sse"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(getExampleLabel(), gbc);

    }
    
    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void loadSettings(MCPServer server) {
        sseUrlField.setText(server.getUrl());
    }

    @Override
    public boolean isValid() {
        String url = sseUrlField.getText().trim();
        return !url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"));
    }

    @Override
    public String getErrorMessage() {
        String url = sseUrlField.getText().trim();
        if (url.isEmpty()) {
            return "SSE URL cannot be empty";
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "SSE URL must start with http:// or https://";
        }
        return null;
    }

    @Override
    public McpClient createClient(Map<String, String> headers) throws Exception {
        String sseUrl = sseUrlField.getText().trim();

        // Validate URL
        if (sseUrl.isEmpty()) {
            throw new IllegalArgumentException("SSE URL cannot be empty");
        }

        if (!sseUrl.startsWith("http://") && !sseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("SSE URL must start with http:// or https://");
        }

        log.debug("Creating HTTP SSE transport with URL: {}", sseUrl);

        // Use Streamable HTTP transport (recommended replacement for legacy HTTP/SSE transport)
        StreamableHttpMcpTransport.Builder transportBuilder = new StreamableHttpMcpTransport.Builder()
                .url(sseUrl)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true);

        if (headers != null && !headers.isEmpty()) {
            transportBuilder.customHeaders(headers);
        }

        McpTransport transport = transportBuilder.build();

        // Create and return the client
        return new DefaultMcpClient.Builder()
                .clientName("DevoxxGenie")
                .protocolVersion("2024-11-05")
                .transport(transport)
                .build();
    }

    @Override
    public void applySettings(MCPServer.MCPServerBuilder builder) {
        builder.url(sseUrlField.getText().trim())
               .transportType(MCPServer.TransportType.HTTP_SSE);
    }
}
