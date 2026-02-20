---
id: TASK-9
title: Fix java:S6204 in SpecService.java at line 77
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
ordinal: 9000
---

# Fix `java:S6204`: Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S6204`
- **File:** `src/main/java/com/devoxx/genie/service/spec/SpecService.java`
- **Line:** 77
- **Severity:** Medium impact on Maintainability
- **Issue:** Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified.

## Task

Fix the SonarQube issue `java:S6204` at line 77 in `src/main/java/com/devoxx/genie/service/spec/SpecService.java`.

## Acceptance Criteria

- [x] Issue `java:S6204` at `SpecService.java:77` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Investigated java:S6204 issue at SpecService.java:77

Current code at line 74-77 (getSpecsByStatus method) already uses Stream.toList():
```java
return specCache.values().stream()
        .filter(spec -> status.equalsIgnoreCase(spec.getStatus()))
        .toList();
```

Verified no occurrences of collect(Collectors.toList()) exist in SpecService.java

All 10 uses of .toList() in SpecService.java use the correct Java 16+ syntax

Code compiles successfully with ./gradlew compileJava
