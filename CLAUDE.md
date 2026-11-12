# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DevoxxGenie is a Java-based LLM Code Assistant plugin for IntelliJ IDEA that integrates with both local LLM providers (
Ollama, LMStudio, GPT4All, Llama.cpp, Rapid-MLX, Exo) and cloud-based LLMs (OpenAI, Anthropic, Mistral, Groq, Gemini, DeepInfra,
DeepSeek, OpenRouter, Azure OpenAI, Amazon Bedrock). The plugin supports advanced features like RAG (Retrieval-Augmented
Generation), MCP (Model Context Protocol) servers, web search, and agentic programming capabilities.

## Threading Constraints

This is a JetBrains/IntelliJ plugin. UI operations must run on the EDT (Event Dispatch Thread), and long-running
operations must NOT block it. Use `ApplicationManager.getApplication().invokeLater()` or `ReadAction`/`WriteAction`
as appropriate.

## Workflow Rules

When asked to investigate or fix an issue, start with the specific area mentioned and ask before widening the
investigation to the rest of the codebase. Search the web only when the user asks for it.

## Git Workflow

Always create a feature/fix branch BEFORE making any code changes. Never edit code on the current branch without
confirming the branch strategy first. Feature/fix branches for issues are cut from `master` (e.g. `fix/issue-1234`).

## Testing

When asked to investigate a bug, write a reproducing test FIRST — confirm it fails — before applying any fix, unless
told otherwise.

## Build & Development Commands

### Java Version Requirement

This project builds with JDK 21; JDK 25 breaks the Gradle build script. Set `JAVA_HOME` before running Gradle commands:

```bash
export JAVA_HOME=~/.sdkman/candidates/java/21-zulu
```

### Testing

Filter test output for failures so you can focus on what failed:

```bash
./gradlew test 2>&1 | grep -E "FAILED|failed"
./gradlew test --tests ClassName 2>&1 | grep -E "FAILED|failed"
./gradlew test --tests ClassName.methodName 2>&1 | grep -E "FAILED|failed"
./gradlew verifyPlugin                            # Verify plugin (includes tests)
```

Compiler errors don't match this filter. When the build fails before the tests run, read the unfiltered output.

## Core Architecture

### Adding a New LLM Provider

1. Create factory class implementing `ChatModelFactory` under `chatmodel/cloud/` or `chatmodel/local/`
2. Implement `createChatModel()` and `createStreamingChatModel()` methods
3. Register in `ChatModelFactoryProvider`
4. Add provider to `ModelProvider` enum in `model/enumarations/`

**LLM routers** (OpenAI-compatible gateways such as OpenRouter, Requesty, DemonRoute) skip the factory: add a
`Type.ROUTER` entry to the `ModelProvider` enum and one `RouterDefinition` in `RouterRegistry`. Use
`RouterRegistry.isGeneric(provider)` rather than `Type.ROUTER` in shared code (Cloudflare is `Type.ROUTER` but not
generic). `RouterRegistryTest.everyDefinition_isFullyWired` enforces the wiring.

**OpenAI-compatible local servers** (Jan, LLaMA.c++, GPT4All, Nativ, Rapid-MLX) skip the factory too: add a `Type.LOCAL` entry to the
`ModelProvider` enum and one `LocalProviderDefinition` in `LocalProviderRegistry`. Use
`LocalProviderRegistry.isGeneric(provider)` in shared code. `LocalProviderRegistryTest.everyDefinition_isFullyWired`
enforces the wiring.

Steps 3 and 4 are easy to miss — a new factory silently does nothing until it is registered in both places.

## Important Implementation Details

### DEVOXXGENIE.md Files

- Projects can include a `DEVOXXGENIE.md` file at root
- This file is automatically added to the system prompt for better LLM context
- Can be generated via Settings UI or `/init` command in prompt input
- Provides project-specific guidance to the LLM

## Common Development Workflows

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

**RAG + Agent mode interaction:**

- When agent mode is **off** and RAG is activated, `MessageCreationService` injects top-K
  semantic hits passively into the user message as a `<SemanticContext>` block.
- When agent mode is **on** and RAG is activated, the passive `<SemanticContext>` injection
  is suppressed and a `semantic_search` agent tool is registered instead (see
  `BuiltInToolProvider` and `SemanticSearchToolExecutor`). The LLM decides when to retrieve
  semantically vs. when to use lexical tools like `search_files`. This avoids the failure
  mode where two competing context sources cause models to ignore the higher-quality
  semantic results in favor of invoking grep-style tools.

## Release Process

Use the `release` skill (`.claude/skills/release/`) — it owns the full sequence. Always ask which version to cut;
never assume the next version number.

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
