---
sidebar_position: 2
---

# Chat Interface

The DevoxxGenie chat interface is the primary way you interact with LLM models. It provides a clean, intuitive interface designed specifically for code-related conversations.

## Chat Window Overview

![Chat Interface](/img/chat-interface.png)

The DevoxxGenie chat interface consists of several key components:

1. **LLM Provider Selector**: Choose between different LLM providers and models
2. **Feature Toggles**: Enable/disable features like RAG, Web Search, and Git Diff
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
4. Press Enter or click the Submit button

You can also use slash commands like `/explain` or `/review` followed by your query.

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

1. **Default File**: The currently open editor file is automatically included
2. **Added Files**: Files you've explicitly added to the context
3. **File Actions**: Remove files or copy their content

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

The feature toggle buttons allow you to enable or disable key features:

1. **RAG**: Retrieval-Augmented Generation for context-aware responses
2. **Web Search**: Incorporate web search results in responses
3. **Git Diff**: Enable Git diff view for code changes

These toggles allow you to customize how DevoxxGenie behaves for different types of questions.

## LLM Provider Selection

The provider dropdown lets you switch between different LLM providers and models:

1. Select a provider (e.g., Ollama, OpenAI, Anthropic)
2. Choose a specific model from that provider
3. View context window size and cost information for each model

Your selection is persisted, so DevoxxGenie will remember your preference.

## Input Area

The input area is where you type your prompts:

1. **Multi-line support**: Press Shift+Enter for new lines
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
3. **Use appropriate toggles**: Enable RAG for project-specific questions
4. **Manage context**: Remove unnecessary files to save context window space
5. **Use slash commands**: Leverage built-in commands for common tasks
