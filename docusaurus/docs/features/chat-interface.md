---
sidebar_position: 2
title: Chat Interface - AI Code Assistant in IntelliJ IDEA
description: Use the DevoxxGenie chat interface to have code-related conversations with LLMs, add files to context, stream responses, and manage conversation history.
keywords: [devoxxgenie, chat interface, conversation, code assistant, streaming, context files, intellij plugin]
image: /img/devoxxgenie-social-card.jpg
---

# Chat Interface

The DevoxxGenie chat interface is the primary way you interact with LLM models. It provides a clean, intuitive interface designed specifically for code-related conversations.

## Chat Window Overview

![Chat Interface](/img/chat-interface.png)

The DevoxxGenie chat interface consists of several key components:

1. **LLM Provider Selector**: Choose between different LLM providers and models
2. **Feature Toggles**: Enable/disable Web Search and Agent Mode; RAG is enabled in settings
3. **Conversation History**: View and manage past conversations
4. **Chat Messages Area**: Display the ongoing conversation
5. **Context Files Panel**: View and manage files added to the context
6. **Input Area**: Enter your prompts and questions
7. **Action Buttons**: Access additional functionality

## Starting a Conversation

To start a new conversation:

1. Click on the DevoxxGenie icon in the right toolbar of IntelliJ IDEA
2. Select your preferred LLM provider and model from the dropdown
3. Type your question or prompt in the input area
4. Press Shift+Enter (the default submit shortcut) or click the Submit button

You can also use slash commands like `/explain` or `/review` followed by your query.

## Queue and steer a running task

The input remains usable while a request runs. The main submit button becomes **Stop**, with **Queue** and **Steer** controls alongside it.

| Action | Use it for | Behavior |
|---|---|---|
| **Queue** | A separate follow-up question or task | Runs after the current request finishes; queued prompts are processed one at a time. Submitting non-empty text with your configured submit shortcut while a request is active also queues it. |
| **Steer** | A correction to the current agent task | Injects the text at the next agent round trip when an active steering consumer is available. It does not interrupt a tool already executing. |
| **Stop** | Cancelling the current request | Discards pending queued prompts and unconsumed steering messages as well. It does not undo file edits or other side effects already performed. |

For example, queue “Now write tests” as an independent next task; steer with “Keep the existing public API” to correct the current task. Steering is not guaranteed to be consumed immediately or supported by every execution path; when it cannot be applied during the run, remaining text can be submitted as a follow-up after the run finishes.

## Chat Messages

The chat messages area displays the conversation between you and the LLM:

- **User Messages**: Your prompts and questions (displayed in blue bubbles)
- **AI Responses**: The LLM's responses (displayed in white/gray bubbles)
- **Code Blocks**: Code in responses is syntax-highlighted
- **Message Controls**: Options to copy or delete messages

### Code Highlighting

Code blocks in responses are automatically detected and syntax-highlighted based on the language. The highlighting follows your IDE theme for consistency.

### Message Actions

Each message has several actions available:

- **Copy Message**: Copy the entire message to clipboard
- **Copy Code**: Copy just the code blocks from the message
- **Delete Message**: Remove the message from the conversation
- **Edit Message**: (User messages only) Edit and resubmit your prompt

## Streaming Responses

DevoxxGenie supports real-time streaming of responses:

1. As you submit a prompt, you'll see a "Thinking..." indicator
2. The response will appear token by token as it's generated
3. You can stop the generation at any point by clicking the Stop button

The streaming feature can be toggled in the settings if you prefer to receive complete responses.

## Context Files

The context files panel shows files that are included in the conversation context:

1. **Editor File**: The currently open editor file is included when **Use file in editor** is enabled in LLM Settings
2. **Added Files**: Files you've explicitly added to the context
3. **File Actions**: Remove files or copy their content

Files you explicitly attach remain in the active chat tab's context across prompts until you remove them; they are not cleared after each submission. The current editor selection is included on the same turn as focus context, rather than waiting until a later prompt.

### Adding Files to Context

There are several ways to add files to the context:

