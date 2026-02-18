---
sidebar_position: 3
title: MCP Support - Model Context Protocol
description: Learn how DevoxxGenie integrates with MCP servers to give your LLM access to external tools, files, databases, and APIs. Enable or disable individual tools per server.
keywords: [devoxxgenie, mcp, model context protocol, tools, agents, intellij plugin, marketplace, tool control]
image: /img/devoxxgenie-social-card.jpg
---

# MCP Support

Model Context Protocol (MCP) is one of the most powerful features in DevoxxGenie. It enables agent-like capabilities, allowing the LLM to access external tools and services to provide more comprehensive and accurate responses.

## What is MCP?

Model Context Protocol (MCP) is a protocol for integrating external capabilities into LLM interactions:

1. **Access External Information**: Query real-time data, APIs, and services
2. **Perform Actions**: Execute tasks outside of the chat environment
3. **Use Specialized Tools**: Leverage purpose-built tools for specific domains

In DevoxxGenie, MCP support enables full agentic AI capabilities where the LLM can actively assist with complex development tasks by calling tools, reading files, querying databases, and more.

## How It Works

When you use MCP in DevoxxGenie:

1. The LLM recognizes when external tools would be helpful
2. It calls the appropriate MCP tool with the necessary parameters
3. The tool executes and returns results to the LLM
4. The LLM incorporates the results into its response

Common MCP server categories include:

- **Filesystem**: Interact with files and directories
- **Database**: Query databases directly
- **API**: Make API calls to external services
- **Browser**: Web browsing and scraping
- **Custom Tools**: Domain-specific tools tailored to your workflow

## MCP Marketplace

DevoxxGenie includes a built-in **MCP Marketplace** that lets you browse, search, and install MCP servers directly from the settings UI.

![MCP Marketplace](/img/MCPMarketplace.jpg)

### Browsing the Marketplace

1. Open **Settings** > **Tools** > **DevoxxGenie** > **MCP**
2. Click the **Browse Marketplace** button
3. Browse or search for servers by name or keyword
4. Click a server to see its details and available tools
5. Click **Install** to add it to your configuration

