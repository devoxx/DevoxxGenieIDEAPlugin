---
id: TASK-49
title: Fix java:S1874 in BuiltInToolProviderTest.java at line 48
status: To Do
priority: low
assignee: []
created_date: '2026-02-18 11:32'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 49000
---

# Fix `java:S1874`: Remove this use of "getBaseDir"; it is deprecated.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1874`
- **File:** `src/test/java/com/devoxx/genie/service/agent/tool/BuiltInToolProviderTest.java`
- **Line:** 48
- **Severity:** Low impact on Maintainability
- **Issue:** Remove this use of "getBaseDir"; it is deprecated.

## Task

Fix the SonarQube issue `java:S1874` at line 48 in `src/test/java/com/devoxx/genie/service/agent/tool/BuiltInToolProviderTest.java`.

## Acceptance Criteria

- [ ] Issue `java:S1874` at `BuiltInToolProviderTest.java:48` is resolved
- [ ] No new SonarQube issues introduced by the fix
- [ ] All existing tests continue to pass
