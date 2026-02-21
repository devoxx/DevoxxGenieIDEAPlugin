---
id: TASK-97
title: Fix java:S3776 in PythonProjectScannerExtension.java at line 21
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:50'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 97000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/python/PythonProjectScannerExtension.java`
- **Line:** 21
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 21 in `src/main/java/com/devoxx/genie/service/analyzer/languages/python/PythonProjectScannerExtension.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `PythonProjectScannerExtension.java:21` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the 4 linting/formatting tool detection `if` statements from `enhanceProjectInfo` into a new private helper method `detectLintingTools(VirtualFile baseDir, Map<String, Object> pythonInfo)`.

**File modified:** `src/main/java/com/devoxx/genie/service/analyzer/languages/python/PythonProjectScannerExtension.java`

**What changed:**
- The 4 consecutive `if` statements that detected flake8, black, mypy, and isort (lines 80-83 in original) were extracted into a new `detectLintingTools()` helper method.
- In `enhanceProjectInfo`, replaced those 4 statements with a single call: `detectLintingTools(baseDir, pythonInfo)`.

**Complexity reduction:**
- Before: 19 (4 flat `if` statements, each +1, contributing 4 points)
- After: 15 (exactly at the allowed limit)
- The extracted method itself has complexity 4, which is well within limits.

**No new SonarQube issues:** The new `detectLintingTools` method has cognitive complexity of 4, well below the threshold of 15.

## Final Summary

Resolved SonarQube java:S3776 in `PythonProjectScannerExtension.java` by extracting 4 consecutive linting-tool detection `if` statements from the `enhanceProjectInfo` method into a new private `detectLintingTools(VirtualFile, Map)` helper method. This reduced the cognitive complexity of `enhanceProjectInfo` from 19 to exactly 15 (the allowed maximum). The new helper method has a complexity of 4, introducing no new violations. All existing behavior is preserved — the linting tool detection logic is identical, just moved to a dedicated method.
