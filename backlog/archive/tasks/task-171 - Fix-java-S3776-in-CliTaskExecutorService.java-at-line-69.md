---
id: TASK-171
title: 'Fix java:S3776 in CliTaskExecutorService.java at line 69'
status: Done
assignee: []
created_date: '2026-02-21 12:33'
updated_date: '2026-02-21 12:42'
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
- **File:** `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`
- **Line:** 69
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 28 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 69 in `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 28 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `CliTaskExecutorService.java:69` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `execute()` method (line 69) in `CliTaskExecutorService.java` to reduce cognitive complexity from 28 to ~4.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java`
- `src/test/java/com/devoxx/genie/service/cli/CliTaskExecutorServiceTest.java`

**Changes made:**
1. Extracted `resolveCliType(CliToolConfig)` (package-private): isolates the CLI type auto-detection logic (ternary + if + for + nested-if). Removes ~7 complexity points from `execute()`. Uses early returns instead of mutating a local variable.
2. Extracted `runCliTaskAsync(...)` (private): moves the entire pooled-thread lambda body into its own method. Removes ~17 complexity points from `execute()`. The extracted method has a complexity of ~14, safely under the 15 limit.
3. Added 5 new unit tests for `resolveCliType()` covering: explicit non-CUSTOM type, case-insensitive name matching, unknown name stays CUSTOM, and null type fallback.

All 23 tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Resolved java:S3776 in `CliTaskExecutorService.execute()` (line 69) by extracting two private helper methods:\n\n1. **`resolveCliType(CliToolConfig)`** — Encapsulates CLI type auto-detection logic. Uses early returns to avoid mutating a local variable through a for-loop, making the intent clearer. Complexity: ~8.\n\n2. **`runCliTaskAsync(...)`** — Moves the entire pooled-thread lambda body out of `execute()`. This is where most of the original complexity lived (try/catch, nested lambdas, conditional chains). Complexity: ~14.\n\nAfter refactoring, `execute()` itself has a cognitive complexity of ~4, well under the 15 limit.\n\nAdded 5 new unit tests for `resolveCliType()` to cover all branching scenarios. All 23 tests (18 pre-existing + 5 new) pass.
<!-- SECTION:FINAL_SUMMARY:END -->
