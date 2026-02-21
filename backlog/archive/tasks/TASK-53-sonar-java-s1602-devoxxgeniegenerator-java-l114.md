---
id: TASK-53
title: Fix java:S1602 in DevoxxGenieGenerator.java at line 114
status: Done
priority: low
assignee: []
created_date: '2026-02-20 18:22'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 53000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java`
- **Line:** 114
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 114 in `src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `DevoxxGenieGenerator.java:114` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

- Removed unnecessary curly braces from single-statement lambda at line 114
- Changed `() -> { NotificationUtil.sendNotification(...); }` to `() -> NotificationUtil.sendNotification(...)`
- File: `src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java`

## Final Summary

Removed useless curly braces around the single-statement lambda at line 114 in `DevoxxGenieGenerator.java`. The `invokeLater` call used a block-style lambda `() -> { ... }` for a single `NotificationUtil.sendNotification()` call. Replaced it with an expression lambda `() -> ...` per SonarQube rule `java:S1602`. No logic was changed.
