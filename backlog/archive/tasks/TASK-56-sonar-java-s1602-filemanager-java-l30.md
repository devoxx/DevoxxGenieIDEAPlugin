---
id: TASK-56
title: Fix java:S1602 in FileManager.java at line 30
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
ordinal: 56000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/service/generator/file/FileManager.java`
- **Line:** 30
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 30 in `src/main/java/com/devoxx/genie/service/generator/file/FileManager.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `FileManager.java:30` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Changed the `invokeLater` lambda in `writeFile()` from a block lambda to an expression lambda, removing the unnecessary curly braces around the single `runWriteAction(...)` call. Also removed the comment `// Run the write action on EDT` that was inside the now-removed block.

**File modified:** `src/main/java/com/devoxx/genie/service/generator/file/FileManager.java`

Before (lines 30-48):
```java
ApplicationManager.getApplication().invokeLater(() -> {
    // Run the write action on EDT
    ApplicationManager.getApplication().runWriteAction(() -> {
        ...
    });
});
```

After (lines 30-47):
```java
ApplicationManager.getApplication().invokeLater(() ->
    ApplicationManager.getApplication().runWriteAction(() -> {
        ...
    }));
```

## Final Summary

Fixed SonarQube rule `java:S1602` ("Lambdas containing only one statement should not have curly braces") in `FileManager.java` at line 30. The `invokeLater` lambda contained a single `runWriteAction(...)` call wrapped in unnecessary curly braces. Converted it to an expression lambda by removing the block braces and adjusting the closing parentheses. The inner `runWriteAction` lambda retains its block form since it contains multiple statements (try-catch with if-else). No logic was changed.
