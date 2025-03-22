# Prompt Structure Refactoring Guide

This document describes the refactoring applied to the prompt structure in the DevoxxGenie plugin to improve maintainability and extensibility.

## Overview of Changes

The prompt structure has been refactored to use cleaner design patterns and improve separation of concerns:

1. **Strategy Pattern** for different execution approaches
2. **Command Pattern** for prompt command handling
3. **Factory Pattern** for strategy selection
4. **Unified Memory Management**
5. **Centralized Prompt Service**

## New Components

### Core Services
- `PromptExecutionService` - Main service that orchestrates prompt execution
- `ChatMemoryManager` - Unified memory management
- `PromptCommandProcessor` - Central command processing

### Strategies
- `PromptExecutionStrategy` (interface)
- `NonStreamingPromptStrategy` - For regular, non-streaming responses
- `StreamingPromptStrategy` - For streaming responses
- `WebSearchPromptStrategy` - For web search based responses

### Commands
- `PromptCommand` (interface)
- `FindCommand` - Handles /find command
- `HelpCommand` - Handles /help command
- `CustomPromptCommand` - Handles user-defined commands

### Factory and Adapters
- `PromptExecutionStrategyFactory` - Creates appropriate strategies
- `LegacyPromptExecutorAdapter` - Adapter for backward compatibility

## Migration

The migration is designed to be incremental:

1. New components are registered in `plugin-services.xml`
2. Adapter classes provide backward compatibility
3. Existing code can gradually migrate to use the new services

## Benefits

1. **Better Separation of Concerns** - Each component has a focused responsibility
2. **Improved Testability** - Components can be tested in isolation
3. **Enhanced Extensibility** - New strategies/commands can be added easily
4. **Reduced Duplication** - Common patterns extracted to shared components
5. **More Consistent Behavior** - Standardized execution flow and memory management

## Future Work

1. Replace legacy calls to directly use the new services
2. Gradually phase out adapter classes
3. Add unit tests for the new components
4. Consider further enhancements like caching strategies or more specialized commands

## Implementation Details

The implementation follows modern Java practices and IntelliJ platform guidelines:
- Services are registered as application services
- Components use dependency injection
- Concurrency is handled safely
- Error handling is centralized