The Marketplace pulls from the official [MCP server registry](https://modelcontextprotocol.io/) and supports filtering by server type (npm, Docker, remote).

## Setting Up MCP

### Enabling MCP

1. Open **Settings** > **Tools** > **DevoxxGenie** > **MCP**
2. Check **Enable MCP Support**
3. Add MCP servers via the Marketplace or manually

### Transport Types

DevoxxGenie supports three MCP transport types:

| Transport | Description | Use Case |
|-----------|-------------|----------|
| **STDIO** | Communicates via standard input/output with a local process | npm packages, Docker containers, local scripts |
| **HTTP SSE** | HTTP with Server-Sent Events for streaming | Remote servers with streaming support |
| **HTTP** | Standard HTTP requests | Simple remote servers |

### Adding a Server Manually

1. Click the **+** (Add) button in the MCP settings
2. Choose a transport type
3. For **STDIO** servers: enter the command, arguments, and optional environment variables
4. For **HTTP/HTTP SSE** servers: enter the server URL and optional custom headers
5. Click **OK**

### Importing and Exporting Configuration

![MCP Import Export](/img/MCPImportExport.jpg)

DevoxxGenie supports importing and exporting MCP server configurations in JSON format. This is useful for:
- **Sharing configurations** with your team
- **Backing up** your MCP setup
- **Migrating** between machines
- **Using configurations** from other MCP-compatible tools

#### Import from JSON

1. Click the **Import from JSON** button in the MCP settings
2. Select a JSON file containing MCP server configurations
3. Choose whether to **Replace** existing servers or **Merge** with them
4. The servers will be added to your configuration

#### Export to JSON

1. Click the **Export to JSON** button in the MCP settings
2. Choose whether to include DevoxxGenie-specific extensions (transport type, enabled status, headers)
3. Select a location and filename for the export
4. The configuration will be saved in Anthropic standard format

#### JSON Format

The import/export follows the Anthropic/Claude Desktop standard format:

```json
{
  "mcpServers": {
    "server-name": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem"],
      "env": {
        "API_KEY": "value"
      }
    }
  }
}
```

DevoxxGenie also supports extensions for transport type (`stdio`, `http`, `http-sse`), enabled status, custom headers, and URLs for HTTP transports.

### Custom HTTP Headers

For HTTP and HTTP SSE transport types, you can configure custom HTTP headers. This is useful for authenticated MCP servers that require API keys or bearer tokens:

```
Authorization: Bearer your-api-key
X-Custom-Header: value
```

### Environment Variables

STDIO servers often need environment variables (e.g., API keys). You can configure these per server in the MCP settings dialog.

## Human-in-the-Loop Approval

DevoxxGenie supports a configurable approval workflow for MCP tool executions. When enabled, you'll be prompted to approve or deny each tool call before it runs.

### Enabling Approval

1. In **MCP Settings**, check **Enable Approval Required**
2. Set the **Approval Timeout** (default: 60 seconds)

When the LLM tries to call an MCP tool, a dialog appears showing:
- The tool name
- The arguments being passed

You can **Approve** or **Deny** the execution. If the timeout expires without a response, the call is denied by default.

This feature is recommended when using MCP servers that can modify files, send messages, or perform other side-effect actions.

## Using MCP in Conversations

When MCP is configured, the available tools are shown in the DevoxxGenie interface. To use them:

1. Start a conversation with your LLM provider
2. Ask questions or give instructions that might require external tools
3. The LLM will automatically use the available MCP tools when appropriate

For example, with a Filesystem MCP server enabled:

```
Can you list all Java files in the src/main directory that implement the Observer pattern?
```

## MCP Debugging

DevoxxGenie includes a debugging panel for MCP requests and responses:

![MCP Debugging](/img/MCPDebug.jpg)

1. Enable **MCP Logging** in the MCP settings
2. Open the **DevoxxGenieMCPLogs** tool window from the bottom panel
3. Observe the requests sent to and responses received from MCP servers
4. **Double-click any log entry** to view the full JSON formatted output in a dialog
5. Use this information to troubleshoot issues or understand how the LLM is using the tools

## Viewing Available Tools

Each MCP server exposes a set of tools. To see what tools a server provides:

1. In the MCP settings table, click the **View** button for a server
2. A dialog shows a table of tool names and their descriptions
3. The tools column in the main table also shows a summary count

## Per-Tool Enable/Disable

You can selectively enable or disable individual tools exposed by each MCP server. This gives you fine-grained control over which capabilities the LLM can use during conversations.

![MCP Tools Activation](/img/MCPToolsActivation.jpg)

### How to Use

1. In the MCP settings table, click the **View** button for a server
2. Each tool is listed with a checkbox next to its name
3. Uncheck a tool to disable it — the LLM will no longer be able to call it
4. Check a tool to re-enable it

### Use Cases

- **Security**: Disable write or delete tools on a filesystem server to make it read-only
- **Focus**: Disable tools that aren't relevant to your current task to reduce noise
- **Testing**: Temporarily disable a tool to see how the LLM behaves without it
- **Cost control**: Disable expensive API-calling tools when not needed

## Troubleshooting

### Connection Problems

If you're having trouble connecting to an MCP server:

1. Verify the server is running and accessible
2. Check the URL or command in the MCP settings
3. Ensure any required environment variables are set correctly
4. Check the MCP logs for error messages

### Tool Usage Issues

If the LLM isn't using the MCP tools as expected:

1. Be more explicit in your instructions
2. Check if the tools are showing up in the interface
3. Verify the LLM provider you're using supports tool calling
4. Review the MCP logs to see if there are any issues with the tool invocation

### STDIO Servers Not Starting

1. Verify the command is installed and available in your PATH (e.g., `npx`, `docker`)
2. Check that all required arguments are configured
3. Look at MCP logs for startup errors

## Learn More

- [Model Context Protocol specification](https://modelcontextprotocol.io/)
- [MCP Server Registry](https://modelcontextprotocol.io/)
