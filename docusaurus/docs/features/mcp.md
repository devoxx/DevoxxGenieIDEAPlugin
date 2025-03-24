---
sidebar_position: 4
---

# MCP Support

Model Context Protocol (MCP) support is a powerful feature introduced in DevoxxGenie v0.5.0 that enables enhanced agent-like capabilities for LLM interactions. MCP allows the LLM to access external tools and services to provide more comprehensive and accurate responses.

## What is MCP?

Model Context Protocol (MCP) is a protocol for integrating external capabilities into LLM interactions. It allows LLMs to:

1. **Access External Information**: Query real-time data, APIs, and services
2. **Perform Actions**: Execute tasks outside of the chat environment
3. **Use Specialized Tools**: Leverage purpose-built tools for specific domains

In DevoxxGenie, MCP support is a crucial step toward full Agentic AI capabilities, where the LLM can actively assist with complex development tasks.

## MCP in DevoxxGenie

DevoxxGenie implements MCP server support, allowing you to connect to various MCP servers that provide specialized tools for your LLM conversations. For example:

- **Filesystem MCP**: Interact with files and directories
- **Database MCP**: Query databases directly
- **API MCP**: Make API calls to external services
- **Custom Tool MCP**: Execute specialized tools

## Setting Up MCP

### Enabling MCP Support

1. In IntelliJ IDEA, open DevoxxGenie settings
2. Navigate to the "MCP Settings (BETA)" section
3. Enable MCP support by checking the appropriate option
4. Add your MCP servers

![MCP Settings](/img/mcp-settings.png)

### Adding an MCP Server

1. Click the "Add" button in the MCP Settings panel
2. Enter the server details:
   - Name: A descriptive name for the server
   - URL: The endpoint URL for the MCP server
   - Optional: Environment variables if required
3. Click "OK" to add the server
4. Enable the server by checking its checkbox in the list

## Using MCP in Conversations

When MCP is configured correctly, you'll see the tools that the MCP brings to your conversations in the DevoxxGenie interface. To use these tools:

1. Start a conversation with your LLM provider
2. Ask questions or give instructions that might require external tools
3. The LLM will automatically use the available MCP tools when appropriate

For example, with Filesystem MCP enabled, you might ask:

```
Can you list all Java files in the src/main directory that implement the Observer pattern?
```

The LLM would use the filesystem tools to search for and analyze the files before responding.

## MCP Debugging

DevoxxGenie includes a debugging feature for MCP requests and responses:

1. Open the "DevoxxGenieMCPLogs" tool window from the bottom panel
2. Observe the requests sent to and responses received from MCP servers
3. Use this information to troubleshoot issues or understand how the LLM is using the tools

![MCP Logs](/img/mcp-logs.png)

## Available MCP Servers

Here are some MCP servers that work well with DevoxxGenie:

### Filesystem MCP

- **Purpose**: Interact with the file system
- **Capabilities**: List directories, read files, search code, create files
- **GitHub**: [mcp-plugins/filesystem](https://github.com/mcp-plugins/filesystem)

### Browser MCP

- **Purpose**: Web browsing capabilities
- **Capabilities**: Search the web, read webpage content, navigate sites
- **GitHub**: [mcp-plugins/browser](https://github.com/mcp-plugins/browser)

### Custom MCPs

You can also create your own MCP servers using the [MCP specification](https://github.com/s-macke/mcp-spec). This allows you to extend DevoxxGenie with domain-specific tools tailored to your development needs.

## Troubleshooting MCP Issues

### Connection Problems

If you're having trouble connecting to an MCP server:

1. Verify the server is running and accessible
2. Check the URL in the MCP settings
3. Ensure any required environment variables are set correctly
4. Check the MCP logs for error messages

### Tool Usage Issues

If the LLM isn't using the MCP tools as expected:

1. Be more explicit in your instructions
2. Check if the tools are showing up in the interface
3. Verify the LLM provider you're using supports MCP adequately
4. Review the MCP logs to see if there are any issues with the tool invocation

## Future MCP Enhancements

The DevoxxGenie team is continuously working on enhancing MCP capabilities:

- **More pre-configured MCP servers**
- **Improved debugging and visualization of MCP interactions**
- **Enhanced integration with project-specific tools**
- **Support for more complex workflows and multi-step operations**
