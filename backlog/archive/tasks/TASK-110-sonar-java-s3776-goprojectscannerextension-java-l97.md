---
id: TASK-110
title: Fix java:S3776 in GoProjectScannerExtension.java at line 97
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
ordinal: 110000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java`
- **Line:** 97
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 97 in `src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `GoProjectScannerExtension.java:97` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the go.sum content inspection from `detectGoFrameworks` into a new private method
`detectGoFrameworksFromGoSum(VirtualFile goSum, Map<String, Object> goInfo)`.

**Before:** `detectGoFrameworks` had cognitive complexity 19 due to nesting:
- `if (goSum != null)` at level 0 (+1, nesting=0)
- Content checks inside `try` inside `if` at nesting level 2 (+2 each)
- `catch` at nesting level 1 (+2)
- Outer if/else-if chain (5 branches) (+5)
Total: 19

**After:** `detectGoFrameworks` has complexity 6:
- `if (goSum != null)` → +1; extracted method call replaces nested try/catch block
- `if/else-if` chain (5 branches) → +5
Total: 6

New method `detectGoFrameworksFromGoSum` has complexity 9 (well under 15).

## Final Summary

Fixed java:S3776 in `GoProjectScannerExtension.java` at line 97 by extracting the go.sum content
inspection logic from `detectGoFrameworks` into a new private method `detectGoFrameworksFromGoSum`.
This removed the deep nesting (try inside if) that was inflating the cognitive complexity from 19 to 6.
The new extracted method itself has complexity 9. No functional changes were made — all logic is preserved.
