---
id: TASK-76
title: Fix java:S3776 in CliTaskExecutorService.java at line 66
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:46'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 76000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`
- **Line:** 66
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 66 in `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `CliTaskExecutorService.java:66` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `execute()` (line 66) by extracting three private helper methods:

1. **`resolveCliType(CliToolConfig)`** — Handles the ternary + for-loop + nested-if logic for auto-detecting
   the CLI type from the tool name (backwards compat for CUSTOM type). Removed the `break` by using early-return
   style, which is cleaner and avoids an extra complexity point.

2. **`runCliProcess(CliToolConfig, CliCommand, String, String, List<String>, String, CliConsoleManager)`** —
   Contains the entire try/catch block that was previously inside the `executeOnPooledThread` lambda. This is the
   biggest reduction: the lambda body itself contributed nesting penalties to `execute`.

3. **`notifyProcessExit(String, int, boolean, long, List<String>, CliConsoleManager)`** — Extracts the
   `invokeLater` block that dispatches exit-code notifications to EDT, removing the nested if/else from
   `runCliProcess`.

**Complexity breakdown after refactoring:**
- `execute()`: ≤ 3 (straight-line setup + method calls)
- `resolveCliType()`: ≈ 6 (ternary, if, for, nested-if)
- `runCliProcess()`: ≈ 8 (two catch blocks, two ifs, one &&)
- `notifyProcessExit()`: ≈ 4 (if/else + || operator)

All well under the 15-point limit.

## Final Summary

Fixed java:S3776 in `CliTaskExecutorService.java` by decomposing the monolithic `execute()` method (cognitive
complexity 26) into four focused methods:

- `execute()` — orchestration only; delegates to helpers
- `resolveCliType()` — CLI type resolution with backwards-compat auto-detection
- `runCliProcess()` — process lifecycle (start, stream I/O, wait, handle errors)
- `notifyProcessExit()` — EDT notification on process completion or failure

No behaviour was changed. All tests pass (`./gradlew test` — BUILD SUCCESSFUL). No new SonarQube patterns introduced.
