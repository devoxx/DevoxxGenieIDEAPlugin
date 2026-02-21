---
id: TASK-85
title: Fix java:S3776 in WebViewBrowserInitializer.java at line 27
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
ordinal: 85000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewBrowserInitializer.java`
- **Line:** 27
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 27 in `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewBrowserInitializer.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `WebViewBrowserInitializer.java:27` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `ensureBrowserInitialized` in `WebViewBrowserInitializer.java` by extracting the monolithic method body into focused private helper methods:

- `waitForBrowserAndExecute(Runnable)` — runs in background thread; orchestrates the wait and callback execution
- `waitForInitialization(long)` — polling loop waiting up to 15s for `initialized` + `jsExecutor.isLoaded()`; returns `true` on success, `false` on timeout or interrupt
- `logWaitingStatusIfNeeded(long)` — periodic log after 5s elapsed
- `executeCallbackWithDelay(Runnable)` — sleeps 200ms for DOM readiness then invokes callback on EDT

Cognitive complexity breakdown after refactor:
- `ensureBrowserInitialized`: ~3 (if + && + else)
- `waitForBrowserAndExecute`: ~2 (if + else)
- `waitForInitialization`: ~6 (while + || + && + nested if)
- `logWaitingStatusIfNeeded`: ~2 (if + &&)
- `executeCallbackWithDelay`: ~2 (lambda nesting + catch)

All existing tests pass. No new issues introduced.

## Final Summary

Fixed SonarQube `java:S3776` in `WebViewBrowserInitializer.java:27` by decomposing the single large `ensureBrowserInitialized` method (cognitive complexity 27) into four private helper methods. The public API is unchanged. All behaviour is preserved. Tests pass.
