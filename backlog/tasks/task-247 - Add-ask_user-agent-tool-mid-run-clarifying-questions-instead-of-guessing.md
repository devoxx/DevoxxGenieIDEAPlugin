---
id: TASK-247
title: Add ask_user agent tool — mid-run clarifying questions instead of guessing
status: To Do
assignee: []
created_date: '2026-07-02 19:40'
labels:
  - agent-mode
  - tools
  - ux
  - feature
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalService.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Give the agent a way to ask the user a clarifying question mid-run. Today the only
human-in-the-loop touchpoint is the approval dialog, which is binary approve/deny on a tool
call the model has *already decided* to make. When the task is ambiguous ("rename this API" —
which of the two overloads? "fix the failing test" — by changing the test or the code?), the
model guesses, and a wrong guess wastes an entire multi-step run plus the user's review time.

### Proposed tool

`ask_user(question, options?)`

- `question` (required): the clarifying question, with enough context to answer without
  scrolling back.
- `options` (optional array of strings): 2-4 suggested answers rendered as buttons; free-text
  input is always available regardless.
- Returns the user's answer as the tool result; if the user dismisses/cancels, returns a
  distinct "user declined to answer — proceed with your best judgment and state your
  assumption" message so the run doesn't dead-end.

### Design

- New `AskUserToolExecutor` under `service/agent/tool/`, registered in `BuiltInToolProvider`
  behind an `askUserToolEnabled` flag (default true).
- Reuse/extend the `AgentApprovalService` dialog plumbing (it already solves
  block-agent-thread → show-dialog-on-EDT → resume-with-result); this is the same shape with
  a richer payload (question text + option buttons + free-text field) and a string result
  instead of a boolean.
- It performs no side effects, but do NOT auto-approve-suppress it — it IS the interaction.
  Add it to `READ_ONLY_TOOLS` so the approval wrapper doesn't show a second dialog on top of
  the question dialog.
- **Timeout:** the agent thread blocks while waiting; add a generous timeout (e.g.
  configurable, default 5 minutes) after which it returns the "declined" message, so an
  unattended run terminates gracefully instead of hanging the loop.
- **Budget:** cap `ask_user` calls per run (e.g. 3, tracked via `AgentLoopTracker`) so a
  model can't turn agent mode into an interrogation.
- Tool description should discourage overuse: "Only ask when genuinely blocked on a decision
  you cannot resolve from the code or the conversation. Never ask for permission to proceed."
- Sub-agents (`parallel_explore`, future `delegate_task` from TASK-225) must NOT get this
  tool — questions must come from the top-level agent only.

### Acceptance criteria

- Model can ask a question with options; the user's click or typed answer arrives as the
  tool result and the run continues.
- Dialog dismissal and timeout both return the graceful "proceed with best judgment" result.
- Per-run question cap enforced; exceeding it returns an instructive error to the model.
- No EDT violations (dialog on EDT, agent thread blocked off-EDT).
- Not exposed to sub-agent tool providers.
<!-- SECTION:DESCRIPTION:END -->
