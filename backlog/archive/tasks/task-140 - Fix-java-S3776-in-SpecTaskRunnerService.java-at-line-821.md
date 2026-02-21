---
id: TASK-140
title: 'Fix java:S3776 in SpecTaskRunnerService.java at line 821'
status: Done
assignee: []
created_date: '2026-02-21 09:48'
updated_date: '2026-02-21 10:10'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`
- **Line:** 821
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 821 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `SpecTaskRunnerService.java:821` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed S3776 in SpecTaskRunnerService.java by refactoring reportLayerSummary (line 821 in HEAD). The original method had cognitive complexity ~26 due to: (1) deeply nested if-else structure checking result.completed/skipped inside if(result != null), (2) ternary operators for title inside nested method calls, (3) inline if(cliMode) with lambda + try/catch for CLI console printing.

Fix approach (already applied to working tree before task was started):
1. Flattened if-else from nested if(result != null){if...else if...} else{...} → flat if(result == null){...} else if(result.completed){...} else if(result.skipped){...}
2. Extracted title variable before the conditional chain to avoid ternary nesting
3. Extracted skip line format into static helper formatSkipLine(tid, title, skipReason)
4. Extracted CLI console printing into private printToCliConsole(text) method

New cognitive complexity of reportLayerSummary: ~7 (well under the 15 limit).

Modified files: src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java, src/test/java/com/devoxx/genie/service/spec/SpecTaskRunnerServiceTest.java

Tests: 37 tests pass including 3 new formatSkipLine tests (formatSkipLine_withSkipReason_includesReason, formatSkipLine_withNullSkipReason_usesUnknown, formatSkipLine_withEmptyTitle_doesNotThrow).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed SonarQube `java:S3776` (Cognitive Complexity too high) in `SpecTaskRunnerService.java` for the `reportLayerSummary` method.\n\n### Root Cause\n\nThe original `reportLayerSummary` method had a cognitive complexity of ~26 (limit is 15) due to:\n1. **Nested if-else structure**: `if (result != null) { if (result.completed) {...} else if (result.skipped) {...} } else {...}` — deeply nested conditionals\n2. **Ternary operators nested in method calls**: `task.getTitle() != null ? task.getTitle() : \"\"` used inline inside `String.format()` calls\n3. **Inline CLI console logic**: An `if (cliMode)` block containing a lambda with a try/catch block, all inside the reporting method\n\n### Fix\n\nRefactored the method by:\n1. **Flattened the if-else chain**: Inverted the outer check to `if (result == null)` and made the remaining branches flat `else if` clauses\n2. **Extracted `title` variable**: Pre-computed the null-safe title before the conditional chain to eliminate nested ternaries\n3. **Extracted `formatSkipLine()` static helper**: Isolated the skip-line string formatting, removing the ternary for `skipReason` from the main method\n4. **Extracted `printToCliConsole()` private helper**: Moved the CLI-mode check, lambda, and try/catch into a dedicated method\n\nThe refactored `reportLayerSummary` now has a cognitive complexity of ~7.\n\n### Files Modified\n\n- `src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java` — refactored `reportLayerSummary`, added `formatSkipLine()` and `printToCliConsole()` helpers\n- `src/test/java/com/devoxx/genie/service/spec/SpecTaskRunnerServiceTest.java` — added 3 unit tests for the new `formatSkipLine()` helper\n\n### Test Results\n\nAll 37 tests in `SpecTaskRunnerServiceTest` pass, including the 3 new tests:\n- `formatSkipLine_withSkipReason_includesReason`\n- `formatSkipLine_withNullSkipReason_usesUnknown`\n- `formatSkipLine_withEmptyTitle_doesNotThrow`
<!-- SECTION:FINAL_SUMMARY:END -->
