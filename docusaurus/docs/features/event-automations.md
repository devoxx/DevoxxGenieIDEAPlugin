---
sidebar_position: 15
title: Event Automations - AI Agents Triggered by IDE Events
description: DevoxxGenie Event Automations let you configure AI agents that automatically activate in response to IDE events like commits, build failures, test failures, and more.
image: /img/devoxxgenie-social-card.jpg
keywords: [devoxxgenie, event automations, ide triggers, ai agents, code review, debug agent, test generator, build fix, scaffolder, onboarding, custom agents]
---

# Event Automations (POC)

:::info Proof of Concept
Event Automations is an experimental feature that brings Cursor-style intelligent automations to IntelliJ IDEA. It allows AI agents to react to IDE events automatically — reviewing code before commits, debugging test failures, scaffolding new files, and more.
:::

## Overview

Event Automations connects **IDE events** (triggers) to **AI agents** (actions). When an event occurs — such as a build failure, a test failing, or a file being created — the configured agent automatically activates with a tailored prompt, providing contextual assistance without manual intervention.

This turns DevoxxGenie from a reactive assistant (you ask, it answers) into a **proactive development partner** that watches what you do and offers help at exactly the right moment.

## How It Works

```
IDE Event (trigger)  →  Agent (action)  →  Result (assistance)
─────────────────       ──────────────      ──────────────────
Build Failed         →  Build Fix Agent  →  "Error in UserDao.java:42.
                                             Missing null check. Here's
                                             the fix..."
```

1. **An IDE event fires** — IntelliJ notifies DevoxxGenie via platform listeners
2. **The matching agent activates** — The configured agent receives the event context (error messages, file contents, stack traces, etc.)
3. **The agent produces a response** — The result appears in the DevoxxGenie chat panel, with actionable suggestions

Each automation can be configured with:
- **Enabled/Disabled** — Toggle individual automations on or off
- **Custom Prompt** — Tailor what the agent does when triggered
- **Auto-run** — Skip the confirmation dialog and run immediately

## Settings Panel

Navigate to **Settings → DevoxxGenie → Event Automations (POC)** to configure your automations.

The settings panel provides:
- A **master toggle** to enable/disable all event automations
- A **table** listing all configured event-agent mappings with columns for: Enabled, Category, IDE Event, Agent, and Auto-run
- **Add/Edit/Remove** toolbar to manage mappings
- **Load Default Automations** button to restore the built-in defaults

### Adding an Automation

Click the **+** button to open the Add Event Automation dialog:

1. **Select an IDE Event** — Choose from the categorized list of supported events
2. **Select an Agent** — Pick a built-in agent or choose "Custom Agent" to define your own
3. **Customize the Prompt** — Edit the agent's prompt to fit your workflow (the default prompt auto-fills when you select a built-in agent)
4. **Auto-run** — Check this box to skip the confirmation dialog

## Supported IDE Events

Events are organized into categories that map to natural development workflows:

### VCS / Git

| Event | Trigger | Example Use Case |
|-------|---------|-----------------|
| **Before Commit** | Fires before a git commit is created | Run Code Review Agent to catch bugs before they're committed |
| **After Commit** | Fires after a git commit is created | Generate changelog entry or update issue tracker |
| **Branch Switch** | Fires when switching git branches | Summarize branch context and in-progress work |
| **After Pull/Merge** | Fires after a git pull or merge | Detect and help resolve merge conflicts |
| **Before Push** | Fires before pushing to remote | Final review of all commits being pushed |

### File & Editor

| Event | Trigger | Example Use Case |
|-------|---------|-----------------|
| **File Saved** | Fires when a file is saved | Run related tests automatically |
| **File Created** | Fires when a new file is created | Scaffold boilerplate matching project conventions |
| **File Opened** | Fires when a file is opened in editor | Explain unfamiliar code and its dependencies |

### Build & Compilation

| Event | Trigger | Example Use Case |
|-------|---------|-----------------|
| **Build Failed** | Fires when a build fails with errors | Analyze errors and propose fixes |
| **Build Succeeded** | Fires after a successful build | Check for deprecation warnings or optimization opportunities |
| **Gradle Sync Complete** | Fires after Gradle sync finishes | Check for vulnerable or outdated dependencies |

