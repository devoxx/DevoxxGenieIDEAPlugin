---
sidebar_position: 2
---

# Prompts Configuration

DevoxxGenie allows you to configure the system prompt, keyboard shortcuts, and project description file. Slash commands (like `/test`, `/explain`) are now managed separately under **Skills** — see the [Skills](../features/skills.md) page.

## Accessing Prompt Settings

1. Open IntelliJ IDEA settings
2. Navigate to `Tools` > `DevoxxGenie` > `Prompts`

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

You can configure keyboard shortcuts for submitting prompts.

### Setting Up Shortcuts

1. In the Prompts settings, locate the "Keyboard Shortcuts" section
2. Define your preferred keystroke combinations
3. Click `Apply` to save

### Default Shortcuts

- **Submit Prompt**: Shift+Enter
- **Cancel Execution**: Escape

## DEVOXXGENIE.md File

From the Prompts settings, you can generate a DEVOXXGENIE.md file for your project. This file is automatically included in the system prompt and gives the LLM context about your project.

### Generating DEVOXXGENIE.md

1. Click the "Generate DEVOXXGENIE.md" button
2. The file will be created in your project root directory

You can also generate this file by typing `/init` in the DevoxxGenie input field.

For more information about DEVOXXGENIE.md, see the [DEVOXXGENIE.md Configuration](devoxxgenie-md.md) page.

## Skills (Slash Commands)

Slash commands like `/test`, `/explain`, `/review`, and custom commands are now configured in a dedicated **Skills** settings page.

To manage skills, go to **Settings** > **Tools** > **DevoxxGenie** > **Skills**.

See the [Skills](../features/skills.md) page for full documentation on creating and using skills.
