---
id: TASK-66
title: Fix java:S1602 in WebViewRecoveryStrategies.java at line 395
status: Done
priority: low
assignee: []
created_date: '2026-02-20 18:22'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 66000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewRecoveryStrategies.java`
- **Line:** 395
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 395 in `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewRecoveryStrategies.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `WebViewRecoveryStrategies.java:395` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Notes

- **File modified:** `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewRecoveryStrategies.java`
- **Change:** At line 395, the lambda passed to `new Timer(3000, evt -> {...})` had unnecessary curly braces around a single `debugLogger.debug(...)` statement. Removed the braces per SonarQube rule `java:S1602`.
- **Before:**
  ```java
  Timer healthCheckTimer = new Timer(3000, evt -> {
      debugLogger.debug("Post-reconnection health check completed");
  });
  ```
- **After:**
  ```java
  Timer healthCheckTimer = new Timer(3000, evt ->
      debugLogger.debug("Post-reconnection health check completed"));
  ```

## Final Summary

Fixed SonarQube rule `java:S1602` ("Lambda expressions should not have useless curly braces around their bodies") in `WebViewRecoveryStrategies.java` at line 395.

The lambda body inside the `Timer` constructor at line 395 contained a single debug log statement wrapped in unnecessary curly braces. Removed the braces and trailing semicolon inside the lambda, converting the block lambda to an expression lambda. This is a pure formatting/style change with no functional impact.
