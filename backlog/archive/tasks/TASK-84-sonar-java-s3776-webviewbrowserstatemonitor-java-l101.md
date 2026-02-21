---
id: TASK-84
title: Fix java:S3776 in WebViewBrowserStateMonitor.java at line 101
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
ordinal: 84000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewBrowserStateMonitor.java`
- **Line:** 101
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 101 in `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewBrowserStateMonitor.java`.

## Implementation Notes

Reduced cognitive complexity of `setupBrowserHandlers()` from 17 to well below 15 by extracting two helper methods:

1. **`handleLoadEnd(String url, int httpStatusCode)`** — extracted from the `onLoadEnd` anonymous class override. This method contained an `if/else` with a nested `if` that contributed significantly to the complexity (multiple nesting levels inside the anonymous class inside the try block).

2. **`isConnectionIssueMessage(String value)`** — extracted the compound boolean with four `||` operators from `onStatusMessage`. The original `if (value != null && (... || ... || ... || ...))` was replaced with a clean call to this predicate method.

The `onLoadEnd` override in the anonymous class now delegates entirely to `handleLoadEnd`, and `onStatusMessage` uses the named predicate. All logic is preserved — only the structural nesting is reduced.

## Final Summary

- **File modified:** `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewBrowserStateMonitor.java`
- **Root cause:** `setupBrowserHandlers()` had cognitive complexity 17 (limit is 15) due to anonymous class overrides containing branching logic nested inside a try block.
- **Fix:** Extracted `handleLoadEnd(String, int)` and `isConnectionIssueMessage(String)` as private helper methods. The complex branching in `onLoadEnd` and the compound boolean in `onStatusMessage` are now in dedicated, clearly-named methods.
- **Tests:** All existing tests pass (`BUILD SUCCESSFUL`). No test existed for this class specifically, and the refactoring is purely structural (no behavioral change).

## Acceptance Criteria

- [x] Issue `java:S3776` at `WebViewBrowserStateMonitor.java:101` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass
