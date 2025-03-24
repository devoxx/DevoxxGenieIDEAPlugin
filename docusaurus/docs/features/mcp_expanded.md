---
sidebar_position: 3
---

# MCP Support

Model Context Protocol (MCP) is one of the most powerful features in DevoxxGenie. It enables advanced agent-like capabilities, allowing the LLM to access external tools and services to provide more comprehensive and accurate responses.

## What is MCP?

Model Context Protocol (MCP) is a protocol for integrating external capabilities into LLM interactions:

1. **Access External Information**: Query real-time data, APIs, and services
2. **Perform Actions**: Execute tasks outside of the chat environment
3. **Use Specialized Tools**: Leverage purpose-built tools for specific domains

In DevoxxGenie, MCP support is a crucial step toward full Agentic AI capabilities, where the LLM can actively assist with complex development tasks.

## Benefits of MCP

- **Extended Capabilities**: Enables the LLM to perform actions beyond text generation
- **Real-time Information**: Access up-to-date information from external sources
- **Tool Integration**: Connect specialized tools to enhance LLM capabilities
- **Improved Accuracy**: More accurate responses by accessing required information or services

## How It Works in DevoxxGenie

DevoxxGenie implements MCP server support, allowing you to connect to various MCP servers that provide specialized tools for your LLM conversations. For example:

- **Filesystem MCP**: Interact with files and directories
- **Database MCP**: Query databases directly
- **API MCP**: Make API calls to external services
- **Custom Tool MCP**: Execute specialized tools

When you use MCP in DevoxxGenie:

1. The LLM recognizes when external tools would be helpful
2. It calls the appropriate MCP tool with necessary parameters
3. The tool executes and returns results to the LLM
4. The LLM incorporates the results into its response

## Setting Up MCP

### Prerequisites

- Access to MCP servers (local or remote)
- Understanding of which MCP tools are appropriate for your use case

### Enabling MCP

1. In IntelliJ IDEA, open DevoxxGenie settings
2. Navigate to the "MCP Settings (BETA)" section
3. Enable MCP support by checking the appropriate option
4. Add your MCP servers and configure them as needed

![MCP Settings](/img/mcp-settings.png)

### Using MCP in Conversations

When MCP is configured correctly, you'll see the tools that the MCP brings to your conversations in the DevoxxGenie interface. To use these tools:

1. Start a conversation with your LLM provider
2. Ask questions or give instructions that might require external tools
3. The LLM will automatically use the available MCP tools when appropriate

For example, with Filesystem MCP enabled, you might ask:

```
Can you list all Java files in the src/main directory that implement the Observer pattern?
```

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
