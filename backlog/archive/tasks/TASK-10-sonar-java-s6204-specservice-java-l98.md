---
id: TASK-10
title: Fix java:S6204 in SpecService.java at line 98
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
ordinal: 10000
---

# Fix `java:S6204`: Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S6204`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecService.java`
- **Line:** 98
- **Severity:** Medium impact on Maintainability
- **Issue:** Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Task

Fix the SonarQube issue `java:S6204` at line 98 in `src/main/java/com/devoxx/genie/service/spec/SpecService.java`.

## Acceptance Criteria

- [x] Issue `java:S6204` at `SpecService.java:98` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

- Replaced all `.collect(Collectors.toList())` calls with `.toList()` in SpecService.java (9 occurrences total)
- Removed the now-unused `import java.util.stream.Collectors;`
- The fix covers line 98 (getStatuses method) and all other similar occurrences in the same file

## Final Summary

Fixed `java:S6204` in `src/main/java/com/devoxx/genie/service/spec/SpecService.java` by replacing all occurrences of `.collect(Collectors.toList())` with the more idiomatic `.toList()` (available since Java 16). This included 9 replacements across methods: `getSpecsByStatus`, `getStatuses`, `getSpecsByFilters` (3 occurrences), `searchSpecs` (2 occurrences), `archiveDoneTasks`, and `searchDocuments` (2 occurrences). The unused `import java.util.stream.Collectors` was also removed. No new issues were introduced and the fix is consistent with modern Java style.
