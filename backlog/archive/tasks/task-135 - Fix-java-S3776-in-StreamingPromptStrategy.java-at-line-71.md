---
id: TASK-135
title: 'Fix java:S3776 in StreamingPromptStrategy.java at line 71'
status: Done
assignee: []
created_date: '2026-02-21 09:47'
updated_date: '2026-02-21 09:55'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java`
- **Line:** 71
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 29 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 71 in `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 29 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `StreamingPromptStrategy.java:71` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `executeStrategySpecific` in `StreamingPromptStrategy.java` to reduce cognitive complexity from 29 to ~9 by extracting four helper methods:

1. `createStreamingResponseHandler(context, panel, resultTask)` — consolidates the test-environment detection and handler instantiation (previously duplicated try-catch + isTestEnvironment flag). Simplified to a single try/multi-catch block.

2. `executeStreamingInBackground(context, streamingModel, handler)` — moves the thread-pool body (multimodal branch + AiServices setup + TokenStream chaining) out of the lambda in the main method.

3. `resolveToolProvider(context)` — extracts agent/MCP tool provider selection and file-reference population.

4. `buildAssistant(context, streamingModel, chatMemory)` — constructs the AiServices assistant with or without a tool provider.

5. `logToolExecution(toolExecution)` — eliminates a lambda-with-ternary on the `.onToolExecuted(...)` call.

Also cleaned up unused imports (`MCPService`, `TokenStream` duplicated, `List`, `ChatMessage`) that were no longer needed after the refactoring.

All 5 existing tests in `StreamingPromptStrategyTest` pass (BUILD SUCCESSFUL).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` in `StreamingPromptStrategy.java` by reducing the cognitive complexity of `executeStrategySpecific` from 29 to ~9 (well within the 15 limit).\n\n### What Changed\n\n**File:** `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java`\n\nThe monolithic `executeStrategySpecific` method was decomposed into five focused helper methods:\n\n1. **`createStreamingResponseHandler(context, panel, resultTask)`** — Consolidates the previously duplicated test-environment detection (two `Class.forName` calls + `isTestEnvironment` flag) into a single try/multi-catch block. Falls through to the normal `StreamingResponseHandler` on `ClassNotFoundException`, or throws on other reflection failures.\n\n2. **`executeStreamingInBackground(context, streamingModel, handler)`** — Extracts the background thread body: multimodal branch (direct model call) and standard AiServices branch.\n\n3. **`resolveToolProvider(context)`** — Extracts the agent/MCP tool provider lookup, tracker registration, and file-reference population.\n\n4. **`buildAssistant(context, streamingModel, chatMemory)`** — Builds the `AiServices` assistant with or without a tool provider, using `var` to avoid exposing the internal builder type.\n\n5. **`logToolExecution(toolExecution)`** — Eliminates the nested lambda-with-ternary that was passed to `.onToolExecuted(...)`, replacing it with a clean method reference.\n\n### Imports Cleaned Up\n\nRemoved imports that became unused after the refactoring:\n- `MCPService` (was already unused in the original)\n- `List` (was only used for `List<ChatMessage>` variable)\n- `ChatMessage` (same)\n\n(`TokenStream` was re-added since it's used in the `Assistant` interface at the bottom of the class.)\n\n### Tests\n\nAll 5 existing tests in `StreamingPromptStrategyTest` pass (BUILD SUCCESSFUL). No new tests were required — the extracted methods are pure reorganizations of existing logic, and all public-facing behaviors are already covered by the existing test suite (including the `TestStreamingResponseHandler` test helper that enables handler creation in tests without a real JCEF webview).
<!-- SECTION:FINAL_SUMMARY:END -->
