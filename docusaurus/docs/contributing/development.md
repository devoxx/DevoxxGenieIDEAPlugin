---
sidebar_position: 3
title: Development Guide
description: Developer guide for contributing to DevoxxGenie, covering project architecture, build setup, module structure, and debugging the IntelliJ plugin.
keywords: [devoxxgenie, development, contributing, intellij plugin development, java, architecture, build, gradle]
image: /img/devoxxgenie-social-card.jpg
---

# Development Guide

This guide provides detailed information for developers who want to contribute to the DevoxxGenie plugin's codebase.

## Project Architecture

DevoxxGenie follows a modular architecture with clear separation of concerns:

```
src/main/java/com/devoxx/genie/
├── action/         # Action classes for menu actions
├── chatmodel/      # LLM provider implementations
│   ├── cloud/      # Cloud LLM providers (OpenAI, Anthropic, etc.)
│   └── local/      # Local LLM providers (Ollama, GPT4All, etc.)
├── controller/     # Controllers for coordinating behavior
├── error/          # Error handling mechanisms
├── model/          # Data models and DTOs
├── service/        # Business logic services
│   ├── chromadb/   # ChromaDB integration for RAG
│   ├── mcp/        # MCP implementation
│   ├── prompt/     # Prompt handling services
│   ├── rag/        # RAG implementation
│   └── websearch/  # Web search functionality
└── ui/             # User interface components
    ├── component/  # Reusable UI components
    ├── dialog/     # Dialog windows
    ├── panel/      # UI panels
    ├── renderer/   # Custom UI renderers
    ├── settings/   # Settings UI
    └── util/       # UI utilities
```

## Key Components

### LLM Provider Integration

The `chatmodel` package contains implementations for different LLM providers. To add a new provider:

1. Create a new package under `chatmodel/cloud/` or `chatmodel/local/`
2. Implement a factory class that extends `ChatModelFactory`
3. Register the factory in `ChatModelFactoryProvider`
4. Add UI components to configure the provider in settings

Example factory class:

```java
public class NewProviderChatModelFactory implements ChatModelFactory {
    @Override
    public ChatLanguageModel createChatModel(ChatModel model) {
        // Implementation...
    }
    
    @Override
    public List<ChatModel> getModels() {
        // Return available models...
    }
}
```

### Prompt Execution

The prompt execution flow is managed through:

1. **PromptExecutionController**: Entry point for prompt execution
2. **PromptExecutionService**: Orchestrates the execution process
3. **ChatPromptExecutor**: Handles the actual interaction with the LLM
4. **Strategy classes**: Different execution strategies based on requirements

See [Prompt Structure](prompt-structure.md) for more details on the prompt architecture.

### User Interface

The UI is built using IntelliJ's Swing-based component system. Key UI components include:

- **DevoxxGenieToolWindowFactory**: Creates the main tool window
- **DevoxxGenieToolWindowContent**: Main content for the tool window
- **ChatResponsePanel**: Displays LLM responses
- **UserPromptPanel**: Handles user input
- **Settings UI**: Various configurable components

## Build System

DevoxxGenie uses Gradle with the IntelliJ Plugin Gradle plugin:

```groovy
plugins {
    id 'org.jetbrains.intellij' version '1.13.3'
    id 'java'
}
```

Important Gradle tasks:

- `./gradlew build`: Builds the project
- `./gradlew test`: Runs tests
- `./gradlew buildPlugin`: Creates a deployable plugin ZIP
- `./gradlew runIde`: Launches a development instance of IntelliJ with the plugin

## Testing

### Unit Testing

Unit tests are in the `src/test` directory, mirroring the structure of `src/main`. We use JUnit 5 and Mockito for testing.

Example test class:

```java
@ExtendWith(MockitoExtension.class)
public class ChatPromptExecutorTest {
    
    @Mock
    private ChatLanguageModel chatLanguageModel;
    
    @InjectMocks
    private ChatPromptExecutor executor;
    
    @Test
    public void testExecutePrompt() {
        // Test implementation...
    }
}
```

### Integration Testing

Integration tests with the `IT` suffix validate integration between components. These may require additional setup:

```java
public class PromptExecutionServiceIT extends AbstractLightPlatformTestCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Setup code...
    }
    
    public void testEndToEndPromptExecution() {
        // Test implementation...
    }
}
```

## Debugging

### Plugin Debugging

To debug the plugin:

1. Run `./gradlew runIde --debug-jvm`
2. Connect your IDE's remote debugger to port 5005
3. Set breakpoints in your code

### LLM Integration Debugging

For debugging LLM provider integration:

1. Enable debug logging in `logback.xml`
2. Add appropriate logging in your code
3. Check the IDE's log files for detailed information

Example logging configuration:

```xml
<logger name="com.devoxx.genie.chatmodel" level="DEBUG"/>
```

## Common Development Tasks

### Adding a New Feature

1. **Identify the Components**: Determine which parts of the codebase need modification
2. **Design the Feature**: Plan the implementation, considering architecture and user experience
3. **Implement Backend**: Add necessary services, models, and controllers
4. **Implement UI**: Add or modify UI components
5. **Add Tests**: Write unit and integration tests
6. **Document**: Update documentation

### Fixing a Bug

1. **Reproduce the Bug**: Understand the steps to consistently reproduce
2. **Investigate**: Find the root cause in the code
3. **Fix**: Make the minimal necessary changes to fix the issue
4. **Test**: Add regression tests to prevent recurrence
5. **Document**: Add comments explaining the fix if necessary

### Working with Dependencies

DevoxxGenie uses several key dependencies:

- **Langchain4j**: For interacting with LLM providers
- **LLM Provider Libraries**: OpenAI, Anthropic, etc.
- **IntelliJ Platform SDK**: For IDE integration

When adding or updating dependencies, make sure to:

1. Use appropriate version constraints
2. Consider compatibility with different IDE versions
3. Be mindful of transitive dependencies

## Best Practices

1. **Follow IDEA Patterns**: Align with IntelliJ platform patterns and conventions
2. **Error Handling**: Use appropriate error handling for LLM interactions
3. **Thread Safety**: Be mindful of threading in UI and background operations
4. **Resource Management**: Properly manage resources like API connections
5. **User Experience**: Focus on making the plugin intuitive and responsive
6. **Performance**: Consider token usage, latency, and memory footprint

## Common Issues and Solutions

### Plugin Loading Issues

If your plugin isn't loading correctly:

1. Check the IDE log for errors
2. Verify plugin.xml configuration
3. Ensure IntelliJ version compatibility

### LLM Provider Connection Problems

If LLM providers aren't connecting:

1. Verify API keys and endpoints
2. Check network connectivity
3. Look for rate limiting or permission issues

### UI Rendering Issues

For UI problems:

1. Use IntelliJ UI Inspector to debug component hierarchy
2. Check threading (UI updates must be on EDT)
3. Verify component initialization order

## Additional Resources

- [IntelliJ Platform UI Guidelines](https://jetbrains.github.io/ui/)
- [Langchain4j Documentation](https://docs.langchain4j.dev/)
- [Plugin Development Forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development)

By following this guide, you'll be well-equipped to contribute effectively to the DevoxxGenie plugin.
