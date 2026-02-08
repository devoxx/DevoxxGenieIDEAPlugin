---
sidebar_position: 4
title: Sub-Agents - Parallel Codebase Exploration
description: Use multiple sub-agents to explore your codebase concurrently. Each sub-agent is a read-only AI assistant that investigates a specific aspect of your project in parallel.
keywords: [devoxxgenie, sub-agents, parallel explore, agent mode, codebase exploration, multi-agent]
---

# Sub-Agents

Sub-agents enable **parallel codebase exploration** by spawning multiple read-only AI assistants that concurrently investigate different aspects of your project. This is especially useful for large codebases where you need to understand multiple components at once.

## How It Works

When agent mode is enabled, the main LLM has access to a `parallel_explore` tool. When the LLM decides it needs to investigate multiple areas of your codebase, it can invoke this tool with a list of exploration queries. Each query is assigned to an independent sub-agent that runs in parallel.

Each sub-agent:

- Has **read-only access** to your project (can read files, list directories, and search for patterns)
- Runs with its own **isolated chat memory** and tool call budget
- Can use a **different LLM provider/model** than the main agent (e.g., a cheaper/faster model)
- Returns a **concise markdown summary** of its findings back to the main agent

The main agent then synthesizes all sub-agent results into a unified response.

## Getting Started

### 1. Enable Agent Mode

Go to **Settings > Tools > DevoxxGenie > Agent** and enable:

- **Enable Agent Mode** (required)
- **Enable Parallel Explore tool** (enabled by default)

### 2. Trigger Sub-Agents

Simply ask the LLM to explore or investigate your codebase. The LLM will automatically decide when to use sub-agents. Here are some example prompts:

- *"Explore this codebase and explain the architecture"*
- *"Investigate how authentication, database access, and API routing work in this project"*
- *"Find all REST endpoints, their request/response types, and the service classes they call"*
- *"Analyze the error handling patterns, logging strategy, and test coverage in this project"*

:::tip
The more specific your exploration queries, the better the results. Asking about multiple distinct topics encourages the LLM to spawn parallel sub-agents rather than investigating sequentially.
:::

## Configuration

All sub-agent settings are in **Settings > Tools > DevoxxGenie > Agent**.

### Parallel Exploration Settings

| Setting | Default | Range | Description |
|---------|---------|-------|-------------|
| **Enable Parallel Explore tool** | Enabled | On/Off | Enables or disables the `parallel_explore` tool |
| **Max concurrent sub-agents** | 3 | 1-5 | Maximum number of sub-agents running in parallel |
| **Max tool calls per sub-agent** | 200 | 1-200 | How many file reads/searches each sub-agent can perform |
| **Sub-agent timeout** | 120s | 10-600s | Maximum time each sub-agent can run before being stopped |

### Default Sub-Agent Model

You can configure a **default provider and model** for all sub-agents. This is separate from the main agent's model, allowing you to use a cheaper or faster model for exploration tasks.

- **Default sub-agent provider**: Select a provider or choose *"None (Auto-detect)"* to let the plugin choose automatically (tries Ollama first, then OpenAI)
- **Default sub-agent model**: Select the model to use with the chosen provider

:::tip Cost Optimization
Using a smaller, faster model (e.g., `gpt-4o-mini`, `qwen2.5:7b`, or `gemini-flash`) for sub-agents can significantly reduce costs while maintaining good exploration quality. Sub-agents only need to read and summarize code, not generate complex implementations.
:::

### Per-Agent Model Overrides

For advanced users, you can assign a **specific model to each sub-agent slot**. This appears as a dynamic list based on your parallelism setting. Each row lets you select a different provider/model, or leave it as *"Use default"* to inherit the default sub-agent model.

This enables scenarios like:
- Sub-agent #1: Uses a powerful model for complex analysis
- Sub-agent #2: Uses a fast local model for simple file scanning
- Sub-agent #3: Uses a different cloud provider for cost balancing

## Sub-Agent Tools

Sub-agents have access to three **read-only** tools:

| Tool | Description |
|------|-------------|
| `read_file` | Read the contents of a file in the project |
| `list_files` | List files and directories (with optional recursion) |
| `search_files` | Search for regex patterns across project files |

Sub-agents **cannot** write files, execute commands, or access MCP servers. This ensures safe, non-destructive exploration.

## Debugging Sub-Agents

### Agent Log Panel

Open the **Agent Log** tool window to see real-time activity from both the main agent and all sub-agents. Each log entry shows:

- **Counter**: `[call#/max]` showing progress against the tool call limit
- **Sub-agent ID**: `[sub-agent-1:Provider:model]` identifying which sub-agent and its model
- **Action**: What tool was called and with what arguments

You can also **copy all logs to clipboard** using the toolbar button for sharing or analysis.

### Enable Debug Logs

For more verbose output, enable **Agent debug logs** in Agent Settings. This adds detailed logging of tool arguments and results.

### WebView Activity

When sub-agents are running, you'll see real-time activity updates in the chat WebView under the "Agent Activity" section, including:

- Sub-agent start/complete/error events
- Individual tool calls per sub-agent
- Color-coded status indicators

## Architecture Overview

```
User Prompt
    |
    v
Main Agent (your configured LLM)
    |
    |--> parallel_explore(queries: ["query1", "query2", "query3"])
    |         |
    |         |--> Sub-Agent #1 (read_file, list_files, search_files)
    |         |--> Sub-Agent #2 (read_file, list_files, search_files)
    |         |--> Sub-Agent #3 (read_file, list_files, search_files)
    |         |
    |         v
    |    Combined Results (markdown)
    |
    v
Final Response (synthesized by main agent)
```

## Best Practices

- **Use cheaper models for sub-agents**: Sub-agents perform read-only exploration, so smaller models work well
- **Set appropriate timeouts**: Increase the timeout for large codebases, decrease for quick scans
- **Be specific in prompts**: Mentioning multiple distinct topics encourages parallel exploration
- **Monitor tool call limits**: If sub-agents hit their limit, increase the max tool calls setting
- **Check the Agent Log**: If results seem incomplete, the Agent Log shows exactly what each sub-agent investigated

## Troubleshooting

### Sub-agents not triggering

- Ensure **Agent Mode** is enabled in settings
- Ensure **Enable Parallel Explore tool** is checked
- Try being more explicit: *"Use sub-agents to explore..."*

### Sub-agents timing out

- Increase the **Sub-agent timeout** setting (default: 120s)
- Reduce the scope of exploration queries
- Use a faster model for sub-agents

### No model configured

- If you see "No sub-agent model configured", set a default provider in Agent Settings
- Or enable Ollama/OpenAI for auto-detection to work

### Sub-agent results are incomplete

- Increase **Max tool calls per sub-agent** (default: 200)
- Make exploration queries more focused
- Check the Agent Log for errors or limit-reached messages
