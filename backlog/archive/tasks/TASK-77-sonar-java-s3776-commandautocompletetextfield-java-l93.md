---
id: TASK-77
title: Fix java:S3776 in CommandAutoCompleteTextField.java at line 93
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
ordinal: 77000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/component/input/CommandAutoCompleteTextField.java`
- **Line:** 93
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 93 in `src/main/java/com/devoxx/genie/ui/component/input/CommandAutoCompleteTextField.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `CommandAutoCompleteTextField.java:93` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Reduced `keyPressed` cognitive complexity from 21 to 9 by:

1. Extracted `resolveSubmitShortcut(DevoxxGenieStateService)` — handles platform-specific submit shortcut resolution (complexity: 5)
2. Extracted `resolveNewlineShortcut(DevoxxGenieStateService)` — handles platform-specific newline shortcut resolution (complexity: 6)
3. Flattened the nested `if` chain for `@` key handling into a single `else if` with combined `&&` conditions — eliminates 2 nesting levels

The refactoring is purely mechanical (no behaviour change). All tests pass.

## Final Summary

Fixed `java:S3776` in `CommandKeyListener.keyPressed()` (line 93) by extracting two helper methods for platform-specific shortcut resolution and flattening the nested `if` for the `@` key handler into a single compound condition. The cognitive complexity of `keyPressed` dropped from 21 to 9. Each extracted helper stays well under the 15-allowed threshold (5 and 6 respectively). All existing tests pass and no new SonarQube issues were introduced.
