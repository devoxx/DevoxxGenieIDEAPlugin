---
id: TASK-75
title: Fix java:S3776 in ChatMemoryManager.java at line 154
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:45'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 75000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java`
- **Line:** 154
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 154 in `src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `ChatMemoryManager.java:154` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

The `addUserMessage` method had a cognitive complexity of 16 due to a deeply nested loop inside an if-else chain. The for-loop iterating over multimodal message contents (with an inner if-instanceof check and else branch) added 3+4+1=8 points of nesting-adjusted complexity.

**Fix:** Extracted the multimodal content escaping logic into a new private helper method `buildEscapedContents(UserMessage)`. This moves the for-loop and nested if-else out of `addUserMessage`, reducing its complexity from 16 to 8 while the helper method itself has complexity 4.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java`

## Final Summary

Resolved SonarQube java:S3776 in `addUserMessage()` at line 154 of `ChatMemoryManager.java`.

**Root cause:** The method contained a for-loop over multimodal message contents with nested instanceof checks, creating 4 levels of nesting inside the try-catch + two if blocks. Cognitive complexity totalled 16 (if+&&=2, if=1, if+nesting=2, for+nesting=3, if+nesting=4, else=1, else=1, catch=1, else=1 = 16).

**Fix:** Extracted the inner loop into a private helper `buildEscapedContents(UserMessage)` which builds the escaped content list for multimodal messages. `addUserMessage` now calls this helper with a single line, reducing its cognitive complexity to 8. The helper itself has complexity 4 — both well within the 15 limit.

No logic was changed; only structure was refactored. All 3 existing `ChatMemoryManagerTest` tests passed (testAddUserMessagePreservesImageContent, testAddUserMessageTextOnlyPath, testAddUserMessageWithTemplateVariablesInMultimodal).
