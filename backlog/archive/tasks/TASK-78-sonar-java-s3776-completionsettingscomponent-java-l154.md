---
id: TASK-78
title: Fix java:S3776 in CompletionSettingsComponent.java at line 154
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:46'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 78000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/settings/completion/CompletionSettingsComponent.java`
- **Line:** 154
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 154 in `src/main/java/com/devoxx/genie/ui/settings/completion/CompletionSettingsComponent.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `CompletionSettingsComponent.java:154` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

The `loadModelsForProvider` method had cognitive complexity of 19 (exceeding the limit of 15) due to:
1. Deeply nested lambdas: `executeOnPooledThread` → `invokeLater`
2. For loops inside those nested lambdas (each adding nesting penalty)
3. A complex boolean condition in the error handler with two `&&` operators

### Fix Applied

Extracted three private helper methods:
- `populateOllamaModels(OllamaModelEntryDTO[], String)` — called via `invokeLater` for Ollama results; moves the for loop out of the lambda nest
- `populateLMStudioModels(LMStudioModelEntryDTO[], String)` — called via `invokeLater` for LMStudio results; moves the for loop out of the lambda nest
- `handleModelFetchError(String)` — called via `invokeLater` in the catch block; contains the error-recovery logic with the complex condition

This reduces the cognitive complexity of `loadModelsForProvider` from 19 to ~9, well within the 15 limit. Each extracted method stays well under 15 individually.

## Final Summary

**File modified:** `src/main/java/com/devoxx/genie/ui/settings/completion/CompletionSettingsComponent.java`

The `loadModelsForProvider` method at line 154 was refactored to reduce its cognitive complexity from 19 to ~9 by extracting three helper methods:

1. **`populateOllamaModels(OllamaModelEntryDTO[], String)`** — Handles populating the model combo box with Ollama models. This removes the for loop from inside the deeply nested lambda chain.

2. **`populateLMStudioModels(LMStudioModelEntryDTO[], String)`** — Handles populating the model combo box with LMStudio models. Same motivation.

3. **`handleModelFetchError(String)`** — Handles the error recovery path, including the complex `&&` condition checking whether to restore the previously-selected model.

The refactoring is purely structural: no logic was changed. The `loadModelsForProvider` method now delegates to these helpers via `SwingUtilities.invokeLater()` method references, making it readable and maintainable. All tests pass after the change.