### Testing

| Event | Trigger | Example Use Case |
|-------|---------|-----------------|
| **Test Failed** | Fires when a test fails | Debug agent analyzes failure, identifies root cause |
| **Test Suite Passed** | Fires when all tests pass | Identify coverage gaps in recently changed code |
| **Test Run Complete** | Fires when a test run finishes | Compare execution times, flag performance regressions |

### Code Structure

| Event | Trigger | Example Use Case |
|-------|---------|-----------------|
| **New Method Added** | Fires when a new method is added (PSI) | Generate unit tests for the new method |
| **Interface Changed** | Fires when an interface is modified | Find and update implementations that are now out of sync |

### Run / Debug

| Event | Trigger | Example Use Case |
|-------|---------|-----------------|
| **Exception During Debug** | Fires when an exception breakpoint is hit | Explain the exception with full variable context |
| **Process Crashed** | Fires when a process exits with non-zero code | Post-mortem analysis of logs and heap dumps |

### Project Lifecycle

| Event | Trigger | Example Use Case |
|-------|---------|-----------------|
| **Project Opened** | Fires when a project is opened | Onboarding agent scans project and provides overview |

## Built-in Agents

DevoxxGenie ships with eight built-in agents, each designed for a specific class of development task:

### Code Review Agent
**Best paired with:** Before Commit, Before Push

Reviews code changes for bugs, security issues, and style violations. Provides specific line-number references and concrete fixes.

> *"You're introducing a SQL injection in UserDao.java:42. The `query` parameter is concatenated directly into the SQL string. Here's a parameterized version..."*

### Build Fix Agent
**Best paired with:** Build Failed

Parses compiler errors, reads the failing source files, and proposes fixes with explanations. Handles missing imports, type mismatches, and ambiguous method calls.

> *"Build failed: 3 errors. Missing import → added automatically. Type mismatch on line 87 → suggested cast. Ambiguous method call → 2 options shown."*

### Debug Agent
**Best paired with:** Test Failed, Exception During Debug

Reads the stack trace, the source under test, and recent changes to those files to identify root causes and suggest fixes.

> *"testUserLogin failed: NullPointerException at AuthService:55. Root cause: commit abc123 removed null check in getUserById(). Suggested fix: [diff shown]"*

### Test Generator Agent
**Best paired with:** New Method Added

Generates comprehensive unit tests for new or changed code, including edge cases, boundary conditions, and error scenarios. Follows the project's existing test framework and conventions.

### Code Explainer Agent
**Best paired with:** File Opened

Provides a brief summary of unfamiliar code files — purpose, key methods, dependencies, and architectural context.

> *"This file handles JWT token validation. Key methods: validate(), refresh(). Called by AuthFilter. Last modified 3 weeks ago."*

### Scaffold Agent
**Best paired with:** File Created

Generates appropriate boilerplate for new files based on location, type, and project conventions — imports, class structure, license headers, and framework annotations.

### Dependency Check Agent
**Best paired with:** Gradle Sync Complete

Checks project dependencies for known CVEs, available updates, and unused dependencies. Prioritizes critical security issues.

> *"2 dependencies have critical CVEs. log4j-core 2.14 → upgrade to 2.21. jackson-databind has CVE-2024-XXXXX."*

### Onboarding Agent
**Best paired with:** Project Opened

Scans the project structure and provides an overview: tech stack, key entry points, build system, test status, and configuration files needing attention.

> *"Welcome to payment-service. Stack: Spring Boot 3.2, PostgreSQL, Kafka. 47 open TODOs. 3 failing tests on main."*

## Custom Agents

Beyond the built-in agents, you can create **custom agents** for workflows specific to your team or project:

1. In the Add Event Automation dialog, select **"Custom Agent"** as the agent type
2. Give your custom agent a **name** (e.g., "Security Scanner", "API Doc Generator")
3. Write a **custom prompt** that tells the agent exactly what to do

### Custom Agent Ideas

| Agent Name | Event | What It Does |
|-----------|-------|-------------|
| Commit Message Writer | After Commit | Rewrites vague commit messages with proper conventional commit format |
| Migration Generator | Interface Changed | Generates database migrations when entity classes change |
| i18n Checker | File Saved | Scans for hardcoded strings that should be internationalized |
| API Doc Sync | Method Added | Updates OpenAPI/Swagger docs when new endpoints are added |
| Performance Guard | Test Run Complete | Compares test durations against baselines, flags regressions |

