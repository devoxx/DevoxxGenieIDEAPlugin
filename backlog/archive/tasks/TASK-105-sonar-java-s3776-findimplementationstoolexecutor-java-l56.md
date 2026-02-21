---
id: TASK-105
title: Fix java:S3776 in FindImplementationsToolExecutor.java at line 56
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
ordinal: 105000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindImplementationsToolExecutor.java`
- **Line:** 56
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 56 in `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindImplementationsToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `FindImplementationsToolExecutor.java:56` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the inner for-loop body of `findImplementations` into a new private helper method `formatImplementationEntry(PsiElement, VirtualFile)`. This removed 3 complexity contributors from the loop body (null check + 2 ternary operators at nesting depth 1), reducing `findImplementations` cognitive complexity from 17 to ~13.

Files modified:
- `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindImplementationsToolExecutor.java`
  - Added `import org.jetbrains.annotations.Nullable`
  - Replaced the inline loop body (null check + 2 ternaries) with a call to the new helper
  - Added `@Nullable private String formatImplementationEntry(PsiElement, VirtualFile)` helper method
  - In the helper, merged the two separate `instanceof PsiNameIdentifierOwner` ternaries into a single if/else block (also eliminates double instanceof check at runtime)

## Final Summary

**Problem:** The `findImplementations` method had a SonarQube S3776 cognitive complexity of 17, exceeding the allowed maximum of 15. The complexity came from: 3 guard-clause ifs, 1 ternary inside a nested if, 1 for loop containing 2 nested ifs (break/continue), 2 ternaries at nesting depth 1, a second for loop, and a trailing if — totaling 17.

**Fix:** Extracted the for-loop body into a new `@Nullable formatImplementationEntry(PsiElement, VirtualFile)` helper method. This moved the `null` location check and the two `instanceof PsiNameIdentifierOwner` ternary expressions out of `findImplementations`. The helper also consolidates both `instanceof` checks into a single if/else block. Result: `findImplementations` complexity drops to ~13; `formatImplementationEntry` has complexity ~3.

**No behavioral change** — the logic is identical, just restructured for readability and SonarQube compliance.
