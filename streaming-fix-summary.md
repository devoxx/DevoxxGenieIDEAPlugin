# Streaming Response Fix Summary

## Issues Identified

Two main issues were identified with the streaming response implementation:

1. **Message Duplication**: For each streaming response chunk, a completely new message pair (user + AI) was being created in the conversation UI, resulting in multiple duplicated user queries.

2. **Response Overwriting**: Each partial response was completely replacing the previous response content rather than building on it, which made it look like the text was being overwritten rather than growing.

## Solution Implemented

### 1. Created New Update Method in ConversationWebViewController

Added a new `updateAiMessageContent` method that allows updating just the AI response part of an existing message pair:

```java
public void updateAiMessageContent(ChatMessageContext chatMessageContext) {
    // Method to update just the AI content part of an existing message pair
    // without creating a whole new message pair
}
```

This method finds the existing message pair by ID and only updates the content of the assistant-message div.

### 2. Modified StreamingResponseHandler Approach

- **Initial Message Setup**: The handler adds only the user query with empty AI response in the constructor
- **First Response**: We track the first response with a flag to avoid duplicate messages 
- **Partial Responses**: For subsequent responses, we only update the AI content part of the existing message

```java
@Override
public void onPartialResponse(String partialResponse) {
    if (!isStopped) {
        // The LLM API sends the full text each time (not just new tokens)
        ApplicationManager.getApplication().invokeLater(() -> {
            // Update the AI response in the context
            context.setAiMessage(dev.langchain4j.data.message.AiMessage.from(partialResponse));

            // Update just the AI content part in the conversation view
            conversationWebViewController.updateAiMessageContent(context);
            
            // Mark first response as handled
            isFirstResponse = false;
        });
    }
}
```

### 3. Proper Completion Handling

Modified the completion handler to use the same approach:

```java
if (!isFirstResponse) {
    // If we've already shown partial responses, just update AI content
    conversationWebViewController.updateAiMessageContent(context);
} else {
    // If no partial responses were shown yet, add a full message pair
    conversationWebViewController.addChatMessage(context);
}
```

## How This Works

1. When streaming starts:
   - Add user message with empty AI response to create the "shell" for streaming
   
2. For each partial response:
   - Update just the AI part of the existing message (via the message ID)
   - This preserves the single user query while updating the response text
   
3. When streaming completes:
   - Add execution time metrics 
   - Update with final response content
   - Add to message bus for conversation history

The key insight was understanding that the LLM API sends the full response text with each update, not just the new tokens.

## Testing

To test this fix:
1. Run the plugin with streaming mode enabled
2. Submit a prompt that will generate a streaming response
3. Verify that:
   - Only one user message appears
   - The AI response appears to build continuously 
   - The response metrics update at completion
