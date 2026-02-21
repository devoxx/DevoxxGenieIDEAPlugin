---
id: TASK-153
title: 'Fix java:S3776 in FindDefinitionToolExecutor.java at line 102'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 11:01'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java`
- **Line:** 102
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 102 in `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `FindDefinitionToolExecutor.java:102` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `resolveAtPosition` in FindDefinitionToolExecutor.java by extracting two private helper methods:
- `resolveAtColumn(PsiFile, int, int)` — handles Strategy 1 (exact column lookup)
- `resolveBySearchingLine(PsiFile, int, String)` — handles Strategy 2 (line walk)

Cognitive complexity reduced from 21 → ~3 in `resolveAtPosition`, ~3 in `resolveAtColumn`, ~10 in `resolveBySearchingLine` (all well under 15).

Created new test file: `src/test/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutorTest.java` with 7 tests covering input validation and exception handling. All tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` (Cognitive Complexity > 15) in `FindDefinitionToolExecutor.java` at line 102.\n\n### What changed\n\n**`src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java`**\n\nThe `resolveAtPosition` method had a cognitive complexity of 21 due to nested `if` blocks inside an `if (column > 0)` guard and a `for` loop with multiple conditions. Extracted two private helper methods:\n\n- `resolveAtColumn(PsiFile, int line, int column)` — implements Strategy 1: looks up the element at the exact `(line, column)` offset and resolves it. Complexity: ~3.\n- `resolveBySearchingLine(PsiFile, int line, String symbol)` — implements Strategy 2: walks all elements on the line and returns the first resolvable reference. Complexity: ~10.\n\n`resolveAtPosition` now just calls these two helpers and falls back to Strategy 3 (named element lookup), reducing its complexity to ~3.\n\n**`src/test/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutorTest.java`** (new file)\n\nAdded 7 unit tests covering:\n- Missing/blank `file` parameter → returns error\n- Missing/zero/negative `line` parameter → returns error\n- Invalid JSON arguments → returns error\n- `ReadAction.compute()` throwing a runtime exception → returns graceful error message\n\nAll 7 tests pass."]
<!-- SECTION:FINAL_SUMMARY:END -->