1. **Right-click menu**: Right-click on a file in the project view and select "Add To Prompt Context"
2. **Code selection**: Select code in the editor, right-click, and choose "Add To Prompt Context"
3. **Drag and drop**: Drag files from the project view into the DevoxxGenie window

### Context Size Indicator

A progress bar shows how much of the available context window is being used by your files and conversation.

## Conversation History

DevoxxGenie saves your conversations for future reference:

1. Click on the "History" button to view past conversations
2. Select a conversation to resume it
3. Use the search function to find specific conversations
4. Delete conversations you no longer need

Conversations are saved per project, so you'll have different conversation histories for different projects.

## Feature Toggles

The feature toggle buttons control **Web Search** and **Agent Mode**.

[RAG](rag.md) is controlled by **Settings → Tools → DevoxxGenie → RAG**, not a separate chat activation toggle. With RAG enabled, non-agent prompts receive retrieved context automatically; in Agent Mode the model retrieves through `semantic_search` instead.

## LLM Provider Selection

The provider dropdown lets you switch between different LLM providers and models:

1. Select a provider (e.g., Ollama, OpenAI, Anthropic)
2. Choose a specific model from that provider
3. View context window size and cost information for each model

Your selection is persisted, so DevoxxGenie will remember your preference. Provider and model dropdowns hide when they contain only one entry.

### Favorite models

Click the **star** next to the selected model to add or remove a favorite. The **Favorites** entry in the provider selector groups favorite models from all providers; selecting one switches both provider and model.

Favorites survive IDE restarts. A favorite that is unavailable remains listed but disabled. To remove saved entries, open the main **Settings → Tools → DevoxxGenie** page and use **Favorite models → Remove selected favorite**, then Apply.

### Personas

Enable **Show personas** under **Settings → Tools → DevoxxGenie → System Prompt & Personas** to add a persona dropdown to the chat window. The selected persona replaces the base system prompt for new conversations; existing conversations retain their original persona. See [Personas configuration](../configuration/prompts.md#personas) for built-in roles, custom prompts, and per-tab behavior.

### Show Thinking

Open the main **Settings → Tools → DevoxxGenie** page and enable **Show Thinking** in **Large Language Model Response**. It is off by default.

When a supported model/provider returns reasoning content, DevoxxGenie displays it separately from the final answer. Support depends on the provider and model; enabling this option does not force every model to generate or expose reasoning. The waiting/streaming indicator is not the same as returned reasoning content.

## Input Area

The input area is where you type your prompts:

1. **Multi-line support**: By default, use Ctrl+Enter on Windows/Linux or Command+Enter on macOS for a new line; configure the newline and submit shortcuts under **System Prompt & Personas**
2. **Command auto-completion**: Suggestions for slash commands
3. **Image attachment**: Drag and drop images for multimodal models
4. **Submit button**: Send your prompt to the LLM
5. **Character counter**: Shows the length of your prompt

## Customizing the Interface

Several aspects of the chat interface can be customized in settings:

1. **Theme integration**: The interface follows your IDE theme
2. **Font size**: Adjusts with your IDE font settings
3. **Splitter positions**: Resize different panels to your preference
4. **Behavior options**: Configure auto-scrolling, prompt clearing, etc.

## Keyboard Shortcuts

DevoxxGenie supports several keyboard shortcuts:

- **Shift+Enter**: Submit prompt (configurable)
- **Escape**: Cancel ongoing request
- **Ctrl/Cmd+C**: Copy selected text
- **Ctrl/Cmd+V**: Paste text
- **Up/Down Arrows**: Navigate through prompt history

## Chat Memory

DevoxxGenie maintains a conversation memory to provide context for the LLM:

1. Previous messages are included in the context for each new prompt
2. The memory size is configurable in settings
3. The memory is cleared when starting a new conversation

## Best Practices for Chat Interface

1. **Be specific**: Clearly state what you want the LLM to do
2. **Provide context**: Include relevant code or explain your use case
3. **Configure retrieval**: Enable RAG in settings for project-specific questions
4. **Manage context**: Remove unnecessary files to save context window space
5. **Use slash commands**: Leverage built-in commands for common tasks
