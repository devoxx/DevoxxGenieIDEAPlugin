---
sidebar_position: 8
---

# Chat Memory

DevoxxGenie's chat memory system allows the LLM to maintain context throughout a conversation, providing more coherent and contextually relevant responses.

## How Chat Memory Works

When you interact with an LLM through DevoxxGenie, each exchange is stored in a conversation memory:

1. **System Message**: The initial instructions for the LLM (includes DEVOXXGENIE.md content)
2. **User Messages**: Your prompts and questions
3. **AI Responses**: The LLM's replies

For each new query, DevoxxGenie sends a portion of this conversation history to the LLM as context, allowing it to understand the ongoing conversation and provide consistent responses.

## Configuring Chat Memory

You can configure the chat memory in DevoxxGenie settings:

1. Go to Settings → Tools → DevoxxGenie → Prompts
2. Locate the "Chat Memory Size" setting
3. Set your preferred memory size (default is 10 messages)

![Chat Memory Settings](/img/chat-memory-settings.png)

### Memory Size Considerations

The optimal memory size depends on your use case:

- **Smaller memory** (5-10 messages): More focused responses, less token usage
- **Larger memory** (15-20 messages): Better for complex, multi-step conversations
- **No memory** (0 messages): Each prompt is treated independently (stateless)

Remember that larger memory sizes consume more tokens, which may increase costs when using cloud LLM providers.

## Memory Management

DevoxxGenie provides several ways to manage chat memory:

### Starting a New Conversation

To clear the memory and start fresh:

1. Click the "New Conversation" button in the DevoxxGenie toolbar
2. Or select "New Conversation" from the conversation history dropdown

### Removing Specific Messages

To selectively remove messages from memory:

1. Hover over a message in the chat interface
2. Click the delete (trash) icon
3. Confirm removal when prompted

Removing messages helps to:
- Clean up irrelevant context
- Remove incorrect information
- Focus the conversation on specific topics

### Conversation History

DevoxxGenie stores conversations separately from the active memory:

1. **Auto-save**: Conversations are automatically saved as you chat
2. **Restore**: Reload past conversations from the history dropdown
3. **Search**: Find specific conversations by content or date
4. **Delete**: Remove old conversations you no longer need

Conversation history is stored locally on your machine and does not count against the memory size limit.

## Chat Memory and Context Window

Chat memory uses tokens from the LLM's context window:

1. Each message in memory consumes tokens
2. The token usage bar shows how much of the context window is being used
3. If you approach the context window limit, consider:
   - Reducing memory size
   - Starting a new conversation
   - Removing unnecessary messages
   - Switching to a model with a larger context window

## Project-Specific Memory

DevoxxGenie maintains separate chat memory for each project:

1. Each project has its own conversation history
2. Switching projects automatically switches to the appropriate memory
3. Project-specific system prompts are maintained

This project isolation helps keep conversations focused on the relevant codebase.

## Memory Behavior with Different LLM Providers

Chat memory works with all supported LLM providers, but there are some considerations:

- **Cloud providers** (OpenAI, Anthropic, etc.): Full support for all memory features
- **Local providers** (Ollama, GPT4All, etc.): Memory support depends on the specific implementation
- **Context window limits**: Varies by provider and model

## Advanced Memory Features

### System Message Persistence

The system message persists across all conversations, providing consistent instructions to the LLM. This includes:

1. Your custom system prompt
2. DEVOXXGENIE.md content (if available)
3. Current project structure information

### Memory Visualization

The chat interface visually represents which messages are in active memory:

1. Messages in memory have a different background shade
2. The oldest messages (about to be dropped from memory) are slightly faded
3. Deleted messages are immediately removed from view

## Best Practices for Chat Memory

To get the most out of DevoxxGenie's chat memory:

1. **Start with clarity**: Begin conversations with clear, specific questions
2. **Remove noise**: Delete irrelevant or erroneous messages
3. **Start fresh when needed**: Don't hesitate to start a new conversation for new topics
4. **Balance memory size**: Use larger memory for complex tasks, smaller for simple ones
5. **Be aware of context**: Remember that files added to context also consume tokens
6. **Optimize for token usage**: With cloud providers, manage memory to control costs

## Troubleshooting Memory Issues

If you notice issues with chat memory:

- **Inconsistent responses**: The LLM may be reaching context limits; reduce memory size
- **Slow performance**: Large memory may cause slower responses; consider reducing size
- **Missing context**: If the LLM forgets earlier conversation, increase memory size
- **Memory not clearing**: Restart the conversation or restart IntelliJ IDEA

## Future Enhancements

The DevoxxGenie team is working on several memory enhancements:

1. **Selective memory**: Intelligently decide which messages to keep in context
2. **Memory compression**: Reduce token usage while maintaining context
3. **Memory search**: Search through past messages more effectively
4. **Memory visualization**: Better visualization of active memory
