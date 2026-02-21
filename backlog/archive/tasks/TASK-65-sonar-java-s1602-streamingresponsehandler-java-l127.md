---
id: TASK-65
title: Fix java:S1602 in StreamingResponseHandler.java at line 127
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
ordinal: 65000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java`
- **Line:** 127
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 127 in `src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `StreamingResponseHandler.java:127` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Notes

Changed the `invokeLater` lambda at line 127 from block syntax `() -> { ... }` (with a single statement) to expression syntax `() -> statement`. Also moved the inline comment outside the lambda block to preserve it. No logic was changed.

## Final Summary

Removed unnecessary curly braces in the `invokeLater` lambda in `StreamingResponseHandler.java` around line 127. The lambda contained a single method call (`conversationWebViewController.addFileReferences(...)`), so the block syntax was replaced with expression syntax as required by java:S1602. The comment that was inside the block was moved just above the `invokeLater` call. No behavioral changes.
