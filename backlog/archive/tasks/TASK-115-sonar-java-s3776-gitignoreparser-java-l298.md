---
id: TASK-115
title: Fix java:S3776 in GitignoreParser.java at line 298
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
ordinal: 115000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 25 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java`
- **Line:** 298
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 25 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 298 in `src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `GitignoreParser.java:298` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `isPathIgnored` (line 298) which had cognitive complexity of 25 (over the allowed 15).

The original method had deeply nested loops:
- Two `for` loops iterating root include/exclude patterns with inner `if` checks (nesting penalty)
- An outer `for` loop over parent directories with null-checked inner `for` loops for nested patterns (triple nesting = +4 per inner `if`)

**Fix:** Extracted two new private helper methods to flatten the structure:
1. `matchesAnyPattern(String path, List<Pattern> patterns)` — replaces inline `for`+`if` pattern checks; complexity = 3
2. `checkNestedPatterns(String normalizedPath)` — handles the nested gitignore directory iteration; complexity = 7 (for loop + 2 ifs each with &&)

The refactored `isPathIgnored` now has cognitive complexity of 2 (two top-level `if` statements + method call), well under the threshold of 15.

No logic changes — all 12 existing `GitignoreParserTest` tests pass.

## Final Summary

**Problem:** `isPathIgnored` in `GitignoreParser.java` had a cognitive complexity of 25, exceeding SonarQube's S3776 threshold of 15. The nested structure of `for` loops inside `for` loops with null checks contributed heavily to the penalty.

**Solution:** Extracted two focused helper methods:
- `matchesAnyPattern(String, List<Pattern>)` — reusable single-loop pattern matcher (complexity 3)
- `checkNestedPatterns(String)` — handles nested gitignore lookup with early return (complexity 7)

The original `isPathIgnored` is now just 3 statements (2 `if` guards + 1 delegation call), reducing its complexity to 2.

**Files changed:**
- `src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java` — replaced 1 method with 3 methods; no logic changes

**Tests:** All 12 `GitignoreParserTest` tests pass (BUILD SUCCESSFUL).
