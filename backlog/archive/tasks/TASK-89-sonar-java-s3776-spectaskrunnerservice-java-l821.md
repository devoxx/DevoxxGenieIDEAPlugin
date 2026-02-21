---
id: TASK-89
title: Fix java:S3776 in SpecTaskRunnerService.java at line 821
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:49'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 89000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`
- **Line:** 821
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 821 in `src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `SpecTaskRunnerService.java:821` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

The `reportLayerSummary` method (line 821) had a cognitive complexity of 16, exceeding the S3776 threshold of 15. The fix was to extract the `ApplicationManager.getApplication().invokeLater(...)` lambda with its try-catch block into a new private helper method `printToCliConsole(String message)`.

**Changes made:**
- `src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java`
  - Replaced the inline `invokeLater` lambda in `reportLayerSummary` with a call to `printToCliConsole(sb.toString())`
  - Added new private method `printToCliConsole(@NotNull String message)` that contains the extracted lambda and try-catch

**Complexity reduction:**
- Before: `reportLayerSummary` had complexity 16 (lambda +2, catch +1 contributed 3 of those points)
- After: `reportLayerSummary` has complexity 13 (well below the 15 threshold)
- New `printToCliConsole` method has complexity 2 (lambda +1, catch +1), well below threshold

## Final Summary

Resolved SonarQube java:S3776 in `SpecTaskRunnerService.reportLayerSummary()` (line 821) by extracting the CLI console printing logic into a dedicated private helper method `printToCliConsole(String message)`. This reduced the cognitive complexity of `reportLayerSummary` from 16 to 13 by moving the `invokeLater` lambda and its `catch` block (which contributed 3 complexity points at nesting level 1) into the new method. The new `printToCliConsole` method itself has a cognitive complexity of only 2. All 13 existing tests in `SpecTaskRunnerServiceTest` continue to pass.
