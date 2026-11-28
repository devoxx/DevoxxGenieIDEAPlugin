---
id: TASK-289.3
title: Respect disabled agent tools inside sub-agents
status: To Do
assignee: []
created_date: '2026-11-28T10:51:00Z'
labels:
  - agent
  - sub-agents
dependencies: []
references:
  - >-
    backlog/tasks/task-289 -
    Improve-parallel-sub-agents-based-on-Codex-multi-agent-architecture.md
  - src/main/java/com/devoxx/genie/service/agent/tool/ReadOnlyToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
parent_task_id: TASK-289
priority: medium
type: bug
ordinal: 44000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found by the TASK-289 review (finding B9, plus the docs half of B15).

Users can disable individual agent tools in Settings (`disabledAgentTools`). The main agent honours this, but sub-agents started by `parallel_explore` get their read-only tools from a separate provider that ignores the setting, so a tool the user turned off is still used by sub-agents. A sub-agent must never have a tool the parent is not allowed to use.

This is independent of any future expansion of the sub-agent tool set (PSI or semantic search tools); ship it on its own.

The agent-mode documentation also contradicts itself on whether sub-agents have PSI (code navigation) tools; correct it to match what sub-agents actually get.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A test reproduces the current failure first: a tool disabled in settings is still offered to a sub-agent
- [ ] #2 Disabling any one of the sub-agent's read/list/search tools removes exactly that tool from sub-agents and leaves the enabled ones available (one test case per tool)
- [ ] #3 With no tools disabled, sub-agents get the same tools as before
- [ ] #4 The agent-mode documentation states consistently which tools sub-agents have
<!-- AC:END -->
