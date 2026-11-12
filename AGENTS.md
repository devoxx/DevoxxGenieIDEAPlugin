# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Project Overview

DevoxxGenie is a Java-based LLM Code Assistant plugin for IntelliJ IDEA that integrates with both local LLM providers (Ollama, LMStudio, GPT4All, Llama.cpp, Exo) and cloud-based LLMs (OpenAI, Anthropic, Mistral, Groq, Gemini, DeepInfra, DeepSeek, OpenRouter, OrcaRouter, Requesty, DemonRoute, Azure OpenAI, Amazon Bedrock). The plugin supports advanced features like RAG (Retrieval-Augmented Generation), MCP (Model Context Protocol) servers, web search, and agentic programming capabilities.

## Build & Development Commands

### Building
```bash
./gradlew buildPlugin              # Build plugin (creates ZIP in build/distributions/)
./gradlew clean                    # Clean build artifacts
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
- **Docusaurus**: Documentation website (`docusaurus/`)

### Key Architectural Components

#### 1. Prompt Execution Flow
The plugin processes user prompts through a layered architecture:

**Entry Point**:
- `ActionButtonsPanelController.handlePromptSubmission()` → `PromptExecutionController.handlePromptSubmission()`

**Processing Layer**:
- `PromptExecutionService.executeQuery()` - Handles token calculations, RAG, and GitDiff settings
- `LLMProviderService.getAvailableModelProviders()` - Retrieves model from ChatModelFactory

**Execution Strategies** (`service/prompt/strategy/`): `StreamingPromptStrategy`, `NonStreamingPromptStrategy`,
`WebSearchPromptStrategy`, `CliPromptStrategy`, `AcpPromptStrategy`

**Response Rendering**:
- The chat output is Compose for Desktop: `ConversationPanel` hosts `ConversationScreen` (`src/main/kotlin/.../ui/compose/screen/`)

#### 2. LLM Provider System
**Factory Pattern Implementation**:
- `ChatModelFactory` (interface) - Base factory for all providers
- `ChatModelFactoryProvider` - Provider registry and lookup
- Provider-specific factories under:
  - `chatmodel/cloud/` - Cloud providers (OpenAI, Anthropic, Gemini, etc.)
  - `chatmodel/local/` - Local providers (Ollama, GPT4All, LMStudio, etc.)

**Cloud Providers**: anthropic, azureopenai, bedrock, deepinfra, deepseek, google, grok, groq, mistral, openai

**Routers** (generic path, defined in `chatmodel/cloud/router/RouterRegistry`): openrouter, orcarouter, requesty, demonroute

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
- Embeddings come from a configurable `EmbeddingProvider` (Ollama, OpenAI, LM Studio, llama.cpp, Custom OpenAI)
- `RagValidatorService` - Validates the RAG prerequisites

**Validators**:
- `DockerValidator` - Checks Docker availability
- `ChromeDBValidator` - Validates ChromaDB connection
- `OllamaValidator` - Verifies Ollama and embedding model
- `EmbeddingProviderValidator`, `EmbeddingModelValidator` - Check the configured embedding provider and model

#### 4. MCP (Model Context Protocol) Support
**Key Services**:
- `MCPService` - Core MCP server management
- `MCPExecutionService` - Executes MCP tool calls
- `MCPListenerService` - Implements ChatModelListener for MCP integration
- `MCPTrafficLogger` - Forwards MCP traffic to the log tool window for debugging

**Configuration**:
- MCP servers configured in Settings UI (`ui/settings/mcp/`)
- Supports stdio and HTTP transports (a saved `HTTP_SSE` server runs as `HTTP`)
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
- `PromptInputArea` - User input area
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

## Code Style & Conventions

### Naming Conventions
- **Variables/Methods**: camelCase
- **Classes/Interfaces**: PascalCase
- **Constants**: SCREAMING_SNAKE_CASE
- **Service Classes**: Suffix with "Service" (e.g., `ChatService`)
- **Factory Classes**: Suffix with "Factory" (e.g., `ChatModelFactory`)
- **Panel Classes**: Suffix with "Panel" (e.g., `ConversationPanel`)

### Dependency Management
- Java bytecode target: 17 (build with JDK 21)
- Minimum IDE build and library versions live in `build.gradle.kts` (`sinceBuild`, `lg4j_version`)
- Uses Lombok for boilerplate reduction

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
Custom prompts are managed in `PromptSettingsComponent` (Settings → Prompts) and edited through `CustomPromptDialog`.

### Debugging MCP Issues
1. Enable MCP logging in Settings → MCP
2. View logs in the DevoxxGenie activity log tool window (`AgentMcpLogPanel`)
3. Check `MCPTrafficLogger` for request/response traffic
4. Verify transport configuration (stdio vs HTTP; a saved `HTTP_SSE` server runs as `HTTP`)

### Working with RAG
1. Ensure Docker is running
2. ChromaDB container must be started (docker-java integration)
3. A configured embedding provider (`EmbeddingProvider`: Ollama, OpenAI, LM Studio, llama.cpp or Custom OpenAI)
4. Use `RagValidatorService` to check prerequisites
5. Index project via `ProjectIndexerService.indexFiles()`

## Release Process
Follow `.claude/skills/release/SKILL.md`; it owns the full sequence. Ask which version to cut rather than assuming
the next number. Publishing to the JetBrains Marketplace is a separate step, done only when the user asks.

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

<!-- BACKLOG.MD MCP GUIDELINES START -->

## Backlog workflow

This project tracks tasks and project work with Backlog.md MCP. Before creating a task, or when unsure whether work
should be tracked, read the workflow overview: the `backlog://workflow/overview` resource, or the Backlog MCP
server's instructions tool where resources aren't available. It covers when to create tasks, searching first to
avoid duplicates, and the guides for creating, executing and finalizing tasks. It is not summarized here.

<!-- BACKLOG.MD MCP GUIDELINES END -->

<!-- groma:start -->
## Groma

This project uses Groma. Before you scan, inspect, or curate architecture, or change files for a Backlog task, run `groma agent-instructions` and read the guide it names for that job. When it reports a first scan, ask the user whether they want you to curate the architecture. Do not edit Groma-owned architecture files directly.
<!-- groma:end -->
