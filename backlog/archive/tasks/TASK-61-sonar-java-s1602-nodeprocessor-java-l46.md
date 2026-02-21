---
id: TASK-61
title: Fix java:S1602 in NodeProcessor.java at line 46
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
ordinal: 61000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/processor/NodeProcessor.java`
- **Line:** 46
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 46 in `src/main/java/com/devoxx/genie/ui/processor/NodeProcessor.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `NodeProcessor.java:46` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

**File modified:** `src/main/java/com/devoxx/genie/ui/processor/NodeProcessor.java`

**Change:** Line 46–48: Converted block lambda `() -> { stmt; }` to expression lambda `() -> stmt` inside `runReadAction(...)` call. The single statement `codeBlockRenderer.getAndSet(...)` had unnecessary curly braces, which is exactly what SonarQube rule `java:S1602` flags.

**Before:**
```java
ApplicationManager.getApplication().runReadAction(() -> {
    codeBlockRenderer.getAndSet(new CodeBlockNodeRenderer(project, context));
});
```

**After:**
```java
ApplicationManager.getApplication().runReadAction(() ->
    codeBlockRenderer.getAndSet(new CodeBlockNodeRenderer(project, context)));
```

No logic was changed. The lambda still runs the same single statement under `runReadAction`.

## Final Summary

Fixed SonarQube rule `java:S1602` ("Lambdas containing only one statement should not have curly braces") in `NodeProcessor.java` at line 46. Removed the block `{ }` wrapper and trailing semicolon from the single-statement `runReadAction` lambda, converting it to a concise expression lambda. No behavioral change.
