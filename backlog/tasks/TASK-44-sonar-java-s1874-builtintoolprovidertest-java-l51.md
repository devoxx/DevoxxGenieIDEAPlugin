---
id: TASK-44
title: Fix java:S1874 in BuiltInToolProviderTest.java at line 51
status: Done
priority: low
assignee: []
created_date: '2026-02-18 11:15'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 44000
---

# Fix `java:S1874`: Remove this use of "getBaseDir"; it is deprecated.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1874`
- **File:** `src/test/java/com/devoxx/genie/service/agent/tool/BuiltInToolProviderTest.java`
- **Line:** 51
- **Severity:** Low impact on Maintainability
- **Issue:** Remove this use of "getBaseDir"; it is deprecated.

## Task

Fix the SonarQube issue `java:S1874` at line 51 in `src/test/java/com/devoxx/genie/service/agent/tool/BuiltInToolProviderTest.java`.

## Acceptance Criteria

- [x] Issue `java:S1874` at `BuiltInToolProviderTest.java:51` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Removed the deprecated `project.getBaseDir()` call from `setUp()` at line 51. Investigation showed that:
- `BuiltInToolProvider` never calls `getBaseDir()` — it only passes the `Project` to tool executors
- All tool executors use `ProjectUtil.guessProjectDir(project)` or `project.getBasePath()` instead
- The `projectBase` mock field and `VirtualFile` import were also unused and removed

**Files modified:** `src/test/java/com/devoxx/genie/service/agent/tool/BuiltInToolProviderTest.java`

**Changes:**
1. Removed `import com.intellij.openapi.vfs.VirtualFile` (unused after fix)
2. Removed `@Mock private VirtualFile projectBase` field (only used in deprecated call)
3. Removed `when(project.getBaseDir()).thenReturn(projectBase)` line (the deprecated API call)

All 13 tests in `BuiltInToolProviderTest` pass after the change.
