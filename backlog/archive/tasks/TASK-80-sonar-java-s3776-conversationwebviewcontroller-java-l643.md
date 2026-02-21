---
id: TASK-80
title: Fix java:S3776 in ConversationWebViewController.java at line 643
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
ordinal: 80000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/webview/ConversationWebViewController.java`
- **Line:** 643
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 643 in `src/main/java/com/devoxx/genie/ui/webview/ConversationWebViewController.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `ConversationWebViewController.java:643` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

The `hasBlackScreenIssues()` method had a cognitive complexity of 17 (above the allowed 15) due to deeply nested reflection code inside a try-catch inside an if-block.

**Fix:** Extracted the reflection-based rendering detector check into a new private helper method `checkRenderingDetectorForIssues()`. This removes the inner try-catch and nested if-blocks from the main method, reducing its complexity from 17 to ~10.

**Files modified:**
- `src/main/java/com/devoxx/genie/ui/webview/ConversationWebViewController.java`
  - Replaced inline reflection block in `hasBlackScreenIssues()` with a call to the new helper
  - Added private `checkRenderingDetectorForIssues()` method containing the extracted logic

## Final Summary

Resolved java:S3776 in `ConversationWebViewController.java` at line 643 by extracting the reflection-based rendering detector check from `hasBlackScreenIssues()` into a private helper method `checkRenderingDetectorForIssues()`.

The original method had complexity 17 due to: outer if with `||` (+2), null check (+1), isShowing check (+1), size check with nesting and `||` (+3), null guard if (+1), inner try-catch with nested if/if/&&/catch (+7) = 17 total.

After extracting the reflection block (inner try-catch + if-checks) to its own method, the main method's complexity drops to ~10. The extracted helper has its own complexity of ~6 but that's counted separately and is well within limits.

All existing tests pass.
