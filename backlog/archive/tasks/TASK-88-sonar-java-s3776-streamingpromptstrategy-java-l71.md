---
id: TASK-88
title: Fix java:S3776 in StreamingPromptStrategy.java at line 71
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:49'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 88000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 29 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java`
- **Line:** 71
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 29 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 71 in `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `StreamingPromptStrategy.java:71` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `executeStrategySpecific` (line 71) by extracting five helper methods, reducing cognitive complexity from 29 to ~5:

1. **`createStreamingResponseHandler()`** (protected) — Replaces the complex reflection-based test environment detection with a clean factory method that can be overridden in tests. Removed `Class.forName` reflection and double try/catch.

2. **`executeStreamingAsync()`** (private) — Encapsulates the thread pool execution logic.

3. **`executeMultimodalStreaming()`** (private) — Isolates the multimodal (image) content path.

4. **`buildAssistant()`** (private) — Handles tool provider selection and AiServices builder construction.

5. **`resolveToolProvider()`** (private) — Handles agent/MCP tool provider resolution with AgentLoopTracker tracking.

6. **`startTokenStream()`** (private) — Handles TokenStream callback setup.

Also removed unused imports (`ChatMessage`, `List`, `ToolExecution`) that were only needed in the original monolithic method.

## Final Summary

**File modified:** `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java`

The `executeStrategySpecific` method had a cognitive complexity of 29 due to:
- Reflection-based test environment detection (`Class.forName` + nested try/catch)
- Inline tool provider selection with multiple conditionals
- Multimodal handling, assistant building, and token stream setup all in one method

**Solution:** Extracted 5 private/protected helper methods, each with complexity ≤ 5. The key improvement was replacing the fragile `Class.forName` reflection pattern with a protected `createStreamingResponseHandler()` factory method that tests can override cleanly. All 5 existing tests pass without modification.
