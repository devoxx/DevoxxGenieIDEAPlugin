---
id: TASK-55
title: Fix java:S1602 in FileEntryComponent.java at line 141
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
ordinal: 55000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/component/FileEntryComponent.java`
- **Line:** 141
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 141 in `src/main/java/com/devoxx/genie/ui/component/FileEntryComponent.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `FileEntryComponent.java:141` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Notes

- Modified `FileEntryComponent.java` line 141: removed useless curly braces from the lambda body in `openFileInEditor()`.
- Changed `() -> { FileEditorManager.getInstance(project).openFile(file, true); }` to `() -> FileEditorManager.getInstance(project).openFile(file, true)` (expression lambda, no braces needed for single statement).

## Final Summary

Fixed SonarQube rule `java:S1602` in `FileEntryComponent.java` at line 141. The `openFileInEditor()` method's `invokeLater` lambda had unnecessary curly braces around a single statement. Converted from a block lambda to an expression lambda by removing the braces and the trailing semicolon inside. No logic was changed; this is a pure style fix.
