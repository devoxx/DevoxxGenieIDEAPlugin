---
id: TASK-246
title: edit_file — support multiple edits in one call to cut agent round-trips
status: To Do
assignee: []
created_date: '2026-07-02 19:40'
labels:
  - agent-mode
  - tools
  - enhancement
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/EditFileToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalService.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

`edit_file` accepts a single `old_string`/`new_string` pair, so a multi-hunk change to one
file costs one LLM round-trip (and one approval prompt) **per hunk**. That is slow, expensive
in tokens (the conversation re-sends context each turn), and each approval interrupts the
user. Allow batching several edits to the same file in one call.

### Proposed change

Extend the `edit_file` tool spec with an alternative `edits` parameter: an array of
`{old_string, new_string, replace_all?}` objects, mutually exclusive with the existing
top-level pair (which stays for backward compatibility — models handle the simple shape
well for single edits).

Semantics:

- Edits are applied **sequentially against the evolving content** (edit N sees the result of
  edit N-1) — matches Claude Code's MultiEdit contract and lets later edits reference earlier
  ones.
- **Atomic:** validate all edits first (each `old_string` must match exactly once, or
  `replace_all` set) against the simulated intermediate contents; if any edit fails, apply
  none, and report which index failed and why. Partial application is worse than failure —
  the model loses track of file state.
- One approval prompt for the whole batch, showing a combined preview/diff of all hunks.
- Result message reports per-edit replacement counts.

### Notes

- Implementation lives in `EditFileToolExecutor`; keep the single-pair path delegating to
  the same batch engine with a one-element list to avoid two code paths.
- Same single-`WriteCommandAction`/undo-in-one-step behavior as the current executor.

### Acceptance criteria

- A 3-hunk edit applies in one tool call with one approval and is undoable as one step.
- A batch where hunk 2 doesn't match leaves the file untouched and names the failing index
  and reason.
- Sequential semantics covered by a test where edit 2 matches text produced by edit 1.
- Existing single-pair calls behave exactly as before.
<!-- SECTION:DESCRIPTION:END -->
