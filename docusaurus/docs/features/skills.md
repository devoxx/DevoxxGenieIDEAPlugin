---
sidebar_position: 4
title: Skills - Custom Slash Commands
description: Learn how to use and create Skills in DevoxxGenie — reusable slash commands that let you trigger common prompts with a simple /command.
keywords: [devoxxgenie, skills, slash commands, custom prompts, intellij plugin, ai assistant]
image: /img/devoxxgenie-social-card.jpg
---

# Skills

Skills are reusable slash commands that you can type in the DevoxxGenie input field to trigger predefined prompts. They provide a fast way to perform common tasks like explaining code, generating tests, or reviewing changes.

## How Skills Work

1. Type a slash command in the input field (e.g., `/explain`)
2. Optionally add arguments after the command
3. Press Enter — the skill's prompt template is sent to the LLM

Skills support a `$ARGUMENT` placeholder that gets replaced with whatever text you type after the command. For example:

```
/test write unit tests for the UserService class
```

If the skill's prompt template is:

```
$ARGUMENT
```

Then `$ARGUMENT` gets replaced with `write unit tests for the UserService class`.

## Built-in Skills

DevoxxGenie ships with several built-in skills:

| Command | Description |
|---------|-------------|
| `/test` | Generate unit tests for the selected code |
| `/explain` | Explain the selected code |
| `/review` | Review the selected code and suggest improvements |
| `/tdg` | Test-Driven Generation — generate implementation from tests |
| `/find` | Search for code in your project using RAG |
| `/help` | Show available commands |
| `/init` | Generate a DEVOXXGENIE.md project description file |

### Using `/find` with RAG

The `/find` command requires RAG to be enabled and activated. When you type `/find authentication flow`, DevoxxGenie performs a semantic search across your indexed codebase and returns the most relevant code snippets.

If RAG is not configured, you'll see a notification explaining how to set it up.

### Using `/help`

The `/help` command displays a summary of all available commands directly in the output panel without sending anything to the LLM.

## Managing Skills

You can add, edit, and remove skills from the settings.

### Accessing Skill Settings

1. Open IntelliJ IDEA settings
2. Navigate to **Tools** > **DevoxxGenie** > **Skills**

### Adding a Skill

1. Click the **+** (Add) button
2. Enter a command name (without the `/` prefix)
3. Enter the prompt template — use `$ARGUMENT` where you want user input inserted
4. Click **OK**

**Example**: Create a `/docstring` skill:
- **Command**: `docstring`
- **Prompt**: `Write comprehensive JavaDoc for the following code: $ARGUMENT`

Usage: `/docstring` with code selected in the editor.

### Editing a Skill

Double-click an existing skill in the table to edit its command name or prompt template.

### Removing a Skill

Select a skill in the table and click the **-** (Remove) button.

### Restoring Defaults

Click the **Restore** button to reset the skill list back to the built-in defaults. This removes any custom skills you've added.

## Skills vs. System Prompt

Skills and the system prompt serve different purposes:

- **System prompt**: Sets the overall context and behavior for the LLM across all conversations. Configured in **Settings** > **Prompts**.
- **Skills**: Individual slash commands that trigger specific prompt templates for one-off tasks. Configured in **Settings** > **Skills**.

## Tips

- Keep skill prompts focused on a single task for best results
- Use `$ARGUMENT` to make skills flexible — the same skill can handle different inputs
- Create project-specific skills for patterns you use frequently (e.g., `/service` to generate a service class following your project's conventions)
- Combine skills with file context — select code in the editor before running a skill to include it in the prompt

## Related Features

- [Agent Mode](agent-mode.md) — let the LLM use skills autonomously via tool calls
- [MCP Support](mcp_expanded.md) — extend capabilities with external tool servers
- [Chat Interface](chat-interface.md) — use skills directly in the chat panel
