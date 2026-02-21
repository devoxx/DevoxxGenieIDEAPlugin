---
id: TASK-63
title: Fix java:S1602 in PromptOutputPanel.java at line 69
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
ordinal: 63000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/panel/PromptOutputPanel.java`
- **Line:** 69
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 69 in `src/main/java/com/devoxx/genie/ui/panel/PromptOutputPanel.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `PromptOutputPanel.java:69` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Removed useless curly braces from the `connection -> { ... }` lambda at line 69.
The lambda body contained only a single statement (`MessageBusUtil.subscribe(...)`),
so the curly braces were unnecessary per java:S1602.

Changed from:
```java
MessageBusUtil.connect(project, connection -> {
    MessageBusUtil.subscribe(connection, AppTopics.FILE_REFERENCES_TOPIC,
        conversationPanel); // Delegate to the conversation panel
});
```

To:
```java
MessageBusUtil.connect(project, connection ->
        MessageBusUtil.subscribe(connection, AppTopics.FILE_REFERENCES_TOPIC,
                conversationPanel))); // Delegate to the conversation panel
```

## Final Summary

Fixed SonarQube rule `java:S1602` in `PromptOutputPanel.java` at line 69. The lambda
`connection -> { ... }` had useless curly braces wrapping a single `MessageBusUtil.subscribe()`
call. Removed the braces and the trailing semicolon inside (converting the block lambda to an
expression lambda). No logic was changed — purely a style/maintainability fix.
