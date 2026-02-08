# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DevoxxGenie is a Java-based LLM Code Assistant plugin for IntelliJ IDEA that integrates with both local LLM providers (Ollama, LMStudio, GPT4All, Llama.cpp, Exo) and cloud-based LLMs (OpenAI, Anthropic, Mistral, Groq, Gemini, DeepInfra, DeepSeek, OpenRouter, Azure OpenAI, Amazon Bedrock). The plugin supports advanced features like RAG (Retrieval-Augmented Generation), MCP (Model Context Protocol) servers, web search, and agentic programming capabilities.

## Build & Development Commands

### Building
```bash
./gradlew buildPlugin              # Build plugin (creates ZIP in build/distributions/)
./gradlew clean                    # Clean build artifacts
./gradlew shadowJar                # Create shadow JAR with dependencies
```

### Testing
```bash
./gradlew test                                    # Run all tests
./gradlew test --tests ClassName                  # Run specific test class
./gradlew test --tests ClassName.methodName      # Run single test method
./gradlew verifyPlugin                            # Verify plugin (includes tests)
```

### Running & Publishing
```bash
./gradlew runIde                   # Run IntelliJ IDEA with plugin for testing
./gradlew publishPlugin            # Publish to JetBrains Marketplace (requires PUBLISH_TOKEN env var)
```

### Task Automation (using Taskfile)
```bash
task build                         # Build the plugin
task test                          # Run tests
task run-ide                       # Run IDE with plugin
task generate-changelog VERSION=0.8.0  # Generate changelog from merged PRs
task preview-changes VERSION=0.8.0     # Preview changelog without committing
```

## Core Architecture

