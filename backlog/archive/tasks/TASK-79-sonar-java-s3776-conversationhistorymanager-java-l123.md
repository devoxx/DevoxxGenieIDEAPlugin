---
id: TASK-79
title: Fix java:S3776 in ConversationHistoryManager.java at line 123
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:46'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 79000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationHistoryManager.java`
- **Line:** 123
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 123 in `src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationHistoryManager.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `ConversationHistoryManager.java:123` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the repeated `ChatMessageContext` builder pattern (including the ternary `conversation.getExecutionTimeMs() > 0 ? ... : 1000`) into a new private helper method `createMessageContext(Conversation, int)`. This method:
1. Builds the `ChatMessageContext` with project and execution time
2. Sets a stable message ID (`conversation.getId() + "_msg_" + messageIndex`)
3. Calls `populateModelInfo` to add LLM provider info

The two occurrences of the inline builder pattern in `processConversationMessages` (lines 146-163 and 178-189) were replaced with calls to `createMessageContext(conversation, messageIndex)`. This removes the two ternary operators from the main method, reducing cognitive complexity from 17 to approximately 13 (well within the 15 limit).

The refactoring also eliminated code duplication (DRY principle) without changing any observable behavior.

## Final Summary

**Problem:** `processConversationMessages` in `ConversationHistoryManager.java` had a SonarQube cognitive complexity of 17, exceeding the allowed limit of 15.

**Root Cause:** The method contained two duplicate inline builder patterns, each with a ternary operator (`conversation.getExecutionTimeMs() > 0 ? conversation.getExecutionTimeMs() : 1000`), nested inside control flow structures. Each ternary contributed 2+ complexity points due to nesting.

**Fix:** Extracted a private helper method `createMessageContext(Conversation conversation, int messageIndex)` that encapsulates the duplicated context-building pattern. This removes both ternary operators from `processConversationMessages`, reducing its cognitive complexity from 17 to ~13.

**Files Modified:**
- `src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationHistoryManager.java`
  - Replaced two duplicate builder blocks with calls to new `createMessageContext` helper
  - Added `createMessageContext` private method (lines 206-218)

**Tests:** All 17 existing tests in `ConversationHistoryManagerTest` pass successfully, confirming no behavioral regressions.
