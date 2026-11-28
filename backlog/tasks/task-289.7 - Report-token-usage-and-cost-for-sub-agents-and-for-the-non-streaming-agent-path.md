---
id: TASK-289.7
title: >-
  Report token usage and cost for sub-agents and for the non-streaming agent
  path
status: To Do
assignee: []
created_date: '2026-11-28T10:52:00Z'
labels:
  - agent
  - sub-agents
  - cost
dependencies:
  - TASK-289.5
references:
  - >-
    backlog/tasks/task-289 -
    Improve-parallel-sub-agents-based-on-Codex-multi-agent-architecture.md
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
parent_task_id: TASK-289
priority: medium
type: enhancement
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
From the TASK-289 review (roadmap item R7, open question Q3, and out-of-scope defect 2).

Sub-agent token usage is discarded, so a run that fans out to several sub-agents shows only the parent's usage and cost; the real spend is invisible. Separately, the non-streaming agent/MCP path reports no token usage or cost even for the main agent.

This matters more once TASK-289.5 lands: sub-agents then inherit the main chat's model by default, which can be a paid provider, so the spend sub-agents add must be visible. The review recommends shipping this with or right after TASK-289.5.

Usage must be measured over the whole run (every round trip, not just the final response) and priced with each sub-agent's own model, kept separate from the parent's.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The non-streaming agent/MCP path reports the main agent's token usage and cost like the streaming path does; a test reproduces the missing usage first
- [ ] #2 Token usage of every sub-agent round trip is accumulated, not only the final response
- [ ] #3 Each sub-agent's cost is priced with that sub-agent's own model, and is reported separately from the parent's in the response metadata
- [ ] #4 A run whose sub-agents use a different provider than the parent shows both providers' usage correctly (test)
- [ ] #5 Usage analytics, if they record sub-agent runs, include the sub-agent token counts
<!-- AC:END -->
