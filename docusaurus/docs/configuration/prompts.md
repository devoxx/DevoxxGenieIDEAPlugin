---
sidebar_position: 2
---

# Prompts Configuration

DevoxxGenie allows you to configure custom prompts and command shortcuts to streamline your AI interactions. This page explains how to set up and use custom prompts effectively.

## Accessing Prompt Settings

1. Open IntelliJ IDEA settings
2. Navigate to `Tools` → `DevoxxGenie` → `Prompts`

![Prompts Settings](/img/prompts-settings.png)

## Custom Commands

Custom commands allow you to define shortcuts that expand into full prompts when typed in the DevoxxGenie input field.

### Default Custom Commands

DevoxxGenie comes with several built-in command shortcuts:

| Command | Description |
|---------|-------------|
| `/explain` | Explain the selected code |
| `/review` | Review the selected code and suggest improvements |
| `/refactor` | Suggest refactoring options for the selected code |
| `/unittest` | Generate unit tests for the selected code |
| `/find` | Search for code in your project (used with RAG) |
| `/help` | Show available commands |

### Creating Custom Commands

To create a new custom command:

1. In the Prompts settings, click `Add` in the Custom Commands section
2. Enter a command name (starting with `/`)
3. Enter the prompt template that will be used when the command is triggered
4. Click `OK` to save

For example:
- Command: `/docstring`
- Prompt: `Write comprehensive JavaDoc for the following code:\n\n{code}`

### Using Variables in Custom Commands

You can use special placeholders in your custom commands:

- `{code}`: Will be replaced with the currently selected code
- `{className}`: Will be replaced with the current class name
- `{methodName}`: Will be replaced with the current method name
- `{package}`: Will be replaced with the current package name

### Using Custom Commands

To use a custom command:

1. Select code in the editor (optional, depending on the command)
2. Open the DevoxxGenie window
3. Type the command (e.g., `/explain`) in the input field
4. Press Enter to execute

You can also add additional text after a command to provide more context, for example:
```
/explain and focus on the concurrency aspects
```

## System Prompt

The system prompt sets the initial context and instructions for the LLM. You can customize it to better fit your use case.

### Editing the System Prompt

1. In the Prompts settings, locate the "System Prompt" text area
2. Modify the text to include specific instructions, context, or preferences
3. Click `Apply` to save changes

### System Prompt Best Practices

- Be clear and specific about what you want the LLM to do
- Include preferred coding styles or standards
- Mention the programming languages you use most often
- Specify how detailed you want explanations to be

Example system prompt:
```
You are an AI programming assistant for Java development. You help with coding tasks, provide explanations, and suggest improvements. Follow these guidelines:
1. Be concise and use examples
2. Follow Google Java Style Guide
3. Provide explanations that include rationale
4. When showing code, include comments for complex logic
5. Prefer modern Java patterns and APIs (Java 11+)
```

## Keyboard Shortcuts

You can configure keyboard shortcuts for submitting prompts and custom commands.

### Setting Up Shortcuts

1. In the Prompts settings, locate the "Keyboard Shortcuts" section
2. Define your preferred keystroke combinations
3. Click `Apply` to save

### Default Shortcuts

- **Submit Prompt**: Shift+Enter
- **Cancel Execution**: Escape

## DEVOXXGENIE.md File

From the Prompts settings, you can generate a DEVOXXGENIE.md file for your project.

### Generating DEVOXXGENIE.md

1. Click the "Generate DEVOXXGENIE.md" button
2. The file will be created in your project root directory

For more information about DEVOXXGENIE.md, see the [DEVOXXGENIE.md Configuration](devoxxgenie-md.md) page.

## Prompt Input Options

Additional settings to customize prompt input behavior:

- **Auto-complete Commands**: Enable/disable command auto-completion in the input field
- **Clear After Submit**: Automatically clear the input field after submitting a prompt
- **Remember History**: Keep track of your recent prompts for easy reuse

## Troubleshooting Custom Commands

If your custom commands aren't working as expected:

- **Command Not Recognized**: Ensure the command starts with `/` and is exactly as defined in settings
- **Variables Not Expanding**: Check that you're using the correct variable names (`{code}`, etc.)
- **Command Too Generic**: Make your prompt templates more specific to get better results
- **Command Too Complex**: Break down complex tasks into simpler commands

## Command Usage Tips

- **Chain Commands**: Create a workflow of commands for complex tasks
- **Specialized Commands**: Create commands for specific frameworks or libraries you use
- **Documentation Commands**: Create commands focused on generating documentation
- **Testing Commands**: Create commands for generating different types of tests

By configuring custom prompts effectively, you can significantly streamline your development workflow and get more consistent results from DevoxxGenie.
