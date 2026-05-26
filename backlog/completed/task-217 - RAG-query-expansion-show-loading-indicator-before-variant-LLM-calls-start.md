---
id: TASK-217
title: 'RAG query expansion: show loading indicator before variant LLM calls start'
status: Done
assignee: []
created_date: '2026-05-26 20:13'
updated_date: '2026-05-26 20:36'
labels:
  - rag
  - ui
  - bug
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
  - src/main/java/com/devoxx/genie/service/rag/
  - src/main/java/com/devoxx/genie/controller/PromptExecutionController.java
  - src/main/java/com/devoxx/genie/service/PromptExecutionService.java
  - src/main/java/com/devoxx/genie/ui/panel/UserPromptPanel.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

When **Query expansion** is enabled in RAG settings (`Settings → RAG → Query expansion → Enable LLM query expansion`), submitting a user prompt appears to freeze the UI:

- The user clicks Submit.
- Nothing visible happens — no blue glowing border, no spinner, no progress.
- After the N variant LLM calls (default 3) complete, the blue glowing loading indicator finally appears and the actual RAG search + main prompt continue.

This gives the strong impression that the plugin has hung. Users may click Submit again, cancel, or assume the plugin is broken.

## Expected Behavior

The blue glowing border (loading indicator on the prompt panel / output panel) must appear **immediately** when the user submits the prompt — *before* the query expansion LLM calls start — so the user has continuous visual feedback throughout the entire pipeline:

1. Submit pressed → blue glowing border ON
2. Query expansion (N variant LLM calls) runs
3. RAG search (per-variant retrieval + RRF fusion) runs
4. Main LLM prompt runs (streaming/non-streaming)
5. Final response rendered → blue glowing border OFF

## Reproduction

1. Enable RAG.
2. Settings → RAG → check "Enable LLM query expansion", keep "Number of variants" at 3.
3. Submit any prompt against a RAG-indexed project.
4. Observe: no visual feedback for several seconds while the 3 variant queries run, then the loading indicator appears.

## Likely Root Cause

The loading indicator activation happens inside the prompt execution pipeline *after* the RAG preprocessing step (which now includes query expansion). The activation needs to be hoisted earlier — to the point of `PromptExecutionController.handlePromptSubmission()` / `UserPromptPanel` submission — so that any RAG preprocessing (including the extra LLM round-trips for query expansion) is wrapped by the loading state.

## Suggested Fix

- Trigger the blue glowing border / loading state at the entry point of prompt submission, before `PromptExecutionService.executeQuery()` invokes RAG.
- Verify the indicator remains active during query expansion, RAG retrieval, and the main LLM call, and is correctly cleared on completion, cancellation, and error.
- Ensure no race condition with the existing "deactivate handlers BEFORE hiding loading indicator" pattern.

## UI Reference

Settings UI text: "Paraphrase the query into multiple variants and fuse the per-variant results (Reciprocal Rank Fusion). Improves retrieval on meta-style questions such as 'where do we discuss X?' at the cost of one extra LLM call per RAG search."

(Note: the help text currently says "one extra LLM call" but the implementation issues N calls — one per variant. This is a separate copy/UX issue worth tracking but not part of this fix.)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Blue glowing loading border appears immediately on prompt submission, before any query-expansion LLM call starts
- [x] #2 Loading indicator remains visible continuously through query expansion, RAG retrieval, and main LLM response
- [x] #3 Loading indicator is correctly cleared on successful completion, user cancellation, and error
- [x] #4 Behavior verified with query expansion enabled (variants = 3) and disabled
- [x] #5 No regression in non-RAG prompt submissions or in standard RAG (without query expansion)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Root cause

`PromptExecutionService.executePrompt()` runs on the EDT and calls `strategy.execute()` synchronously, which calls `executeStrategySpecific()` → `prepareMemory(context)` synchronously on the EDT. `prepareMemory()` eventually invokes `SemanticSearchService.searchWithExpansion()` which issues N synchronous LLM calls via `ExpandingQueryTransformer`. While the EDT is blocked, neither the Compose loading indicator (`isLoadingIndicatorVisible` set by `addUserPromptMessage()`) nor the `AnimatedGlowingBorder` Timer can repaint, even though they were activated before the strategy runs.

## Fix

Move `prepareMemory(context)` off the EDT into the existing background pool task in both strategies. The state for the loading indicator is already set earlier in `PromptExecutionController.handlePromptSubmission()`, so freeing the EDT lets the paint actually happen.

- `StreamingPromptStrategy.executeStrategySpecific()` — hoist `prepareMemory()` into the `threadPoolManager.getPromptExecutionPool().execute(...)` block; catch errors and route them through `handler.onError()`; re-check `resultTask.isCancelled()` after `prepareMemory()` returns.
- `NonStreamingPromptStrategy.executeStrategySpecific()` — move `prepareMemory()` to the first line of the existing pool task body; existing try/catch in that block already routes errors through `handleExecutionError()` (hides the loading indicator).
- Regression tests pin behavior: capture the runnable handed to the executor, verify `chatMemoryManager.prepareMemory(...)` was NOT called before the runnable runs, then verify it IS called once the runnable executes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Modified `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java` — `prepareMemory()` now runs inside the prompt execution pool task; errors routed via `handler.onError(e)`; cancellation re-checked after prepareMemory.
- Modified `src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java` — `prepareMemory()` now runs inside the existing pool task (first statement), so it shares the existing try/catch.
- Added `executeStrategySpecific_runsPrepareMemoryInsideExecutorTask` regression test to both `NonStreamingPromptStrategyTest` and `StreamingPromptStrategyTest`.
- Full Gradle test suite: BUILD SUCCESSFUL.
- Manual verification by Stephan: confirmed indicator now appears immediately on submit with query expansion enabled.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Moved `prepareMemory(context)` off the EDT into the existing background pool task in both `StreamingPromptStrategy` and `NonStreamingPromptStrategy`. The Compose loading indicator and animated glow border are already activated earlier in `PromptExecutionController.handlePromptSubmission()` via `addUserPromptMessage()`; the freeze was caused by the EDT being blocked while `ExpandingQueryTransformer` issued N synchronous LLM calls inside `prepareMemory`. With that work now on the pool, the indicator paints immediately on submit. Added `executeStrategySpecific_runsPrepareMemoryInsideExecutorTask` regression tests to both strategy test classes (full Gradle suite green). Verified manually with query expansion enabled (variants = 3): glow + indicator appear instantly.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1056
<!-- SECTION:FINAL_SUMMARY:END -->
