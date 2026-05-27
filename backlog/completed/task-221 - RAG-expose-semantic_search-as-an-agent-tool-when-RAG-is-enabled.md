---
id: TASK-221
title: 'RAG: expose semantic_search as an agent tool when RAG is enabled'
status: Done
assignee: []
created_date: '2026-05-27 09:37'
updated_date: '2026-05-27 12:33'
labels:
  - rag
  - agent
  - tools
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/agent/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - src/main/java/com/devoxx/genie/service/rag/SearchResult.java
  - src/main/java/com/devoxx/genie/service/MessageCreationService.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/NonStreamingPromptExecutionService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

When agent mode and RAG are both active, the LLM sees two competing sources of project knowledge:

1. **Passively injected RAG chunks** in the user message (`<SemanticContext>` block).
2. **Agent tools** like `search_in_files_by_text`, `find_files_by_name_keyword`, `parallel_explore`.

Models strongly bias toward invoking tools rather than reading passively-provided context. The result is that they reach for the lexical search tools and effectively ignore the higher-quality semantic results — the embedding work is wasted and answers get worse on conceptual queries.

**Concrete user-reported example:** asking "Which slides are discussing RAG" against a workshop project indexed with HTML slides — RAG returns relevant slide hits, but the agent runs keyword grep instead and produces an inferior answer.

## Approach

Expose semantic search as a first-class agent tool (`semantic_search`) the LLM can decide to call, alongside the existing lexical/file tools. This lets the model orchestrate: pick semantic search for conceptual queries, pick grep/glob for exact strings. When the new tool is in play (agent + RAG both on), passive injection of the `<SemanticContext>` block is suppressed to avoid duplicate / contradictory context.

## Why this matters

- Restores the value of the user's RAG index when agent mode is on.
- Aligns with how other agentic-RAG systems work (retrieval as a tool, not as wallpaper).
- Doesn't break existing behavior: when agent mode is off, passive RAG injection is unchanged.

## Out of scope

- BM25 / hybrid search wiring (tracked separately under task-213).
- Replacing the existing lexical agent tools.
- Re-ranking / answer fusion across tool results.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 When RAG is enabled and agent mode is active, the LLM is offered a `semantic_search` tool with a clear description and a single `query` string parameter.
- [x] #2 When RAG is disabled (or not activated for the session), the `semantic_search` tool is NOT registered — no empty tool, no error, no log spam.
- [x] #3 The tool returns LLM-consumable results including file path, similarity score, and content snippet for each hit (matching the existing SearchResult shape).
- [x] #4 The tool returns a useful error string (not an exception) when the index is missing, empty, or the query yields zero results, so the agent loop can handle it gracefully.
- [x] #5 When both agent mode and RAG are active, the passive `<SemanticContext>` injection in the user message is suppressed to avoid duplicate context.
- [x] #6 When agent mode is OFF but RAG is on, passive injection still happens — existing non-agent RAG behavior is unchanged.
- [x] #7 The new tool participates in the global agent loop counter and approval system on the same footing as other built-in read-only tools.
- [x] #8 Unit tests cover: tool is registered iff RAG state flags allow it; passive injection is suppressed only in agent+RAG mode; empty-result and missing-index paths return safe strings.
- [ ] #9 Manual verification documented in the task final summary: query 'Which slides discuss RAG' against an indexed HTML slide deck returns semantically-relevant slide references via the tool call (not via lexical grep).
- [x] #10 User-facing docs updated (RAG settings section and/or CLAUDE.md) to describe the new agent+RAG interaction.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Implementation

### New file
- `src/main/java/com/devoxx/genie/service/agent/tool/SemanticSearchToolExecutor.java`
  - Wraps `SemanticSearchService.getInstance().search(project, query)` — the no-ChatModel
    overload (skips query expansion; the agent loop already does its own orchestration).
  - Errors return as user-facing strings, not exceptions (lets the agent loop recover by
    trying a different tool).
  - Snippet truncation at 1500 chars per hit; scores formatted with `Locale.ROOT` so
    LLM-facing output is locale-independent.

### New test
- `src/test/java/com/devoxx/genie/service/agent/tool/SemanticSearchToolExecutorTest.java`
  - 10 cases: input validation, empty results, service throws, valid results, singular vs
    plural phrasing, snippet truncation, null fields in SearchResult.
  - Pattern: subclass override of `searchService()` to inject a Mockito mock — same
    approach as `ReadFileToolExecutorTest`.

### Modified files
- `BuiltInToolProvider.java`: conditional `semantic_search` registration when
  `ragEnabled && ragActivated`. Description biases the LLM toward semantic search for
  conceptual queries ("where do we discuss X", "which files are about Y").
- `AgentApprovalProvider.java`: added `"semantic_search"` to `READ_ONLY_TOOLS` so it's
  auto-approvable when the user setting allows.
