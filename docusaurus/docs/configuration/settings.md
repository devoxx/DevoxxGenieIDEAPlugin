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

### LLM providers (main DevoxxGenie page)

This is the main settings page where you can:

- Select and configure LLM providers
- Enter API keys for cloud providers, including NVIDIA
- Enable routers and enter their API keys
- Manage [favorite models](../features/chat-interface.md#favorite-models)
- Enable [Show Thinking](../features/chat-interface.md#show-thinking) for supported reasoning models
- Configure endpoints for local providers
- Enable optional providers (Azure OpenAI, Amazon Bedrock)

The providers are grouped in five tabs:

| Tab | Contains |
|-----|----------|
| **Local** | Endpoints for providers running on your machine, such as Ollama and LM Studio |
| **Cloud Direct** | API keys for [cloud providers](../llm-providers/cloud-models.md) you have your own account with, such as OpenAI, Anthropic and Google |
| **Routers** | A checkbox and API key for each [router](../llm-providers/cloud-models.md#routers): OpenRouter, Cloudflare AI Gateway, OrcaRouter, Requesty and DemonRoute |
| **Sharding** | Exo, which splits a model across several Apple Silicon devices. Other solutions are listed as coming soon |
| **Custom OpenAI** | The URL, model and API key of any other [OpenAI-compatible endpoint](../llm-providers/custom-providers.md) |

![The Local tab in the LLM settings](/img/settings-local.png)

![The Cloud Direct tab in the LLM settings](/img/settings-cloud-direct.png)

![The Routers tab in the LLM settings](/img/settings-routers.png)

![The Custom OpenAI tab in the LLM settings](/img/settings-custom-openai.png)

### LLM Settings

Configure general settings for LLM interactions:

- Default temperature for responses
- Response format preferences
- Timeout settings
- Request parameters
- Chat memory size (default **50**, range **1–500**); see [Chat Memory](../features/chat-memory.md)

### System Prompt & Personas

Configure the system prompt and keyboard shortcuts:

- Customize the system prompt that sets LLM behavior
- Configure keyboard shortcuts for prompt submission
- Enable and manage [Personas](prompts.md#personas) that replace the base system prompt for new conversations
- Generate and manage the DEVOXXGENIE.md project description file

See [Prompts Configuration](prompts.md) for details.

### Commands

Configure slash commands that trigger predefined prompts:

- View and manage built-in commands (`/test`, `/explain`, `/review`, `/find`, etc.)
- Create custom commands with `$ARGUMENT` placeholder support
- Restore default commands

See [Commands](../features/commands.md) for details.

### Skills

Manage LLM-activated `SKILL.md` capabilities loaded from disk:

- View all skills detected across the six supported directories (user/project × `.agents`/`.claude`/`.devoxxgenie`)
- Enable or disable individual skills
- Open (and auto-create) any of the six skill directories from the dropdown
- Reload skills from disk without restarting the IDE
- Discover downloadable skills through curated repository and directory links

Requires **Agent Mode** to be enabled. See [Skills](../features/skills.md) for details.

### MCP Settings

Configure Model Context Protocol (MCP) servers:

- Enable/disable MCP support
- Browse and install servers from the MCP Marketplace
- Add and configure MCP servers manually (STDIO and Streamable HTTP transports)
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
- Exclude specific files and directories
- Respect `.gitignore` and optionally exclude JavaDoc
- Save these preferences per project; new projects start from existing global defaults

There are no configurable scan-depth, file-size, line-count, file-count, or token-limit controls on this page.

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

### Analytics

Choose whether to send anonymous usage data and review the payload categories. No analytics is sent before consent acknowledgement or when disabled. See [Privacy and analytics](privacy-and-analytics.md), including the distinction between local inference and other network-enabled features.

### Debug

Enable raw request/response, MCP, or agent logs in the Activity Log. Raw logs can include sensitive conversation text even when likely credentials are masked. See [Debug and Activity Log](debugging.md).

### Docker Agentic Platform (PREVIEW)

Configure the external `ap` CLI path, authentication, and optional Agentic Platform tab. See [Docker Agentic Platform preview](../features/docker-agentic-platform.md) for setup and limitations.

### Other feature settings

- **Agent Mode**: [Built-in tools, approvals, tool descriptions, and parallel exploration](../features/agent-mode.md).
- **Spec Driven Development**: [Task/spec management](../features/spec-driven-development.md) and [batch agent runs](../features/sdd-agent-loop.md).
- **Security Scanning**: [Scanner setup and optional finding-to-task creation](../features/security-scanning.md).
- **CLI/ACP Runners**: [CLI](../features/cli-runners.md) and [ACP](../features/acp-runners.md) executable configuration.
- **Inline Completion**: [FIM provider, model, and timing controls](../features/inline-completion.md).
- **Event Automations**: [Event triggers, agent prompts, and confirmation](../features/event-automations.md).

## Global Settings Options

At the bottom of each settings page, you'll find common actions:

- **Apply**: Apply changes without closing the settings dialog
- **OK**: Apply changes and close the settings dialog
- **Cancel**: Discard changes and close the settings dialog
- **Reset**: Reload saved settings and discard pending edits; this is not a factory reset. Pages that offer a separate restore/defaults action label it explicitly.

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
