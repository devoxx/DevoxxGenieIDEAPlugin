---
id: TASK-289.4
title: >-
  Sub-agent batches have a clear deadline, report every query they skip, and
  return a bounded result
status: To Do
assignee: []
created_date: '2026-11-28T10:52:00Z'
labels:
  - agent
  - sub-agents
dependencies:
  - TASK-289.2
  - TASK-285
references:
  - >-
    backlog/tasks/task-289 -
    Improve-parallel-sub-agents-based-on-Codex-multi-agent-architecture.md
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/threading/ThreadPoolManager.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptions.java
  - src/main/java/com/devoxx/genie/model/agent/SubAgentConfig.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java
parent_task_id: TASK-289
priority: medium
type: bug
ordinal: 45000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found by the TASK-289 review (findings B1, B2, B4, B10 and the budget half of B14).

- Deadline (B1): sub-agents share five application-wide threads, but the timeout restarts for each result collected, so a batch can run far longer than the configured timeout, and a queued sub-agent can be timed out before it ever started. The deadline semantics must be decided first: does the setting limit the whole batch from submission, or each sub-agent from when it starts (with a separate bound on waiting)? On expiry, completed results must be kept and unfinished work cancelled, distinguishing never-started from running.
- Pool (B4): pool isolation and a limit on concurrent sub-agents across chat tabs are chosen only after the deadline semantics are settled, since the two interact.
- Query loss (B2): the tool tells the model it accepts 2-5 queries but silently drops any beyond the configured number of slots (3 by default).
- Unbounded output (B10): the combined report handed back to the parent has no size cap. Multiplying the per-sub-agent output setting by the number of children is not a bound, because the Ollama factory does not pass that setting through, and raw `read_file` content is uncapped.
- Budgets (B14): sub-agent tool-call budgets are not clamped where they are used, and the sub-agent default (200 tool calls) is higher than the parent's (50).

Depends on TASK-289.2 because cancelling unfinished work at the deadline needs its cancellation fixes. Depends on TASK-285 (in review) because `search_files` and `list_files`, which sub-agents use, otherwise hold the IDE read lock for a whole walk with up to five in flight. Decide with TASK-287 (context size for local models) whether its output caps apply inside sub-agents and how truncation is signalled.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The chosen deadline semantics (whole batch vs. per sub-agent from start) are recorded in the task notes and the timeout setting's help text describes them
- [ ] #2 On a real small pool, a test shows a queued sub-agent that can still finish before the deadline does finish, and one that cannot is reported as never started
- [ ] #3 At the deadline, completed sub-agent results are kept in the parent's result and unfinished sub-agents are cancelled
- [ ] #4 The tool's parameter description states the effective maximum number of queries, and queries beyond it are listed as skipped in the result instead of being dropped silently
- [ ] #5 The combined result returned to the parent is capped, including headings and error text, with a visible truncation notice; tested with one large result and with several smaller ones
- [ ] #6 Sub-agent tool-call budgets are clamped where they are used, and the sub-agent default is reconsidered against the parent's (decision recorded)
- [ ] #7 Pool isolation and the cross-tab concurrency limit are chosen after the deadline semantics and covered by a test
<!-- AC:END -->
