---
id: TASK-3
title: Fix java:S6204 in SpecService.java at line 0
status: Done
priority: medium
assignee: []
created_date: '2026-02-20 16:36'
completed_date: '2026-02-20 16:50'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 3000
---

# Fix `java:S6204`: Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S6204`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecService.java`
- **Line:** 0
- **Severity:** Medium impact on Maintainability
- **Issue:** Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Task

Fix the SonarQube issue `java:S6204` at line 0 in `src/main/java/com/devoxx/genie/service/spec/SpecService.java`.

## Acceptance Criteria

- [x] Issue `java:S6204` at `SpecService.java:0` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

### Investigation Results

Upon thorough investigation of `src/main/java/com/devoxx/genie/service/spec/SpecService.java`:

1. **No `Collectors.toList()` usage found**: Searched the entire file - no occurrences of `Collectors.toList()` exist.

2. **Already using modern `.toList()` syntax**: The file already uses Java 16+ `Stream.toList()` method at all relevant locations:
   - Line 76: `.toList()` in `getSpecsByStatus()`
   - Line 97: `.toList()` in `getStatuses()`
   - Line 143: `.toList()` in `getSpecsByFilters()`
   - Line 150: `.toList()` in `getSpecsByFilters()`
   - Line 182: `.toList()` in `searchSpecs()`
   - Line 332: `.toList()` in `archiveDoneTasks()`
   - Line 525: `.toList()` in `searchDocuments()`

3. **No `Collectors` import**: The file does not import `java.util.stream.Collectors`.

4. **Stale SonarQube report**: The report-cache.json contains offset ranges that don't correspond to actual `Collectors.toList()` usages. The character offsets (3019, 3645, 5596, etc.) map to unrelated code sections (javadoc comments, method signatures).

5. **All tests pass**: Verified by running `SpecServiceTest` - all 47 tests pass successfully.

### Conclusion

The SonarQube issue `java:S6204` at line 0 in `SpecService.java` is a **false positive from a stale report**. The code already complies with the rule by using `.toList()` instead of `Collectors.toList()`. No code changes were required.

## Final Summary

- **Files Modified**: None (no code changes required)
- **Issue Resolution**: Issue was already resolved/stale report
- **Test Results**: All 47 tests in `SpecServiceTest` pass
- **Verification**: Confirmed no `Collectors.toList()` usages exist in the file
