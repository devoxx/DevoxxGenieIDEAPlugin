---
id: TASK-138
title: 'Fix java:S3776 in SpecTaskRunnerService.java at line 821'
status: Done
assignee: []
created_date: '2026-02-21 09:48'
updated_date: '2026-02-21 10:05'
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
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`
- **Line:** 821
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 821 in `src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`.
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
Refactored `reportLayerSummary` in `SpecTaskRunnerService.java` to reduce cognitive complexity from 26 to 9.

Root cause: The original method had a double-nested if/else chain (if result != null → if result.completed / else if result.skipped / else), plus three ternary operators at nesting depth 2-3, plus a cliMode if with a lambda containing a catch.

Changes made:
1. `SpecTaskRunnerService.java`: Flattened the double-nested if-else chain to a flat if/else-if/else-if structure (inverting the null check to avoid extra nesting level). Hoisted ternary operators (`tid`, `title`) before the branching structure so they run at nesting=1 instead of 2-3. Extracted `formatSkipLine(String, String, String)` as a package-private static helper for skip-line formatting. Extracted `printToCliConsole(String)` as a private helper for the CLI console printing logic with early return instead of nested if.
2. `SpecTaskRunnerServiceTest.java`: Added 3 new unit tests for `formatSkipLine` covering: non-null skip reason, null skip reason (falls back to 'unknown'), and empty title.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed java:S3776 in `SpecTaskRunnerService.java:821` by reducing cognitive complexity of `reportLayerSummary` from 26 to 9 (well below the 15 threshold).\n\n## Root Cause\nThe method had a double-nested if/else chain combined with three ternary operators at nesting depths 2-3, and a CLI console lambda with a catch block — totalling 26 cognitive complexity points.\n\n## Changes\n\n### `src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`\n- **Flattened if/else chain**: Inverted the `result == null` check to eliminate one nesting level, restructuring `if (result != null) { if (...) else if (...) } else { ... }` into a flat `if/else if/else if` chain.\n- **Hoisted ternary operators**: Moved `tid` and `title` ternary expressions to before the branching block so they execute at nesting=1 instead of nesting=2-3.\n- **Extracted `formatSkipLine`**: New package-private static helper method formats the `[SKIP]` line with a `skipReason != null` check at nesting=0.\n- **Extracted `printToCliConsole`**: New private method handles the CLI console printing with an early-return guard, moving the lambda/catch complexity out of `reportLayerSummary`.\n\n### `src/test/java/com/devoxx/genie/service/spec/SpecTaskRunnerServiceTest.java`\n- Added 3 unit tests for the new `formatSkipLine` helper: non-null reason, null reason (falls back to `\"unknown\"`), and empty title.\n\n## Complexity Breakdown (refactored `reportLayerSummary`)\n- `for` loop: +1\n- ternary `tid`: +2 (nesting=1)\n- ternary `title`: +2 (nesting=1)\n- `if (result == null)`: +2 (nesting=1)\n- `else if (result.completed)`: +1\n- `else if (result.skipped)`: +1\n- **Total: 9** (was 26)\n\n## Tests\nAll existing tests pass. 3 new tests added for `formatSkipLine`."
<!-- SECTION:FINAL_SUMMARY:END -->
