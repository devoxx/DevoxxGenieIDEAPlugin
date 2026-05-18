---
sidebar_position: 4
title: Commands - Custom Slash Commands
description: Learn how to use and create Commands in DevoxxGenie — reusable slash commands that let you trigger common prompts with a simple /command.
keywords: [devoxxgenie, commands, slash commands, custom prompts, intellij plugin, ai assistant]
image: /img/devoxxgenie-social-card.jpg
---

# Commands

Commands are reusable slash commands that you can type in the DevoxxGenie input field to trigger predefined prompts. They provide a fast way to perform common tasks like explaining code, generating tests, or reviewing changes.

:::note Renamed from "Custom Prompts" / "Skills"
This feature was previously called *Custom Prompts*, and briefly *Skills*. It is now called **Commands**.

The new [Skills](skills.md) feature is a separate capability — Skills are LLM-activated `SKILL.md` files loaded from disk, not user-typed slash commands.
:::

## How Commands Work

1. Type a slash command in the input field (e.g., `/explain`)
2. Optionally add arguments after the command
3. Press Enter — the command's prompt template is sent to the LLM

Commands support a `$ARGUMENT` placeholder that gets replaced with whatever text you type after the command. For example:

```
/test write unit tests for the UserService class
```

If the command's prompt template is:

```
$ARGUMENT
```

Then `$ARGUMENT` gets replaced with `write unit tests for the UserService class`.

Commands are a **pre-LLM text substitution macro** — DevoxxGenie expands the template locally before sending the resulting text to the model. This is fundamentally different from [Skills](skills.md), which are activated by the LLM mid-conversation via a tool call.

## Built-in Commands

DevoxxGenie ships with several built-in commands:

| Command | Description |
|---------|-------------|
| `/test` | Generate unit tests for the selected code |
| `/explain` | Explain the selected code |
| `/review` | Review the selected code and suggest improvements |
| `/find` | Search for code in your project using RAG |
| `/help` | Show available commands |
| `/init` | Generate a DEVOXXGENIE.md project description file |

### Using `/find` with RAG

The `/find` command requires RAG to be enabled and activated. When you type `/find authentication flow`, DevoxxGenie performs a semantic search across your indexed codebase and returns the most relevant code snippets.

If RAG is not configured, you'll see a notification explaining how to set it up.

### Using `/help`

The `/help` command displays a summary of all available commands directly in the output panel without sending anything to the LLM.

## Managing Commands

You can add, edit, and remove commands from the settings.

### Accessing Commands Settings

1. Open IntelliJ IDEA settings
2. Navigate to **Tools** > **DevoxxGenie** > **Commands**

### Adding a Command

1. Click the **+** (Add) button
2. Enter a command name (without the `/` prefix)
3. Enter the prompt template — use `$ARGUMENT` where you want user input inserted
4. Click **OK**

**Example**: Create a `/docstring` command:
- **Command**: `docstring`
- **Prompt**: `Write comprehensive JavaDoc for the following code: $ARGUMENT`

Usage: `/docstring` with code selected in the editor.

### Editing a Command

Double-click an existing command in the table to edit its command name or prompt template.

### Removing a Command

Select a command in the table and click the **-** (Remove) button.

### Restoring Defaults

Click the **Restore** button to reset the command list back to the built-in defaults. This removes any custom commands you've added.

## Commands vs. System Prompt

Commands and the system prompt serve different purposes:

- **System prompt**: Sets the overall context and behavior for the LLM across all conversations. Configured in **Settings** > **Prompts**.
- **Commands**: Individual slash commands that trigger specific prompt templates for one-off tasks. Configured in **Settings** > **Commands**.

## Commands vs. Skills

|                          | Commands                                | Skills                                       |
|--------------------------|-----------------------------------------|----------------------------------------------|
| **Invoked by**           | User types `/name args`                 | LLM calls `activate_skill(name)`             |
| **Timing**               | Pre-LLM, before the request is sent     | Mid-conversation, LLM-driven                 |
| **Requires Agent mode**  | No                                      | Yes                                          |
| **Storage**              | IDE Settings (XML)                      | `SKILL.md` files on disk                     |
| **Best for**             | Quick repeatable prompt templates       | Reusable agent capabilities and workflows    |

See the [Skills](skills.md) page for the new SKILL.md-based feature.

## Tips

- Keep command prompts focused on a single task for best results
- Use `$ARGUMENT` to make commands flexible — the same command can handle different inputs
- Create project-specific commands for patterns you use frequently (e.g., `/service` to generate a service class following your project's conventions)
- Combine commands with file context — select code in the editor before running a command to include it in the prompt

## Related Features

- [Skills](skills.md) — LLM-activated capabilities loaded from `SKILL.md` files
- [Agent Mode](agent-mode.md) — let the LLM use tools and skills autonomously
- [MCP Support](mcp_expanded.md) — extend capabilities with external tool servers
- [Chat Interface](chat-interface.md) — use commands directly in the chat panel
