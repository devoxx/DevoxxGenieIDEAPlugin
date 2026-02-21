---
id: TASK-154
title: 'Fix java:S3776 in GitignoreParser.java at line 298'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 11:09'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java`
- **Line:** 298
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 25 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 298 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 25 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `GitignoreParser.java:298` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation Notes

### Root cause
The `isPathIgnored` method in the worktree copy `.claude/worktrees/sunny-exploring-lemon/src/main/java/.../GitignoreParser.java` had Cognitive Complexity of 25 due to three nested loop structures:
- Two sequential `for` loops over `rootIncludePatterns` / `rootExcludePatterns` (each with an `if` inside: +2 each)
- One outer `for` loop over `parentDirs` containing two nested `if`/`for`/`if` combinations (+9 total)

### Fix applied
Extracted three private helper methods, matching the already-refactored main source file (committed in ad60f170):
1. `matchesAnyPattern(String path, List<Pattern> patterns)` — iterates and returns first match
2. `checkNestedPatterns(String normalizedPath)` — checks nested .gitignore include/exclude patterns
3. `isPathIgnored` simplified to 2 plain `if` checks + delegate to `checkNestedPatterns`, reducing complexity to ~2

### Files modified
- `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java` — refactored `isPathIgnored`, added `matchesAnyPattern` and `checkNestedPatterns`
- `src/test/java/com/devoxx/genie/service/analyzer/util/GitignoreParserTest.java` — added two new tests:
  - `shouldIgnore_negationPattern_unignoresSpecificFile` — verifies rootIncludePatterns (negation `!` patterns) take precedence over rootExcludePatterns
  - `shouldIgnore_negationPatternOnDirectory_unignoresDirectory` — verifies directory negation patterns coexist with directory exclusion

### Test results
All 14 `GitignoreParserTest` tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` (Cognitive Complexity > 15) in `GitignoreParser.java` at line 298.\n\n### Problem\nThe `isPathIgnored` method had Cognitive Complexity of 25, caused by three nested loop structures inline:\n- Two `for` loops each containing an `if` (+4)\n- One outer `for` with two nested `if`/`for`/`if` blocks inside (+21)\n\nThe main source file was already fixed in commit `ad60f170`. The worktree copy at `.claude/worktrees/sunny-exploring-lemon/...` still had the original complex version.\n\n### Changes\n\n**`.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java`**\n- Replaced the complex `isPathIgnored` body with calls to two new private helpers\n- Added `matchesAnyPattern(String, List<Pattern>)` — single-responsibility loop over patterns\n- Added `checkNestedPatterns(String)` — handles nested `.gitignore` include/exclude logic\n- Resulting `isPathIgnored` has Cognitive Complexity of ~2\n\n**`src/test/java/com/devoxx/genie/service/analyzer/util/GitignoreParserTest.java`**\n- Added `shouldIgnore_negationPattern_unignoresSpecificFile` — verifies `rootIncludePatterns` (negation `!`) takes precedence over `rootExcludePatterns`\n- Added `shouldIgnore_negationPatternOnDirectory_unignoresDirectory` — verifies directory negation patterns work alongside directory exclusions\n\n### Test Results\nAll 14 `GitignoreParserTest` tests pass (BUILD SUCCESSFUL).
<!-- SECTION:FINAL_SUMMARY:END -->
