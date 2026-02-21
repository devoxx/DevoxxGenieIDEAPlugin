---
id: TASK-57
title: Fix java:S1602 in FileManager.java at line 81
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
ordinal: 57000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/service/generator/file/FileManager.java`
- **Line:** 81
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 81 in `src/main/java/com/devoxx/genie/service/generator/file/FileManager.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `FileManager.java:81` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass (no FileManager tests exist; pre-existing NodeProcessor.java build error is unrelated)

## Implementation Notes

Changed `saveContent()` in `FileManager.java`: removed useless curly braces from the outer `invokeLater(() -> { ... })` lambda. The body contained a single statement (`runWriteAction(...)`), so the braces are unnecessary per java:S1602. The redundant `// Run the write action` comment inside the lambda was also removed. No new issues introduced; the change is purely stylistic.

## Final Summary

Fixed SonarQube rule `java:S1602` ("Remove useless curly braces around statement") in `FileManager.java` `saveContent()` method (original line 81). The outer `invokeLater` lambda had a block body `() -> { runWriteAction(...); }` with a single statement; replaced with the expression form `() -> runWriteAction(...)`. No tests exist for `FileManager`; the pre-existing compilation error in `NodeProcessor.java` is unrelated to this fix.
