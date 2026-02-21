---
id: TASK-60
title: Fix java:S1602 in MessageRenderer.java at line 193
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
ordinal: 60000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/panel/conversation/MessageRenderer.java`
- **Line:** 193
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 193 in `src/main/java/com/devoxx/genie/ui/panel/conversation/MessageRenderer.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `MessageRenderer.java:193` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Notes

Changed the lambda in `onFileReferencesAvailable()` at line 193 from block form (with curly braces) to expression form:

```java
// Before
ApplicationManager.getApplication().invokeLater(() -> {
    webViewController.addFileReferences(chatMessageContext, files);
});

// After
ApplicationManager.getApplication().invokeLater(() ->
    webViewController.addFileReferences(chatMessageContext, files));
```

File modified: `src/main/java/com/devoxx/genie/ui/panel/conversation/MessageRenderer.java`

## Final Summary

Resolved SonarQube rule `java:S1602` ("Lambdas should not contain only one statement") at line 193 in `MessageRenderer.java`. The fix removes the unnecessary curly braces around the single-statement lambda body in `onFileReferencesAvailable()`, converting the block lambda to an expression lambda. No logic was changed — only the syntax was simplified. No new issues introduced.
