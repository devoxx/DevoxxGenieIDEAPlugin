---
sidebar_position: 1
---

# Settings Configuration

DevoxxGenie offers a comprehensive settings interface that allows you to customize the plugin according to your preferences and needs.

## Accessing DevoxxGenie Settings

There are several ways to access the DevoxxGenie settings:

1. **From the DevoxxGenie Window**: Click the gear icon (⚙️) in the top-right corner of the DevoxxGenie tool window
2. **From IDE Settings**: Navigate to `Settings` (or `Preferences` on macOS) → `Tools` → `DevoxxGenie`
3. **Using Keyboard Shortcut**: Press `Ctrl+Alt+S` (or `⌘,` on macOS) to open settings, then navigate to `Tools` → `DevoxxGenie`

## Settings Categories

DevoxxGenie's settings are organized into several categories:

### LLM Providers

This is the main settings page where you can:

- Select and configure LLM providers
- Enter API keys for cloud providers
- Configure endpoints for local providers
- Access provider-specific settings

![LLM Providers Settings](/img/settings-llm-providers.png)

### LLM Settings

Configure general settings for LLM interactions:

- Default temperature for responses
- Response format preferences
- Timeout settings
- Request parameters

### Prompts

Configure prompt templates and custom commands:

- Define custom prompt templates
- Generate the DEVOXXGENIE.md file
- Configure keyboard shortcuts for prompts
- Set up system prompts

### MCP Settings

Configure Model Context Protocol (MCP) settings:

- Enable/disable MCP support
- Add and configure MCP servers
- Set up environment variables for MCP
- Configure MCP logging

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

### LLM Git Diff View

Configure the Git diff integration:

- Enable/disable Git diff
- Choose between two-panel and three-panel views
- Configure diff visualization options
- Set up keyboard shortcuts for diff actions

### RAG

Configure Retrieval-Augmented Generation:

- Set up local embeddings
- Configure ChromaDB
- Set indexing parameters
- Configure document retrieval settings

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

## Importing and Exporting Settings

To backup or transfer your settings:

1. Go to `File` → `Manage IDE Settings` → `Export Settings`
2. Check the box for "DevoxxGenie" settings
3. Choose a location to save the settings file

To import settings:

1. Go to `File` → `Manage IDE Settings` → `Import Settings`
2. Select a settings file to import
3. Ensure the "DevoxxGenie" settings are checked
4. Click "OK" to import

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
- Invalidating caches and restarting IntelliJ (File → Invalidate Caches / Restart...)
- Reinstalling the DevoxxGenie plugin
- Checking for conflicting plugins

For more specific issues, consult the [GitHub Issues](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) page.
