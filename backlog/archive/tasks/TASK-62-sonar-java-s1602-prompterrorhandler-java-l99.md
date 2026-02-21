---
id: TASK-62
title: Fix java:S1602 in PromptErrorHandler.java at line 99
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
ordinal: 62000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/service/prompt/error/PromptErrorHandler.java`
- **Line:** 99
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 99 in `src/main/java/com/devoxx/genie/service/prompt/error/PromptErrorHandler.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `PromptErrorHandler.java:99` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Changed the lambda in `showNotification()` from a block lambda (with curly braces around a single statement) to an expression lambda:

Before:
```java
ApplicationManager.getApplication().invokeLater(() -> {
    NotificationUtil.sendNotification(project, exception.getMessage());
});
```

After:
```java
ApplicationManager.getApplication().invokeLater(() ->
    NotificationUtil.sendNotification(project, exception.getMessage()));
```

## Final Summary

Fixed SonarQube rule `java:S1602` in `PromptErrorHandler.java` at line 99. The `showNotification()` method had a lambda passed to `invokeLater()` that wrapped a single statement in unnecessary curly braces. Converted it to a concise expression lambda. No logic was changed, no new issues introduced.
