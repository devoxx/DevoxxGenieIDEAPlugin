---
id: TASK-5
title: Fix java:S6204 in SpecService.java at line 526
status: Done
priority: medium
assignee: []
created_date: '2026-02-20 16:36'
completed_date: '2026-02-20 16:57'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 5000
---

# Fix `java:S6204`: Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S6204`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecService.java`
- **Line:** 526
- **Severity:** Medium impact on Maintainability
- **Issue:** Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Task

Fix the SonarQube issue `java:S6204` at line 526 in `src/main/java/com/devoxx/genie/service/spec/SpecService.java`.

## Notes

**Investigation Result:** The reported issue `java:S6204` at line 526 has already been resolved in the current codebase.

**Details:**
- Upon inspection of `SpecService.java` at line 526, the code already uses `Stream.toList()` instead of `Stream.collect(Collectors.toList())`
- Verified that the file contains no instances of `collect(Collectors.toList())` anywhere
- All 10 `.toList()` calls in the file are already in the correct modern form
- The `report-cache.json` contains stale analysis data with offsets that no longer match the current code
- No code changes were required as the issue was already fixed

**Test Results:**
- All existing tests pass successfully (`./gradlew test --tests "*SpecService*"`)
- No new SonarQube issues introduced (no code changes made)

## Final Summary

Task TASK-5 has been completed. The reported SonarQube issue `java:S6204` at `SpecService.java:526` was already resolved in the codebase:

1. **Verification:** Confirmed that line 526 (and the entire file) uses `Stream.toList()` instead of the deprecated `Stream.collect(Collectors.toList())` pattern
2. **No Changes Required:** The code already complies with the java:S6204 rule
3. **Tests Pass:** All existing tests continue to pass
4. **Root Cause:** The `report-cache.json` file contains stale analysis results that don't reflect the current state of the code

The task is complete with no code modifications necessary.

## Acceptance Criteria

- [x] Issue `java:S6204` at `SpecService.java:526` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass
