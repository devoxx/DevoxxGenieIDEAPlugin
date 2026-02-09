---
sidebar_position: 4
title: Agent Mode
description: Enable agent mode to let the LLM autonomously explore and modify your codebase using built-in tools. Use parallel sub-agents for concurrent read-only exploration of multiple aspects.
keywords: [devoxxgenie, agent mode, sub-agents, parallel explore, codebase exploration, multi-agent, tools]
---

# Agent Mode

:::info Available from v0.9.4
Agent Mode requires DevoxxGenie **v0.9.4** or later.
:::

Agent Mode enables the LLM to autonomously explore and modify your codebase using built-in tools. Instead of relying solely on the context you provide, the agent can investigate your project on-demand—reading files, listing directories, searching for patterns, and even making changes to help you develop faster.

![Agent Mode Settings](/img/agent-mode-top.jpg)

## How Agent Mode Works

When Agent Mode is enabled, the LLM gains access to a set of **built-in tools** that allow it to explore and modify your project dynamically:

### Read Tools

| Tool | Description |
|------|-------------|
| `read_file` | Read the contents of a file in the project |
| `list_files` | List files and directories (with optional recursion) |
| `search_files` | Search for regex patterns across project files |

### Write Tools

| Tool | Description |
|------|-------------|
| `write_file` | Create new files with the specified content |
| `edit_file` | Modify existing files by replacing text |
| `run_command` | Execute terminal commands in the project directory (30s timeout) |

As you chat with the LLM, it decides when to use these tools. For exploration tasks, the agent might search, list, and read files to understand your codebase. For development tasks, it can create new files, edit existing code, or run commands to help you implement features.

:::tip Safety
Write operations require user approval by default. You'll see a diff preview before any changes are applied to your project.
:::

## Getting Started

### 1. Enable Agent Mode

Go to **Settings > Tools > DevoxxGenie > Agent** and enable:

- **Enable Agent Mode** (required)

### 2. Start Chatting

Once enabled, simply ask the LLM questions about your codebase. The agent will automatically use tools when needed to investigate:

- *"How does the database connection work in this project?"*
- *"Find where the user authentication is implemented"*
- *"Explain the project structure and main components"*

:::tip
Agent mode works best for exploratory questions where the LLM needs to discover information across multiple files. For simple questions with provided context, regular chat mode may be more efficient.
:::

---

# Parallel Sub-Agents

## Parallel Codebase Exploration

**Parallel Sub-Agents** extend Agent Mode by spawning multiple read-only AI assistants that concurrently investigate different aspects of your project. This is especially useful for large codebases where you need to understand multiple components at once.

When the LLM decides it needs to investigate multiple areas simultaneously, it can invoke the `parallel_explore` tool with a list of exploration queries. Each query is assigned to an independent sub-agent that runs in parallel.

Each sub-agent:

- Has **read-only access** to your project (read_file, list_files, search_files only)
- Runs with its own **isolated chat memory** and tool call budget
- Can use a **different LLM provider/model** than the main agent (e.g., a cheaper/faster model)
- Returns a **concise markdown summary** of its findings back to the main agent

The main agent then synthesizes all sub-agent results into a unified response.

## Configuration

All agent settings are in **Settings > Tools > DevoxxGenie > Agent**.

![Agent Mode Parallel Settings](/img/agent-mode-parallel.jpg)

### Agent Mode Settings

| Setting | Default | Description |
|---------|---------|-------------|
| **Enable Agent Mode** | Disabled | Enables the agent with full tool access (read, write, and execute) |
| **Enable Debug Logs** | Disabled | Adds detailed logging of tool arguments and results |

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
Using a smaller, faster model (e.g., `gpt-4o-mini`, `qwen2.5:7b`, `gemini-flash`, or `glm-4.7-flash` via Ollama) for sub-agents can significantly reduce costs while maintaining good exploration quality. Sub-agents only need to read and summarize code, not generate complex implementations.

For the **main agent coordinator**, `glm-4.7-flash` via Ollama is a very powerful local option that provides excellent reasoning capabilities for orchestrating tool calls and coordinating sub-agents.
:::