### Multi-Module Structure
- **Root module**: Main IntelliJ plugin code (`src/main/java/com/devoxx/genie/`)
- **Core module**: Shared utilities being refactored (see `core/README.md` - issue #564)
- **Docusaurus**: Documentation website (`docusaurus/`)

### Key Architectural Components

#### 1. Prompt Execution Flow
The plugin processes user prompts through a layered architecture:

**Entry Point**:
- `UserPromptPanel` → `PromptSubmissionListener.onPromptSubmitted()` → `PromptExecutionController.handlePromptSubmission()`

**Processing Layer**:
- `PromptExecutionService.executeQuery()` - Handles token calculations, RAG, and GitDiff settings
- `ChatPromptExecutor.executePrompt()` - Dispatches to appropriate LLM provider
- `LLMProviderService.getAvailableModelProviders()` - Retrieves model from ChatModelFactory

**Execution Strategies**:
- `StreamingPromptExecutor` - Token-by-token streaming responses
- `NonStreamingPromptExecutionService` - Full response mode
- `WebSearchPromptExecutionService` - Web search augmented prompts

**Response Rendering**:
- `ChatStreamingResponsePanel` - Real-time streaming UI updates
- `ChatResponsePanel` - Final response display with code highlighting
- `ResponseHeaderPanel`, `ResponseDocumentPanel`, `MetricExecutionInfoPanel` - Modular response components

#### 2. LLM Provider System
**Factory Pattern Implementation**:
- `ChatModelFactory` (interface) - Base factory for all providers
- `ChatModelFactoryProvider` - Provider registry and lookup
- Provider-specific factories under:
  - `chatmodel/cloud/` - Cloud providers (OpenAI, Anthropic, Gemini, etc.)
  - `chatmodel/local/` - Local providers (Ollama, GPT4All, LMStudio, etc.)

**Cloud Providers**: anthropic, azureopenai, bedrock, deepinfra, deepseek, glm, google, grok, groq, kimi, mistral, openai, openrouter

**Local Providers**: ollama, gpt4all, lmstudio, llamacpp, jan, customopenai

**Adding New Providers**:
1. Create factory class implementing `ChatModelFactory` under `chatmodel/cloud/` or `chatmodel/local/`
2. Implement `createChatModel()` and `createStreamingChatModel()` methods
3. Register in `ChatModelFactoryProvider`
4. Add provider to `ModelProvider` enum in `model/enumarations/`

#### 3. RAG (Retrieval-Augmented Generation) System
**Components**:
- `ProjectIndexerService` - Indexes project files for semantic search
- `ChromaEmbeddingService` - Stores embeddings in ChromaDB (Docker-based, v0.6.2)
- `SemanticSearchService` - Retrieves relevant code based on similarity
- Uses Ollama with Nomic Text embeddings for vector generation
- `RAGValidatorService` - Validates Docker, ChromaDB, and Ollama setup

**Validators**:
- `DockerValidator` - Checks Docker availability
- `ChromeDBValidator` - Validates ChromaDB connection
- `OllamaValidator` - Verifies Ollama and embedding model
- `NomicEmbedTextValidator` - Checks nomic-embed-text model

#### 4. MCP (Model Context Protocol) Support
**Key Services**:
- `MCPService` - Core MCP server management
- `MCPExecutionService` - Executes MCP tool calls
- `MCPListenerService` - Implements ChatModelListener for MCP integration
- `MCPCallbackLogger` - Logs MCP requests/responses for debugging

**Configuration**:
- MCP servers configured in Settings UI (`ui/settings/mcp/`)
- Supports stdio and HTTP SSE transports
- Tools are automatically exposed to LLM conversations when MCP is enabled

#### 5. Service Layer Organization
Key services under `service/`:
- `ChatService` - Manages chat conversations
- `MessageCreationService` - Constructs LLM messages with context
- `TokenCalculationService` - Calculates token usage and costs
- `ProjectContentService` - Extracts project content for context
- `FileListManager` - Manages files added to prompt context
- `ConversationStorageService` - Persists chat history locally (SQLite)

### UI Architecture
**Main Panels**:
- `DevoxxGenieToolWindowContent` - Main plugin window
- `ConversationPanel` - Chat conversation display
- `UserPromptPanel` - User input area with image/file DnD support
- `ActionButtonsPanel` - Control buttons (submit, stop, clear, etc.)
- `PromptOutputPanel` - Response output with streaming support

**Settings UI** (`ui/settings/`):
- `LLMProvidersComponent` - Configure LLM providers and API keys
- `RAGSettingsComponent` - RAG feature configuration
- `MCPSettingsComponent` - MCP server management
- `PromptSettingsComponent` - Custom prompts and shortcuts
- `WebSearchProvidersComponent` - Google/Tavily search setup

### Project Scanner & AST Analysis
**Language-Specific Scanners** (`service/analyzer/languages/`):
- Each language has a `ProjectScannerExtension` implementation
- Supports: Java, Kotlin, Python, JavaScript, Go, Rust, C++, PHP
- Extracts AST context (parent classes, field references) for better code analysis
- `ProjectAnalyzer` coordinates language-specific scanning
- `CachedProjectScanner` - Caches scan results for performance

### Test Driven Generation (TDG)
**Experimental Feature** (`service/tdg/`):
- `CodeGeneratorService` - Generates implementation from unit tests
- Allows writing tests first, then generating implementation

## Code Style & Conventions

### Naming Conventions
- **Variables/Methods**: camelCase
- **Classes/Interfaces**: PascalCase
- **Constants**: SCREAMING_SNAKE_CASE
- **Service Classes**: Suffix with "Service" (e.g., `ChatService`)
- **Factory Classes**: Suffix with "Factory" (e.g., `ChatModelFactory`)
- **Panel Classes**: Suffix with "Panel" (e.g., `UserPromptPanel`)

### Dependency Management
- Java minimum: JDK 17
- IntelliJ minimum: 2023.3.4
- Langchain4J version: 1.10.0 (beta: 1.10.0-beta18 for MCP/Chroma/web search)
- Uses Lombok for boilerplate reduction
- Shadow plugin for fat JAR creation with dependency merging

### Testing Practices
- Tests located in `src/test/java/` mirroring main structure
- Uses JUnit 5 (Jupiter) for testing
- Mockito for mocking
- AssertJ for fluent assertions
- Integration tests suffixed with `IT` (e.g., `PromptExecutionServiceIT`)
- Platform tests extend `AbstractLightPlatformTestCase`

## Important Implementation Details

### DEVOXXGENIE.md Files
- Projects can include a `DEVOXXGENIE.md` file at root
- This file is automatically added to the system prompt for better LLM context
- Can be generated via Settings UI or `/init` command in prompt input
- Provides project-specific guidance to the LLM

### Chat Memory & Context
- Configurable chat memory size (default: 10 messages)
- `ChatMemoryManager` handles memory lifecycle per conversation
- Conversations stored locally using SQLite
- Each conversation maintains independent memory context

### Token Calculation & Cost Estimation
- Real-time token counting using JTokkit library
- Cost calculation for cloud providers
- Context window tracking and warnings
- Supports adding full project to context for large context models (e.g., Gemini 1M tokens)

### Web Search Integration
- Google Custom Search and Tavily supported
- `WebSearchPromptExecutionService` augments prompts with search results
- Configured via Settings UI with API keys

### Git Integration
- `GitMergeService` - Git diff integration
- Can include uncommitted changes in prompt context
- Useful for explaining changes or generating commit messages

## Plugin Configuration Files
- `src/main/resources/META-INF/plugin.xml` - Main plugin descriptor
- Language-specific features: `java-features.xml`, `kotlin-features.xml`, `python-features.xml`, etc.
- `src/main/resources/application.properties` - Application properties (version auto-updated by build)
- `gradle.properties` - Gradle configuration
- `.env` - Local environment variables (not committed)

## Common Development Workflows

### Adding a New Custom Prompt
1. Update `model/CustomPrompt.java` if new model properties needed
2. Modify `ui/settings/prompt/PromptSettingsComponent.java` for UI
3. Update `service/prompt/command/CustomPromptCommand.java` for execution
4. Add entry in Settings → Prompts panel

### Debugging MCP Issues
1. Enable MCP logging in Settings → MCP
2. View logs in MCP Log Tool Window (`MCPLogPanel`)
3. Check `MCPCallbackLogger` for request/response details
4. Verify transport configuration (stdio vs HTTP SSE)

### Working with RAG
1. Ensure Docker is running
2. ChromaDB container must be started (docker-java integration)
3. Ollama with nomic-embed-text model required
4. Use `RAGValidatorService` to check prerequisites
5. Index project via `ProjectIndexerService.indexFiles()`

## Release Process
1. Update version in `build.gradle.kts`
2. Generate changelog: `task generate-changelog VERSION=x.y.z`
3. Review and commit changes to `CHANGELOG.md` and `plugin.xml`
4. Build: `./gradlew buildPlugin`
5. Test: `./gradlew test` and `./gradlew verifyPlugin`
6. Publish: Set `PUBLISH_TOKEN` env var and run `./gradlew publishPlugin`

## Key Dependencies & Tools
- **Langchain4J**: LLM integration framework (core abstraction)
- **Docker Java**: Docker integration for ChromaDB
- **JTokkit**: Token counting
- **SQLite JDBC**: Local conversation storage
- **CommonMark**: Markdown parsing and rendering
- **Netty**: Async networking
- **Logback**: Logging framework
- **Retrofit**: HTTP client for external APIs
- **AWS SDK**: Amazon Bedrock integration
