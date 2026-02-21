---
id: TASK-86
title: Fix java:S3776 in TokenCalculationService.java at line 53
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
ordinal: 86000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 43 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/TokenCalculationService.java`
- **Line:** 53
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 43 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 53 in `src/main/java/com/devoxx/genie/service/TokenCalculationService.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `TokenCalculationService.java:53` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `showOnlyScanInfo` (line 53) by extracting 4 focused static helper methods:
- `formatTokenCount(int)` – formats token counts below 1000 as exact, above as K notation
- `buildScanBaseMessage(VirtualFile, ModelProvider, ScanContentResult, String)` – builds the base directory/token message
- `appendSkippedFileInfo(StringBuilder, ScanContentResult)` – appends skipped extension/dir info to message
- `collectSkippedExtensions(Map<String,String>)` – collects extension names from skipped-file reasons
- `collectSkippedDirectories(Map<String,String>)` – collects directory names from skipped-file reasons

The `showOnlyScanInfo` method body is now only 4 lines inside the lambda. Each helper stays well under cognitive complexity 15.
All 13 existing tests in `TokenCalculationServiceTest` pass with no modifications.

## Final Summary

**Problem:** `showOnlyScanInfo` at line 53 had cognitive complexity of 43 (limit 15) due to deeply nested conditionals and loops inside a `thenAccept` lambda.

**Fix:** Extracted 5 private static helper methods, reducing `showOnlyScanInfo` to a trivial 4-line orchestrating method. The helpers each have complexity well below 15:
- `formatTokenCount` – complexity 1
- `buildScanBaseMessage` – complexity 1
- `appendSkippedFileInfo` – complexity ~4
- `collectSkippedExtensions` – complexity ~5
- `collectSkippedDirectories` – complexity ~6

**Files modified:** `src/main/java/com/devoxx/genie/service/TokenCalculationService.java`

**Tests:** All 13 tests in `TokenCalculationServiceTest` pass. No new SonarQube issues introduced (all helpers are focused, simple methods).
