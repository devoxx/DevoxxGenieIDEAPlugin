---
id: TASK-59
title: Fix java:S1602 in MessageRenderer.java at line 132
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
ordinal: 59000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/panel/conversation/MessageRenderer.java`
- **Line:** 132
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 132 in `src/main/java/com/devoxx/genie/ui/panel/conversation/MessageRenderer.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `MessageRenderer.java:132` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Changed `scrollToBottom()` lambda from block form (`() -> { ... }`) to expression form (`() -> ...`).

Before:
```java
ApplicationManager.getApplication().invokeLater(() -> {
    webViewController.executeJavaScript("setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);");
});
```

After:
```java
ApplicationManager.getApplication().invokeLater(() ->
    webViewController.executeJavaScript("setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);"));
```

## Final Summary

Resolved SonarQube rule `java:S1602` in `MessageRenderer.java` at line 132. The `scrollToBottom()` method had a single-statement lambda body wrapped in unnecessary curly braces. Converted from block lambda (`() -> { stmt; }`) to expression lambda (`() -> stmt`). No logic was changed; the fix is purely cosmetic/style.
