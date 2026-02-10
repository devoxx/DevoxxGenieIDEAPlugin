---
sidebar_position: 4
title: Agent Mode
description: Enable agent mode to let the LLM autonomously explore and modify your codebase using built-in tools. Run tests automatically after code changes. Use parallel sub-agents for concurrent read-only exploration of multiple aspects.
keywords: [devoxxgenie, agent mode, sub-agents, parallel explore, codebase exploration, multi-agent, tools, run tests, test execution]
---

# Agent Mode

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
| `run_tests` | Auto-detect build system and run tests with structured results (configurable timeout) |

As you chat with the LLM, it decides when to use these tools. For exploration tasks, the agent might search, list, and read files to understand your codebase. For development tasks, it can create new files, edit existing code, run commands, or run tests to verify changes.

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

## Automated Test Execution

The `run_tests` tool gives the agent the ability to run your project's tests and inspect the results. After modifying code with `write_file` or `edit_file`, the agent automatically runs relevant tests, analyzes any failures, fixes the code, and re-runs until the tests pass.

### Build System Auto-Detection

The tool automatically detects your project's build system by looking for configuration files in the project root:

| Build System | Detected By | Test Command |
|-------------|-------------|--------------|
| **Gradle** | `build.gradle` or `build.gradle.kts` | `./gradlew test` |
| **Maven** | `pom.xml` | `mvn test` |
| **npm** | `package.json` | `npm test` |
| **Cargo** | `Cargo.toml` | `cargo test` |
| **Go** | `go.mod` | `go test ./...` |
| **Make** | `Makefile` | `make test` |

The agent can also target specific tests (e.g., a single class or method) by passing a `test_target` parameter, which is translated into the appropriate flag for each build system.

### Structured Results

Unlike `run_command`, the `run_tests` tool parses test output and returns structured results to the LLM:

- **Status**: PASSED, FAILED, ERROR, or TIMEOUT
- **Counts**: tests run, passed, failed, skipped
- **Failed test names**: extracted from the output for targeted debugging
- **Raw output**: included on failure so the agent can diagnose the issue

### Custom Test Command

If auto-detection doesn't fit your project, you can set a custom test command in **Settings > Tools > DevoxxGenie > Agent > Test Execution**. Use `{target}` as a placeholder for specific test targets:

```
./gradlew test --tests "{target}"
npm run test -- {target}
```

:::tip Test-Driven Development with Agent Mode
Ask the agent to write tests first, then implement the code:

*"Write a unit test for a `reverseString` method in StringUtils, then implement the method and run the tests to verify it works."*

The agent will create the test, write the implementation, call `run_tests`, and iterate until everything passes.
:::

---

## Parallel Sub-Agents

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

### Test Execution Settings

| Setting | Default | Range | Description |
|---------|---------|-------|-------------|
| **Enable Run Tests tool** | Enabled | On/Off | Enables or disables the `run_tests` tool |
| **Test timeout** | 300s | 10-600s | Maximum time a test run can take before being stopped |
| **Custom test command** | *(empty)* | — | Override auto-detected test command. Use `{target}` for specific test targets |

### Parallel Exploration Settings

| Setting | Default | Range | Description |
|---------|---------|-------|-------------|
| **Enable Parallel Explore tool** | Enabled | On/Off | Enables or disables the `parallel_explore` tool |
| **Max tool calls per sub-agent** | 200 | 1-200 | How many file reads/searches each sub-agent can perform |
| **Sub-agent timeout** | 120s | 10-600s | Maximum time each sub-agent can run before being stopped |

:::note
The number of concurrent sub-agents is determined by the number of rows in the **Per-Agent Model Overrides** section below (up to 10). There is no separate parallelism spinner — simply add or remove sub-agent rows to control parallelism.
:::

### Default Sub-Agent Model

You can configure a **default provider and model** for all sub-agents. This is separate from the main agent's model, allowing you to use a cheaper or faster model for exploration tasks.

- **Default sub-agent provider**: Select a provider or choose *"None (Auto-detect)"* to let the plugin choose automatically (tries Ollama first, then OpenAI)
- **Default sub-agent model**: Select the model to use with the chosen provider

:::tip Cost Optimization
Using a smaller, faster model (e.g., `gpt-4o-mini`, `qwen2.5:7b`, `gemini-flash`, or `glm-4.7-flash` via Ollama) for sub-agents can significantly reduce costs while maintaining good exploration quality. Sub-agents only need to read and summarize code, not generate complex implementations.

For the **main agent coordinator**, `glm-4.7-flash` via Ollama is a very powerful local option that provides excellent reasoning capabilities for orchestrating tool calls and coordinating sub-agents.
:::

### Per-Agent Model Overrides

You can dynamically **add and remove sub-agent slots** using the **"+ Add"** and **"-"** buttons. The number of rows directly controls how many sub-agents can run in parallel (from 1 to 10). Each row lets you select a different provider/model, or leave it as *"Use default"* to inherit the default sub-agent model.

- Click **"+ Add"** to add a new sub-agent slot (up to 10)
- Click **"-"** on any row to remove that sub-agent slot (minimum 1 must remain)
- Row labels automatically re-number when a row is removed

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
    |--> list_files, search_files, run_command, run_tests
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
- **Let the agent verify its own changes**: With `run_tests` enabled, the agent automatically runs tests after code modifications and iterates on failures
- **Target specific tests**: For faster feedback, ask the agent to run a specific test class rather than the full suite
- **Set appropriate timeouts**: Increase the test timeout for large test suites or integration tests, decrease for quick unit tests
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

### Tests not running or build system not detected

- Ensure **Enable Run Tests tool** is checked in Agent Settings
- Verify the project has a recognized build file (`build.gradle`, `pom.xml`, `package.json`, etc.) in the project root
- For non-standard setups, configure a **Custom test command** in Agent Settings
- On Unix/macOS, if the Gradle wrapper (`gradlew`) isn't executable, the tool will automatically use `bash` to run it

### Test execution timing out

- Increase the **Test timeout** setting (default: 300s)
- Ask the agent to target specific test classes instead of running all tests
- Check that your test suite doesn't have hanging tests or infinite loops

### Agent results are incomplete

- Increase **Max tool calls** or **Max tool calls per sub-agent** (default: 200)
- Make exploration queries more focused
- Check the Agent Log for errors or limit-reached messages
