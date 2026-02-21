---
id: TASK-113
title: Fix java:S3776 in FindSymbolsToolExecutor.java at line 53
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:58'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 113000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java`
- **Line:** 53
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 53 in `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `FindSymbolsToolExecutor.java:53` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

This task is a duplicate of TASK-106, which fixed the same issue. The fix was already applied to the working tree.

**File modified:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java`

**Root cause:** The `findSymbols` method had a deeply nested lambda passed to `processElementsWithWord` with 4 levels of `if` nesting, giving cognitive complexity of 19 (limit 15).

**Fix:** Extracted the lambda body to a new private method `processFoundElement(PsiElement, List<String>, String, VirtualFile)` using guard clauses (early returns) to flatten nesting. The lambda is now a single method-reference call with no nested control flow.

- `findSymbols` complexity reduced from 19 → 4
- `processFoundElement` complexity: 6
- Both well under the 15 limit

## Final Summary

Duplicate of TASK-106. The fix was already implemented: extracted the `processElementsWithWord` lambda body in `findSymbols` (line 53) into a new private method `processFoundElement`, using guard clauses to replace nested `if` blocks. Cognitive complexity reduced from 19 to 4. No behaviour changes. No new SonarQube issues. All existing tests continue to pass.
