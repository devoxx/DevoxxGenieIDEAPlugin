---
id: TASK-104
title: Fix java:S3776 in FindDefinitionToolExecutor.java at line 102
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:48'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 104000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java`
- **Line:** 102
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 102 in `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `FindDefinitionToolExecutor.java:102` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted two helper methods from `resolveAtPosition` to reduce its cognitive complexity from 21 to ~6:

- `resolveAtExactPosition(psiFile, line, column)` — handles Strategy 1 (exact column offset lookup)
- `resolveBySearchingLine(psiFile, startOffset, endOffset, symbol)` — handles Strategy 2 (linear scan of line elements)

The `resolveAtPosition` method now delegates to these helpers, keeping its complexity well under the 15 threshold. Also improved the non-matching element skip in `resolveBySearchingLine` to skip to end of element even when the symbol doesn't match (minor efficiency improvement, same behaviour).

## Final Summary

**File modified:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java`

Refactored `resolveAtPosition` (line 102) by extracting two private helper methods:

1. `resolveAtExactPosition` — resolves symbol at the exact file offset given a line+column, with cognitive complexity of 2.
2. `resolveBySearchingLine` — walks elements on the line to find the first resolvable reference, with cognitive complexity of 8.

The original `resolveAtPosition` now has a cognitive complexity of ~6 (down from 21), satisfying the `java:S3776` rule. No new SonarQube issues were introduced. All existing tests pass (BUILD SUCCESSFUL).
