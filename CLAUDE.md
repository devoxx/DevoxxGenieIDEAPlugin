# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DevoxxGenie is a Java-based LLM Code Assistant plugin for IntelliJ IDEA that integrates with both local LLM providers (
Ollama, LMStudio, GPT4All, Llama.cpp, Exo) and cloud-based LLMs (OpenAI, Anthropic, Mistral, Groq, Gemini, DeepInfra,
DeepSeek, OpenRouter, Azure OpenAI, Amazon Bedrock). The plugin supports advanced features like RAG (Retrieval-Augmented
Generation), MCP (Model Context Protocol) servers, web search, and agentic programming capabilities.

## Threading Constraints

This is a JetBrains/IntelliJ plugin. UI operations must run on the EDT (Event Dispatch Thread), and long-running
operations must NOT block it. Use `ApplicationManager.getApplication().invokeLater()` or `ReadAction`/`WriteAction`
as appropriate.

## Workflow Rules

When asked to investigate or fix an issue, do NOT deeply explore the entire codebase autonomously. Start focused on
the specific area mentioned, and ask before expanding scope. Avoid unnecessary web searches unless explicitly
requested.

## Git Workflow

Always create a feature/fix branch BEFORE making any code changes. Never edit code on the current branch without
confirming the branch strategy first. Feature/fix branches for issues are cut from `develop` (e.g. `fix/issue-1234`).

## Testing

When asked to investigate a bug, write a reproducing test FIRST — confirm it fails — before applying any fix, unless
told otherwise.

## Build & Development Commands

### Java Version Requirement

**IMPORTANT**: This project requires **JDK 21** for building. JDK 25 causes Gradle build script failures. Always set `JAVA_HOME` before running Gradle commands:

```bash
export JAVA_HOME=~/.sdkman/candidates/java/21-zulu
```

### Testing

**IMPORTANT**: Always pipe test output to grep for failures so you can immediately focus on what failed:

```bash
./gradlew test 2>&1 | grep -E "FAILED|failed"
./gradlew test --tests ClassName 2>&1 | grep -E "FAILED|failed"
./gradlew test --tests ClassName.methodName 2>&1 | grep -E "FAILED|failed"
./gradlew verifyPlugin                            # Verify plugin (includes tests)
```

## Core Architecture

### Adding a New LLM Provider

1. Create factory class implementing `ChatModelFactory` under `chatmodel/cloud/` or `chatmodel/local/`
2. Implement `createChatModel()` and `createStreamingChatModel()` methods
3. Register in `ChatModelFactoryProvider`
4. Add provider to `ModelProvider` enum in `model/enumarations/`

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
2. View logs in MCP Log Tool Window (`MCPLogPanel`)
3. Check `MCPCallbackLogger` for request/response details
4. Verify transport configuration (stdio vs HTTP SSE)

### Working with RAG

1. Ensure Docker is running
2. ChromaDB container must be started (docker-java integration)
3. Ollama with nomic-embed-text model required
4. Use `RAGValidatorService` to check prerequisites
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

<CRITICAL_INSTRUCTION>

## BACKLOG WORKFLOW INSTRUCTIONS

This project uses Backlog.md MCP for all task and project management activities.

**CRITICAL GUIDANCE**

- If your client supports MCP resources, read `backlog://workflow/overview` to understand when and how to use Backlog for this project.
- If your client only supports tools or the above request fails, call `backlog.get_workflow_overview()` tool to load the tool-oriented overview (it lists the matching guide tools).

- **First time working here?** Read the overview resource IMMEDIATELY to learn the workflow
- **Already familiar?** You should have the overview cached ("## Backlog.md Overview (MCP)")
- **When to read it**: BEFORE creating tasks, or when you're unsure whether to track work

These guides cover:
- Decision framework for when to create tasks
- Search-first workflow to avoid duplicates
- Links to detailed guides for task creation, execution, and finalization
- MCP tools reference

You MUST read the overview resource to understand the complete workflow. The information is NOT summarized here.

</CRITICAL_INSTRUCTION>

<!-- BACKLOG.MD MCP GUIDELINES END -->
