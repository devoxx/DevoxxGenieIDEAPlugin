---
id: TASK-71
title: Fix java:S3776 in BacklogConfigService.java at line 425
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:45'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 71000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java`
- **Line:** 425
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 425 in `src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `BacklogConfigService.java:425` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

**What changed:** Extracted the inner per-file logic from `scanMaxId` into a new private helper method `extractNumericIdFromFile(Path file)`.

**Why:** The `scanMaxId` method had cognitive complexity of 19 (exceeding the 15 threshold) due to deeply nested try/catch, for, and if blocks. Extracting the file-reading and ID-parsing logic into a separate method reduces nesting in `scanMaxId` to well within the limit.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java`
  - `scanMaxId`: now delegates per-file work to `extractNumericIdFromFile`, replacing the nested try/if/if/try block with two lines
  - `extractNumericIdFromFile` (new): handles reading a file and parsing its numeric ID suffix, returning 0 if not found or on error

**Tests:** All 43 tests in `BacklogConfigServiceTest` pass, including the `scanMaxId_*` edge-case tests that exercise the extraction logic.

## Final Summary

Resolved SonarQube `java:S3776` ("Cognitive Complexity too high") in `BacklogConfigService.scanMaxId` (line 425) by extracting the deeply nested per-file logic into a new private method `extractNumericIdFromFile(Path)`.

**Before:** `scanMaxId` contained a for-loop whose body had a try-catch wrapping two nested `if` blocks and another inner try-catch, yielding cognitive complexity of 19.

**After:** `scanMaxId` delegates per-file work to `extractNumericIdFromFile`, reducing its complexity to ~8. The helper method itself has complexity ~8 — both well under the threshold of 15. No new issues were introduced; all 43 existing tests pass.
