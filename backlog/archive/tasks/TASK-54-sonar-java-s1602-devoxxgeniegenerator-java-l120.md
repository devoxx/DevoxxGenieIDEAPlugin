---
id: TASK-54
title: Fix java:S1602 in DevoxxGenieGenerator.java at line 120
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
ordinal: 54000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java`
- **Line:** 120
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 120 in `src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `DevoxxGenieGenerator.java:120` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Notes

- **File modified:** `src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java`
- **Change:** Removed unnecessary curly braces around single-statement lambda body in the catch block at line 120. Changed `() -> { NotificationUtil.sendNotification(...); }` to `() -> NotificationUtil.sendNotification(...)`.
- No logic was altered; this is purely a cosmetic/style fix per java:S1602.

## Final Summary

Fixed SonarQube rule `java:S1602` in `DevoxxGenieGenerator.java` at line 120. The lambda expression in the catch block had unnecessary curly braces around a single statement. Removed the curly braces so the lambda uses expression body syntax instead of block body syntax. No functional change — this is purely a maintainability improvement as flagged by SonarQube.