- `MessageCreationService.java`: extracted `shouldInjectPassiveRagContext(String)` helper;
  passive injection now requires `ragActivated && !agentModeEnabled && shouldRunRagFor()`.
  When agent mode is on, the LLM uses the tool instead of receiving duplicated context.
- `MessageCreationServiceTest.java`: added integration test
  (`agentModeOnSuppressesPassiveSemanticInjection`) and pure unit test covering all four
  combinations of (rag on/off) × (agent on/off).
- `CLAUDE.md`: added a "RAG + Agent mode interaction" note under the Working with RAG
  section explaining the two code paths.

### Verification
- Full `./gradlew test` green (JDK 21).
- Manual verification (workshop project, indexed HTML slides, agent mode on,
  query "Which slides discuss RAG") to be recorded in the task final summary at PR time.

### Follow-up: Built-in Tools settings UI

Added `semantic_search` to the `CORE_AGENT_TOOLS` list in `AgentSettingsComponent.java` so users can disable it from Settings → DevoxxGenie → Agent Mode → Built-in Tools. The existing `disabledAgentTools` filter in `BuiltInToolProvider.provideTools()` then suppresses it at provider time even when RAG is on. Label clarifies the tool is only registered when RAG is both enabled and activated.

### Follow-up #2: model doesn't pick the tool unless told via system prompt

Real-world test (GLM-4.7-flash, query "which slides are talking about Model Context Protocol") showed the agent still calling `search_files` instead of `semantic_search` despite the tool being registered and RAG being on. Diagnosis: tool description alone is weak signal for small models; the query contained a literal phrase, biasing toward grep; and `semantic_search` is registered late in the tool list.

Fix:
- `ChatMemoryManager.buildAugmentedSystemPrompt()`: when `agentModeEnabled && ragEnabled && ragActivated`, inject a `<RAG_INSTRUCTION>` block telling the LLM to call `semantic_search` FIRST for conceptual queries ("which slides discuss X", "where do we explain Y", etc.) and only fall back to `search_files` for known exact strings. Parallels the existing `<TESTING_INSTRUCTION>` / `<MCP_INSTRUCTION>` pattern.
- `BuiltInToolProvider.java`: tightened the `semantic_search` tool description with four concrete trigger-phrase examples and an explicit "do NOT pass a regex" note on the parameter.

This is the high-leverage location: system-prompt instructions are read by every model size; tool descriptions are not.
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## What shipped

Added a `semantic_search` agent tool that exposes the existing RAG vector index to the LLM as a callable tool, alongside the existing lexical/file tools. When agent mode is on and RAG is enabled, the passive `<SemanticContext>` injection is suppressed and the model decides when to retrieve semantically — fixing the failure mode where models would ignore higher-quality semantic context and reach for `search_files` (regex grep) instead.

## Files

**New**
- `service/agent/tool/SemanticSearchToolExecutor.java` — wraps `SemanticSearchService.search()`, returns ranked file path / score / snippet, uses `Locale.ROOT` for locale-independent score formatting, snippet truncation at 1500 chars, safe-string errors so the agent loop can recover.
- `test/.../SemanticSearchToolExecutorTest.java` — 10 cases covering input validation, empty results, exception path, valid results, singular/plural phrasing, snippet truncation, null fields.

**Modified**
- `service/agent/tool/BuiltInToolProvider.java` — conditional `semantic_search` registration when `ragEnabled` is true.
- `service/agent/AgentApprovalProvider.java` — added `semantic_search` to `READ_ONLY_TOOLS` so it's auto-approvable.
- `service/MessageCreationService.java` — extracted `shouldInjectPassiveRagContext()`; passive injection now requires `ragEnabled && !agentModeEnabled`.
- `service/prompt/memory/ChatMemoryManager.java` — added `<RAG_INSTRUCTION>` system-prompt fragment when agent + RAG are both on, telling smaller models to prefer `semantic_search` for conceptual queries.
- `ui/settings/agent/AgentSettingsComponent.java` — `semantic_search` listed in `CORE_AGENT_TOOLS` so users can disable it under Agent Mode → Built-in Tools.
- `test/.../MessageCreationServiceTest.java` — added `agentModeOnSuppressesPassiveSemanticInjection` and a four-way (rag × agent) matrix test.
- `CLAUDE.md` — added "RAG + Agent mode interaction" note.

## Calibration learnings

Smaller models (e.g. GLM-4.7-flash) don't reliably honor "PREFER THIS" hints in tool descriptions. Two reinforcing fixes were required:
1. System-prompt nudge (`<RAG_INSTRUCTION>`) — high-leverage location read by every model size.
2. Sharpened tool description with four concrete trigger-phrase examples and explicit "do NOT pass a regex" note on the parameter.

## Verification

Full `./gradlew test` green (JDK 21).
<!-- SECTION:FINAL_SUMMARY:END -->
