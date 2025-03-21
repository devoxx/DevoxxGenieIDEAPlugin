# Standardized Error Handling for Prompt Service

This package provides a standardized error handling approach for the prompt service, with the goal of making error handling more consistent, maintainable, and user-friendly across the application.

## Design Goals

1. **Consistent Error Handling**: All prompt-related errors follow the same patterns.
2. **Appropriate Error Categorization**: Each type of error has its own exception class.
3. **Controlled Error Visibility**: Errors are only shown to users when appropriate.
4. **Centralized Recovery Patterns**: Common recovery actions are standardized.
5. **Better Context Preservation**: Errors carry enough context for logging and recovery.

## Exception Hierarchy

- **`PromptException`**: Base exception for all prompt-related errors
  - **`ModelException`**: Errors related to LLM models and providers
  - **`ExecutionException`**: Errors during prompt execution
  - **`StreamingException`**: Errors specific to streaming responses
  - **`WebSearchException`**: Errors during web searches
  - **`MemoryException`**: Errors related to chat memory management

## Usage Guidelines

### Throwing Exceptions

When an error occurs, throw the most specific exception type:

```java
try {
    // Some operation
} catch (Exception e) {
    throw new ModelException("Failed to initialize model", e);
}
```

### Handling Exceptions

Always use the `PromptErrorHandler` to handle exceptions:

```java
try {
    // Some operation
} catch (Exception e) {
    PromptErrorHandler.handleException(project, e, chatMessageContext);
}
```

### Error Severity

Each exception has a severity level:
- `INFO`: Informational, non-critical errors
- `WARNING`: Warnings that might affect functionality
- `ERROR`: Serious errors that prevent normal operation
- `CRITICAL`: Critical errors that require immediate attention

### User Visibility

Each exception specifies whether it should be visible to users. The `PromptErrorHandler` will automatically show notifications for user-visible errors.

## Migration From Old Error Handling

1. Replace direct calls to `ErrorHandler.handleError()` with `PromptErrorHandler.handleException()`
2. Replace generic exceptions with specific ones from this package
3. Replace direct logging with throwing appropriate exceptions
4. Use the severity levels to ensure proper error prioritization

## Best Practices

1. Always include the cause exception when wrapping another exception
2. Make error messages clear and actionable
3. Only mark errors as user-visible when the user can take action
4. Provide context information when handling exceptions