### Per-Agent Model Overrides

For advanced users, you can assign a **specific model to each sub-agent slot**. This appears as a dynamic list based on your parallelism setting. Each row lets you select a different provider/model, or leave it as *"Use default"* to inherit the default sub-agent model.

This enables scenarios like:
- Sub-agent #1: Uses a powerful model for complex analysis
- Sub-agent #2: Uses a fast local model for simple file scanning
- Sub-agent #3: Uses a different cloud provider for cost balancing

## Triggering Parallel Sub-Agents

When Agent Mode is enabled, the **Enable Parallel Explore tool** option (enabled by default) allows the LLM to spawn sub-agents automatically. Here are example prompts that trigger parallel exploration:

- *"Explore this codebase and explain the architecture"*
- *"Investigate how authentication, database access, and API routing work in this project"*
- *"Find all REST endpoints, their request/response types, and the service classes they call"*
- *"Analyze the error handling patterns, logging strategy, and test coverage in this project"*

![Sub-agents prompt example](/img/sub-agents.jpg)

The main agent will spawn parallel sub-agents to investigate different aspects of your codebase, and then synthesize their findings into a unified response:

![Sub-agents response](/img/sub-agents-response.jpg)

:::tip
The more specific your exploration queries, the better the results. Asking about multiple distinct topics encourages the LLM to spawn parallel sub-agents rather than investigating sequentially.
:::

## Debugging

### Agent Log Panel

Open the **Agent Log** tool window to see real-time activity from both the main agent and all sub-agents. Each log entry shows:

- **Counter**: `[call#/max]` showing progress against the tool call limit
- **Sub-agent ID**: `[sub-agent-1:Provider:model]` identifying which sub-agent and its model
- **Action**: What tool was called and with what arguments

You can also **copy all logs to clipboard** using the toolbar button for sharing or analysis.

### WebView Activity

When agents are running, you'll see real-time activity updates in the chat WebView under the "Agent Activity" section, including:

- Agent/sub-agent start/complete/error events
- Individual tool calls
- Color-coded status indicators

## Architecture Overview

### Single Agent Mode

```
User Prompt
    |
    v
Main Agent (your configured LLM)
    |
    |--> read_file, write_file, edit_file
    |--> list_files, search_files, run_command
    |         |
    |         v
    |    Tool Results
    |
    v
Final Response (with context from tools)
```

### Parallel Sub-Agents Mode

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

- **Start with single agent mode**: For simple questions, single agent mode is often sufficient and faster
- **Use parallel sub-agents for complex exploration**: When you need to understand multiple unrelated aspects of your codebase
- **Use cheaper models for sub-agents**: Sub-agents perform read-only exploration, so smaller models work well
- **Set appropriate timeouts**: Increase the timeout for large codebases, decrease for quick scans
- **Be specific in prompts**: Mentioning multiple distinct topics encourages parallel exploration
- **Monitor tool call limits**: If sub-agents hit their limit, increase the max tool calls setting
- **Check the Agent Log**: If results seem incomplete, the Agent Log shows exactly what was investigated

## Troubleshooting

### Agent mode not working

- Ensure **Agent Mode** is enabled in settings
- Check that the model supports tool/function calling
- Try asking questions that require exploration (e.g., "Find all files that...")

### Sub-agents not triggering

- Ensure **Enable Parallel Explore tool** is checked
- Try being more explicit: *"Use sub-agents to explore..."*
- Ask about multiple distinct topics in one prompt

### Sub-agents timing out

- Increase the **Sub-agent timeout** setting (default: 120s)
- Reduce the scope of exploration queries
- Use a faster model for sub-agents

### No model configured

- If you see "No sub-agent model configured", set a default provider in Agent Settings
- Or enable Ollama/OpenAI for auto-detection to work

### Agent results are incomplete

- Increase **Max tool calls** or **Max tool calls per sub-agent** (default: 200)
- Make exploration queries more focused
- Check the Agent Log for errors or limit-reached messages
