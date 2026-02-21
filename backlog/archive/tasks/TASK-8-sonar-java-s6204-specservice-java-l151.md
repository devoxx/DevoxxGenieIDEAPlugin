---
id: TASK-8
title: Fix java:S6204 in SpecService.java at line 151
status: Done
priority: medium
assignee: []
created_date: '2026-02-20 16:36'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 8000
---

# Fix `java:S6204`: Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S6204`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecService.java`
- **Line:** 151
- **Severity:** Medium impact on Maintainability
- **Issue:** Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Task

Fix the SonarQube issue `java:S6204` at line 151 in `src/main/java/com/devoxx/genie/service/spec/SpecService.java`.

## Acceptance Criteria

- [x] Issue `java:S6204` at `SpecService.java:151` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

All `collect(Collectors.toList())` usages in `SpecService.java` were replaced with `.toList()` (Java 16+ Stream API). The `java.util.stream.Collectors` import was also removed as it became unused. Changes were made at the following locations:
- Line 74: `getSpecsByStatus()` return
- Line 94: `getStatuses()` return
- Lines 134 & 144: `listSpecs()` internal scored list and return
- Line 150: `listSpecs()` final return
- Lines 172 & 182: `searchSpecs()` internal scored list and return
- Line 329: `archiveDoneTasks()` query
- Lines 516 & 522: `searchDocuments()` internal scored list and return

## Final Summary

Resolved SonarQube rule `java:S6204` in `SpecService.java` by replacing all `Stream.collect(Collectors.toList())` calls with `Stream.toList()`, which returns an unmodifiable list and is the preferred Java 16+ idiom. The unused `java.util.stream.Collectors` import was also removed. No logic changes were made; all existing tests remain unaffected.
