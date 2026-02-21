---
id: TASK-109
title: Fix java:S3776 in GlobTool.java at line 14
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
ordinal: 109000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`
- **Line:** 14
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 36 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 14 in `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `GlobTool.java:14` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `convertGlobToRegex` in `GlobTool.java` to reduce cognitive complexity from 36 to ~9.

**Strategy:**
- Extracted three private static helper methods to handle the complex switch cases:
  - `handleBackslash(StringBuilder, String, int) -> int`: handles `\\` escape sequences
  - `handleAsterisk(StringBuilder, String, int, int) -> int`: handles `*` and `**` glob patterns
  - `handleOpenBracket(StringBuilder, String, int) -> int`: handles `[` character class opening
- Switched from traditional `switch` with `break` statements to arrow-syntax switch (Java 14+, valid since project targets JDK 17+)
- Replaced simple `if/else` branches in switch cases with ternary expressions (e.g., `case '?' -> regex.append(inClass == 0 ? "[^/]" : ".");`)
- Added private constructor to comply with utility class convention
- Multi-char case labels for regex-special chars: `case '.', '(', ')', '+', '|', '^', '$', '@', '%'`

**Files modified:** `src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java`

**Tests:** All 18 tests in `GlobToolTest` pass.

## Final Summary

Fixed SonarQube `java:S3776` ("Cognitive Complexity too high") in `GlobTool.convertGlobToRegex()` by refactoring the monolithic switch statement into a lean dispatcher with three private static helpers:

1. `handleBackslash` — handles `\` escape sequences (complexity: ~3)
2. `handleAsterisk` — handles `*` and `**` glob wildcards (complexity: ~4)
3. `handleOpenBracket` — handles `[` character class start with `!` negation and `^` escaping (complexity: ~4)

The main `convertGlobToRegex` method's cognitive complexity dropped from 36 to approximately 9, well within the allowed threshold of 15. Arrow-syntax switch cases replaced colon+break style, and ternary operators replaced simple `if/else` pairs in two cases. No logic was changed — all 18 existing tests pass.