## Default Automations

When you first open the Event Automations settings, DevoxxGenie loads a set of **default automations** (all disabled by default):

| IDE Event | Agent | Description |
|-----------|-------|-------------|
| Before Commit | Code Review Agent | Review changes before committing |
| Build Failed | Build Fix Agent | Analyze and fix build errors |
| Test Failed | Debug Agent | Debug failing tests |
| File Created | Scaffold Agent | Generate boilerplate for new files |
| File Opened | Code Explainer Agent | Explain unfamiliar files |
| New Method Added | Test Generator Agent | Generate tests for new methods |
| Gradle Sync Complete | Dependency Check Agent | Check for vulnerable dependencies |
| Project Opened | Onboarding Agent | Project overview on open |

Enable the ones you want by checking the **Enabled** checkbox in the table.

## Auto-run vs. Confirmation

Each automation can be configured in two modes:

- **Confirmation mode** (default): When the event fires, a dialog appears asking "Run [Agent Name]?" — you choose to proceed or skip
- **Auto-run mode**: The agent runs immediately without asking. Best for low-risk, high-frequency agents (e.g., Explainer on File Opened)

:::tip
Start with confirmation mode for all agents. Once you're comfortable with an agent's behavior, switch to auto-run for a smoother workflow.
:::

## Architecture

The Event Automations system is built on three core model classes:

### IdeEventType
An enum of all supported IDE events, organized by `Category` (VCS, File, Build, Test, Code, Debug, Lifecycle). Each event has a display name and description.

### AgentType
An enum of built-in agents (Code Review, Build Fix, Debug, Test Generator, Explainer, Scaffolder, Dependency Check, Onboarding) plus a `CUSTOM` type for user-defined agents. Each built-in agent carries a default prompt.

### EventAgentMapping
A data class that connects an event to an agent, with fields for: enabled, event type, agent type, custom agent name, prompt, and auto-run flag.

Settings are persisted via `DevoxxGenieStateService` in IntelliJ's application-level XML storage (`DevoxxGenieSettingsPlugin.xml`).

## MCP Integration

Event Automations can be combined with [MCP servers](/docs/features/mcp_expanded) for even more powerful workflows:

- **Before Commit + Linear MCP**: Auto-update issue status when code is committed
- **Test Failed + Slack MCP**: Post failure notifications to team channels
- **Build Failed + Jira MCP**: Create bug tickets automatically

This combination turns DevoxxGenie into a full **workflow automation platform** that bridges your IDE with external tools and services.

## Roadmap

This POC lays the groundwork for future enhancements:

- **Event listener implementation** — Wire up actual IntelliJ platform listeners (`CheckinHandlerFactory`, `CompilationStatusListener`, `SMTRunnerEventsListener`, etc.)
- **Context injection** — Pass rich event context to agents (error messages, stack traces, diff contents, file AST)
- **Agent chaining** — Trigger multiple agents in sequence (e.g., Build Fix → then re-build → then Test)
- **Conditional triggers** — Add filters like "only for files in `src/main/`" or "only on branch `main`"
- **Scheduled triggers** — Cron-style automations (daily tech debt report, weekly docs check)
- **Agent history** — View past agent activations and their results
- **Import/Export** — Share automation configurations across teams

## FAQ

**Q: Will this slow down my IDE?**
A: No. The settings panel is purely configuration. Agent activations happen asynchronously and don't block the EDT (Event Dispatch Thread). Auto-run agents execute in background threads.

**Q: Can I use any LLM provider?**
A: Yes. Event Automations use whichever LLM provider and model you have configured in DevoxxGenie's main settings. Local models (Ollama, LMStudio) and cloud models (OpenAI, Anthropic, etc.) all work.

**Q: What happens if the same event fires rapidly?**
A: A debounce mechanism prevents duplicate activations. For events like File Saved, the agent waits for a brief pause before activating.

**Q: Can I have multiple agents for the same event?**
A: Yes. You can add multiple mappings for the same IDE event, each with a different agent. They will execute in the order listed.
