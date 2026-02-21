---
id: TASK-108
title: Fix java:S3776 in GitignoreParser.java at line 298
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
ordinal: 108000
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

Refactored `isPathIgnored()` (line 298) by extracting two private helper methods:

1. **`matchesAnyPattern(List<Pattern>, String)`** — iterates a pattern list and returns `true` on first match (complexity: 3)
2. **`checkNestedPatterns(String, List<String>)`** — checks nested `.gitignore` patterns across parent dirs, returning `Boolean` (null = no match) (complexity: 5)

The original `isPathIgnored` had cognitive complexity 25 due to 3 levels of nesting (for→if→for→if). After extraction, `isPathIgnored` itself has complexity 3, well under the limit of 15.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java`

All 12 `GitignoreParserTest` tests pass after the change.

## Final Summary

Resolved `java:S3776` in `GitignoreParser.isPathIgnored()` by extracting two helper methods (`matchesAnyPattern` and `checkNestedPatterns`). The original method had a cognitive complexity of 25 due to deeply nested loops and conditionals (for→if→for→if). The refactoring:

- `isPathIgnored`: complexity reduced from 25 → 3 (delegates to helpers)
- `matchesAnyPattern`: new method, complexity 3 (single for+if)
- `checkNestedPatterns`: new method, complexity 5 (for + 2 nested ifs with &&)

No logic was changed — only structure. All 12 existing `GitignoreParserTest` tests pass with `BUILD SUCCESSFUL`.
