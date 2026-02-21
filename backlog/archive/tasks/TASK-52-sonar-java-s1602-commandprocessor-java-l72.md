---
id: TASK-52
title: Fix java:S1602 in CommandProcessor.java at line 72
status: Done
priority: low
assignee: []
created_date: '2026-02-20 18:21'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 52000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/processor/CommandProcessor.java`
- **Line:** 72
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 72 in `src/main/java/com/devoxx/genie/ui/processor/CommandProcessor.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `CommandProcessor.java:72` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Changed the lambda at line 72 (inside `handleInitCommand`) from a block lambda with unnecessary curly braces to an expression lambda:

Before:
```java
ApplicationManager.getApplication().invokeLater(() -> {
    NotificationUtil.sendNotification(
            project,
            "Error generating DEVOXXGENIE.md file: " + e.getMessage());
});
```

After:
```java
ApplicationManager.getApplication().invokeLater(() ->
    NotificationUtil.sendNotification(
            project,
            "Error generating DEVOXXGENIE.md file: " + e.getMessage()));
```

The lambda body contained only a single statement, so curly braces were unnecessary per SonarQube rule `java:S1602`.

## Final Summary

Resolved SonarQube rule `java:S1602` ("Lambdas containing only one statement should not nest this statement in a block") in `CommandProcessor.java` at line 72. The fix converts a single-statement block lambda `() -> { ... }` to an expression lambda `() -> ...`, removing the unnecessary curly braces. No logic was changed.
