---
id: TASK-175
title: 'Fix java:S3776 in MCPListenerService.java at line 52'
status: Done
assignee: []
created_date: '2026-02-21 12:34'
updated_date: '2026-02-21 13:07'
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
- **File:** `src/main/java/com/devoxx/genie/service/mcp/MCPListenerService.java`
- **Line:** 52
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 52 in `src/main/java/com/devoxx/genie/service/mcp/MCPListenerService.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `MCPListenerService.java:52` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `onRequest` in MCPListenerService.java to reduce cognitive complexity from 18 to ~4.

**Changes made:**
- Extracted `handleAiMessage(AiMessage, boolean)` helper method containing the AiMessage-related logic
- Removed the redundant inner `if (toolExecutionRequests != null && !toolExecutionRequests.isEmpty())` check that was already guarded by the outer `hasToolExecutionRequests() && !isEmpty()` check
- `onRequest` now has complexity ~4; `handleAiMessage` has complexity ~8 — both well below the 15 limit
- `List` import retained (still used for `List<ChatMessage>`)

**Files modified:** `src/main/java/com/devoxx/genie/service/mcp/MCPListenerService.java`

**Tests:** All 13 existing tests in `MCPListenerServiceTest` pass. No new tests needed — existing coverage is comprehensive and behavior is unchanged.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed SonarQube rule `java:S3776` (Cognitive Complexity too high) in `MCPListenerService.java` at line 52.\n\n### Problem\nThe `onRequest` method had a cognitive complexity of 18, exceeding the allowed maximum of 15. The method contained deeply nested conditionals: instance-of checks, text null/empty checks, agent mode branching, and tool execution request handling.\n\n### Solution\nExtracted the AiMessage-handling logic into a private `handleAiMessage(AiMessage, boolean)` helper method:\n\n- **`onRequest`**: Now has complexity ~4 (only the guard clause and the instanceof dispatch remain)\n- **`handleAiMessage`**: Has complexity ~8 (handles text publishing and tool request publishing)\n\nAdditionally removed the redundant inner null/empty check for `toolExecutionRequests` — it was already fully guarded by `aiMessage.hasToolExecutionRequests() && !aiMessage.toolExecutionRequests().isEmpty()` in the outer condition.\n\n### Files Modified\n- `src/main/java/com/devoxx/genie/service/mcp/MCPListenerService.java`\n\n### Tests\nAll 13 existing tests in `MCPListenerServiceTest` pass with no modifications. The existing test suite already provides comprehensive coverage of all code paths, so no new tests were added.
<!-- SECTION:FINAL_SUMMARY:END -->
