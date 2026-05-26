---
id: TASK-217
title: 'RAG query expansion: show loading indicator before variant LLM calls start'
status: To Do
assignee: []
created_date: '2026-05-26 20:13'
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
- [ ] #1 Blue glowing loading border appears immediately on prompt submission, before any query-expansion LLM call starts
- [ ] #2 Loading indicator remains visible continuously through query expansion, RAG retrieval, and main LLM response
- [ ] #3 Loading indicator is correctly cleared on successful completion, user cancellation, and error
- [ ] #4 Behavior verified with query expansion enabled (variants = 3) and disabled
- [ ] #5 No regression in non-RAG prompt submissions or in standard RAG (without query expansion)
<!-- AC:END -->
