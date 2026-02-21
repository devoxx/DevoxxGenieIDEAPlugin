---
id: TASK-82
title: Fix java:S3776 in FileEntryComponent.java at line 151
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:47'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 82000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/component/FileEntryComponent.java`
- **Line:** 151
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 151 in `src/main/java/com/devoxx/genie/ui/component/FileEntryComponent.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `FileEntryComponent.java:151` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `openFileWithSelectedCode` in `FileEntryComponent.java:151` to reduce cognitive complexity from 17 to well below 15.

Extracted two private helper methods:
- `openImageVirtualFile(Project, VirtualFile)` — handles opening image files via VirtualFileManager
- `openCodeFileWithHighlight(Project, VirtualFile)` — handles opening code files with optional snippet highlight

The original method now only contains: lambda + try/catch + if/else dispatch, bringing its cognitive complexity to ~4-5. All tests pass (BUILD SUCCESSFUL).

## Final Summary

**What changed:** `src/main/java/com/devoxx/genie/ui/component/FileEntryComponent.java`

The `openFileWithSelectedCode` method had a cognitive complexity of 17 (limit is 15) due to deeply nested control flow: a lambda wrapping a try/catch with two nested if/else branches, one of which contained a further two levels of nesting.

**Fix:** Extracted the two branches into dedicated private methods:
1. `openImageVirtualFile` — resolves the canonical path and opens the image file via `FileEditorManagerEx`
2. `openCodeFileWithHighlight` — retrieves the original file user data, opens it, then conditionally highlights selected text

After extraction, `openFileWithSelectedCode` contains only the lambda, try/catch, and a single if/else dispatch — well within the allowed complexity. No logic was changed, only restructured. All existing tests continue to pass.
