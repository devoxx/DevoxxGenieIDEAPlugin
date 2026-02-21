---
id: TASK-58
title: Fix java:S1602 in LLMProvidersComponent.java at line 229
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
ordinal: 58000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java`
- **Line:** 229
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 229 in `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `LLMProvidersComponent.java:229` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Converted lambda with single statement wrapped in unnecessary curly braces to concise lambda expression.

**File modified:** `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java`

**Change:** Line 229-231, `enableAzureOpenAICheckBox.addItemListener` lambda:
- Before: `event -> { setNestedComponentsVisibility(...); }`
- After: `event -> setNestedComponentsVisibility(...)`

## Final Summary

Fixed SonarQube rule `java:S1602` in `LLMProvidersComponent.java` at line 229. The lambda body for `enableAzureOpenAICheckBox.addItemListener` contained a single statement wrapped in unnecessary curly braces. Converted it to a concise block-less lambda expression. No logic changes; purely a style/maintainability fix.
