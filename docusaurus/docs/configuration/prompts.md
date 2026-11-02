---
sidebar_position: 2
title: System Prompt and Personas Configuration
description: Configure the system prompt, keyboard shortcuts, and DEVOXXGENIE.md settings in DevoxxGenie to customize how the LLM responds to your queries.
keywords: [devoxxgenie, prompts, system prompt, keyboard shortcuts, configuration, intellij plugin, llm settings]
image: /img/devoxxgenie-social-card.jpg
---

# System Prompt and Personas Configuration

DevoxxGenie allows you to configure the system prompt, keyboard shortcuts, and project description file. Slash commands (like `/test`, `/explain`) are now managed separately under **Commands** — see the [Commands](../features/commands.md) page.

## Accessing Prompt Settings

1. Open IntelliJ IDEA settings
2. Navigate to `Tools` > `DevoxxGenie` > `System Prompt & Personas`

## System Prompt

The system prompt sets the initial context and instructions for the LLM. You can customize it to better fit your use case.

### Editing the System Prompt

1. In the System Prompt & Personas settings, locate the "System Prompt" text area
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

## Personas

Personas are named system prompts you can select in the chat window. Enable **Show personas** under **Settings → Tools → DevoxxGenie → System Prompt & Personas** to display the selector; it is hidden by default.

The built-in personas are **Developer** (the default), **Reviewer**, **Architect**, and **Test Engineer**. Use **Add**, **Edit**, or **Remove** to customize the list, **Restore** to restore the built-in personas, and **Default persona** to choose the initial selection. Apply your changes to save them.

- A persona **replaces**, rather than appends to, the base system prompt. Project instruction files and other context are still added separately.
- Selection is per chat tab. Choose a persona before starting a new conversation: an ongoing conversation keeps the persona it started with.
- Tab selections are runtime-only. After an IDE restart, tabs start with the configured default persona again.
- Turn off **Show personas** to use the normal system prompt without persona selection.

See [Chat Interface](../features/chat-interface.md) for the model selector and conversation controls.

## Keyboard Shortcuts

You can configure keyboard shortcuts for submitting prompts.

### Setting Up Shortcuts

1. In the System Prompt & Personas settings, locate the "Keyboard Shortcuts" section
2. Define your preferred keystroke combinations
3. Click `Apply` to save

### Default Shortcuts

- **Submit Prompt**: Shift+Enter
- **Cancel Execution**: Escape

## DEVOXXGENIE.md File

From the System Prompt & Personas settings, you can generate a DEVOXXGENIE.md file for your project. This file is automatically included in the system prompt and gives the LLM context about your project.

### Generating DEVOXXGENIE.md

1. Click the "Generate DEVOXXGENIE.md" button
2. The file will be created in your project root directory

You can also generate this file by typing `/init` in the DevoxxGenie input field.

For more information about DEVOXXGENIE.md, see the [DEVOXXGENIE.md Configuration](devoxxgenie-md.md) page.

## Commands (Slash Commands)

Slash commands like `/test`, `/explain`, `/review`, and custom commands are now configured in a dedicated **Commands** settings page.

To manage commands, go to **Settings** > **Tools** > **DevoxxGenie** > **Commands**.

See the [Commands](../features/commands.md) page for full documentation on creating and using commands.

## Skills (LLM-Activated SKILL.md Files)

Skills are a separate, newer feature: self-describing capabilities loaded from `SKILL.md` files on disk and activated by the LLM itself when Agent Mode is enabled.

To manage skills, go to **Settings** > **Tools** > **DevoxxGenie** > **Skills**.

See the [Skills](../features/skills.md) page for full documentation.
