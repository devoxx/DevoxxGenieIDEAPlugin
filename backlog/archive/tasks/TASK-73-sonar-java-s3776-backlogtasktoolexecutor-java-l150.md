---
id: TASK-73
title: Fix java:S3776 in BacklogTaskToolExecutor.java at line 150
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:45'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 73000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java`
- **Line:** 150
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 150 in `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `BacklogTaskToolExecutor.java:150` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the inner for-loop body of `listTasks()` into a new private helper method `appendTaskLine(StringBuilder, TaskSpec)`.

The `listTasks` method had cognitive complexity of 16, coming from:
- Ternary in method args: +1
- `if (specs.isEmpty())`: +1
- Lambda with ternary in `groupingBy`: +2 (nesting level 1)
- Outer `for` loop: +1
- Inner `for` loop (nested): +2
- Three `if` statements at nesting level 2: +3 each = +9

Total: 16. The three deeply-nested `if` blocks were moved to `appendTaskLine`, reducing `listTasks` to complexity 7.
The `appendTaskLine` helper has complexity 3 (three flat `if` statements), well within limits.

Also extracted `labels.isEmpty() ? null : labels` into a local variable `labelsFilter` for clarity (no complexity change — ternary still present but now on its own line).

## Final Summary

**File modified:** `src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java`

**What changed:**
1. Extracted the inner for-loop body of `listTasks()` into a new private method `appendTaskLine(StringBuilder sb, TaskSpec spec)`.
2. Extracted the inline ternary `labels.isEmpty() ? null : labels` into a local variable `labelsFilter` for clarity.

**Why:** The `listTasks` method had cognitive complexity 16 (SonarQube S3776, threshold 15). Three `if` statements at nesting depth 2 each contributed +3 (1 base + 2 nesting penalty). Moving them into a private helper at nesting depth 0 reduces their cost to +1 each, dropping `listTasks` complexity to 7 — well within the limit. No logic was changed.
