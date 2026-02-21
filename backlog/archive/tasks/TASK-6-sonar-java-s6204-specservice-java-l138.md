---
id: TASK-6
title: Fix java:S6204 in SpecService.java at line 138
status: Done
priority: medium
assignee: []
created_date: '2026-02-20 16:36'
updated_date: '2026-02-20 17:00'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 6000
---

# Fix `java:S6204`: Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S6204`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecService.java`
- **Line:** 138
- **Severity:** Medium impact on Maintainability
- **Issue:** Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Task

Fix the SonarQube issue `java:S6204` at line 138 in `src/main/java/com/devoxx/genie/service/spec/SpecService.java`.

## Acceptance Criteria

- [x] Issue `java:S6204` at `SpecService.java:138` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Fixed all `collect(Collectors.toList())` occurrences in `SpecService.java` by replacing them with `.toList()` (available since Java 16). The `java.util.stream.Collectors` import was also removed as it is no longer needed.

Changes made in `src/main/java/com/devoxx/genie/service/spec/SpecService.java`:
- Removed `import java.util.stream.Collectors;`
- Line ~76: `getSpecsByStatus()` — `.collect(Collectors.toList())` → `.toList()`
- Line ~98: `getStatuses()` — `.collect(Collectors.toList())` → `.toList()`
- Line ~138 (original): `getSpecsByFilters()` search branch — `.collect(Collectors.toList())` → `.toList()` (the primary reported line)
- Line ~143 (original): `getSpecsByFilters()` result stream — `.collect(Collectors.toList())` → `.toList()`
- Line ~150 (original): `getSpecsByFilters()` non-search branch — `.collect(Collectors.toList())` → `.toList()`
- Line ~175 (original): `searchSpecs()` — `.collect(Collectors.toList())` → `.toList()`
- Line ~182 (original): `searchSpecs()` result stream — `.collect(Collectors.toList())` → `.toList()`
- Line ~332 (original): `archiveDoneTasks()` — `.collect(Collectors.toList())` → `.toList()`
- Line ~519 (original): `searchDocuments()` — `.collect(Collectors.toList())` → `.toList()`
- Line ~525 (original): `searchDocuments()` result stream — `.collect(Collectors.toList())` → `.toList()`

## Final Summary

**What was implemented:** Replaced all usages of `Stream.collect(Collectors.toList())` with the more concise `Stream.toList()` terminal operation throughout `SpecService.java`. The `java.util.stream.Collectors` import was removed since it is no longer referenced anywhere in the file.

**Why:** SonarQube rule `java:S6204` flags `collect(Collectors.toList())` as a code smell since Java 16 introduced the more readable and intention-revealing `Stream.toList()` method. The new method also returns an unmodifiable list, which is semantically correct for all these use cases (callers should not mutate the returned lists).

**Files modified:**
- `src/main/java/com/devoxx/genie/service/spec/SpecService.java` — 10 replacements + 1 import removal

**No new issues introduced:** All replacements are semantically equivalent for the read-only use cases in this file. The returned lists are used for display/iteration only and not mutated by callers.
