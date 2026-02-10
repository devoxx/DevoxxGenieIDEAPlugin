---
sidebar_position: 1
title: Settings Configuration
description: Comprehensive guide to configuring DevoxxGenie settings including LLM providers, API keys, endpoints, prompts, MCP, RAG, and appearance options.
keywords: [devoxxgenie, settings, configuration, api keys, llm providers, intellij plugin, setup, preferences]
image: /img/devoxxgenie-social-card.jpg
---

# Settings Configuration

DevoxxGenie offers a comprehensive settings interface that allows you to customize the plugin according to your preferences and needs.

## Accessing DevoxxGenie Settings

There are several ways to access the DevoxxGenie settings:

1. **From the DevoxxGenie Window**: Click the gear icon in the top-right corner of the DevoxxGenie tool window
2. **From IDE Settings**: Navigate to `Settings` (or `Preferences` on macOS) > `Tools` > `DevoxxGenie`
3. **Using Keyboard Shortcut**: Press `Ctrl+Alt+S` (or `Cmd+,` on macOS) to open settings, then navigate to `Tools` > `DevoxxGenie`

## Settings Categories

DevoxxGenie's settings are organized into the following categories:

### LLM Providers

This is the main settings page where you can:

- Select and configure LLM providers
- Enter API keys for cloud providers
- Configure endpoints for local providers
- Enable optional providers (Azure OpenAI, Amazon Bedrock)

### LLM Settings

Configure general settings for LLM interactions:

- Default temperature for responses
- Response format preferences
- Timeout settings
- Request parameters

### Prompts

Configure the system prompt and keyboard shortcuts:

- Customize the system prompt that sets LLM behavior
- Configure keyboard shortcuts for prompt submission
- Generate and manage the DEVOXXGENIE.md project description file

See [Prompts Configuration](prompts.md) for details.

### Skills

Configure slash commands that trigger predefined prompts:

- View and manage built-in skills (`/test`, `/explain`, `/review`, `/find`, etc.)
- Create custom skills with `$ARGUMENT` placeholder support
- Restore default skills

See [Skills](../features/skills.md) for details.

### MCP Settings

Configure Model Context Protocol (MCP) servers:

- Enable/disable MCP support
- Browse and install servers from the MCP Marketplace
- Add and configure MCP servers manually (STDIO, HTTP, HTTP SSE transports)
- Enable human-in-the-loop approval with configurable timeout
- Enable MCP logging for debugging

See [MCP Support](../features/mcp_expanded.md) for details.

### Web Search

Configure web search integration:

- Enable/disable web search
- Set up Google Custom Search
- Configure Tavily search integration
- Set search result limits

### Scan & Copy Project

Configure project scanning options:

- Include/exclude file patterns
- Set depth limits for scanning
- Configure handling of large files
- Set up code extraction preferences

### RAG

Configure Retrieval-Augmented Generation:

- Set up ChromaDB connection (port)
- Configure max results and min similarity score
- Manage project indexing

See [RAG Support](../features/rag.md) for details.

### Appearance

Customize the visual appearance of the chat interface:

- Override theme colors for user and assistant messages
- Adjust spacing (line height, padding, margin, border width, corner radius)
- Override font sizes for message text and code blocks

See [Appearance Settings](appearance.md) for details.

### Token Cost & Context Window

Configure token usage and cost settings:

- Set token cost rates for different providers
- Configure context window sizes
- Set up token usage alerts
- Configure token calculation preferences

## Global Settings Options

At the bottom of each settings page, you'll find common actions:

- **Apply**: Apply changes without closing the settings dialog
- **OK**: Apply changes and close the settings dialog
- **Cancel**: Discard changes and close the settings dialog
- **Reset**: Reset settings to their default values

## Settings Storage

DevoxxGenie settings are stored using the IntelliJ Platform's built-in settings framework:

- Project-specific settings are stored in your project directory
- Application-wide settings are stored in the IntelliJ configuration directory
- Sensitive data like API keys are stored in the system's secure storage

## Troubleshooting Settings Issues

If you encounter issues with DevoxxGenie settings:

1. **Settings Not Saving**: Ensure you're clicking "Apply" or "OK" after making changes
2. **API Keys Not Working**: Check that you've entered the correct API key and that it's valid
3. **Settings Conflicts**: If settings seem inconsistent, try resetting to defaults and reconfiguring
4. **Missing Settings**: Ensure you're using a compatible version of IntelliJ and DevoxxGenie

If problems persist, consider:
- Invalidating caches and restarting IntelliJ (File > Invalidate Caches / Restart...)
- Reinstalling the DevoxxGenie plugin
- Checking for conflicting plugins

For more specific issues, consult the [GitHub Issues](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) page.
