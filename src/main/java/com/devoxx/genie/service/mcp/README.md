# MCP (Model Context Protocol) Support in DevoxxGenie

This package contains the implementation of MCP support in the DevoxxGenie plugin.

## Transport Types

The MCP client supports two transport types:

1. **STDIO** - Communicates with an MCP server via standard input/output, typically by starting a local process.
2. **HTTP SSE** - Communicates with an MCP server via HTTP Server-Sent Events (SSE).

## Usage

1. Configure MCP servers in the plugin settings.
2. Select the appropriate transport type for each server.
3. For STDIO transport, specify the command and arguments.
4. For HTTP SSE transport, specify the SSE URL.
5. Test the connection to verify the server is working and fetch available tools.

## Transport-Specific Configuration

### STDIO Transport

#### NPX
- **Command** - Full path to the command to execute (e.g., `/path/to/npx`).
- **Arguments** - Arguments to pass to the command (e.g., `-y @modelcontextprotocol/server-filesystem /path/to/project`).

#### Java
- **Command** - Full path to the command to execute (e.g., `/path/to/java`).
- **Arguments** - Arguments to pass to the command (e.g., `-jar /path/to/jar-file.jar`).

### HTTP SSE Transport

- **SSE URL** - URL of the SSE endpoint for the MCP server (e.g., `http://localhost:3000/mcp/sse`).

## Implementation Details

- The `MCPExecutionService` creates the appropriate transport based on the server configuration.
- Each transport type has its own panel in the server configuration dialog.
- The MCPSettingsComponent displays server configurations in a table with transport-specific information.
