---
id: TASK-289.5
title: >-
  Sub-agents use the main chat's model by default and receive only explicit,
  bounded context
status: To Do
assignee: []
created_date: '2026-11-28T10:52:00Z'
labels:
  - agent
  - sub-agents
dependencies: []
references:
  - >-
    backlog/tasks/task-289 -
    Improve-parallel-sub-agents-based-on-Codex-multi-agent-architecture.md
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java
  - src/main/java/com/devoxx/genie/model/agent/SubAgentConfig.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - src/main/java/com/devoxx/genie/service/MessageCreationService.java
parent_task_id: TASK-289
priority: medium
type: enhancement
ordinal: 46000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
From the TASK-289 review (roadmap items R2 and R6, defect B8, open questions Q3 and Q5).

Model choice (R2, B8): with no sub-agent model configured, sub-agents fall back to auto-detection instead of the model the user is chatting with. With no configured default and an empty Ollama model list, a sub-agent can even get a model with no name instead of failing clearly. The decided order is: an explicit per-slot model, then the configured sub-agent default, then the parent turn's model, with a clear failure and no silent probing of other providers. The existing sub-agent model dropdown gets "Same as main chat" as its empty entry (no new setting). Inheriting the parent model can move an existing user's sub-agents to a paid provider, so the help text states that sub-agents multiply model usage, and the changelog discloses the change.

Context (R6, Q5): sub-agents currently start with an empty memory and only the delegated query. The parent's full user message must NOT be forwarded automatically: it can contain editor and project content and system instructions, and the sub-agent may use a different provider. If context is passed, it is the delegated query plus explicit context, with a size cap on the whole assembled sub-agent input.

Both need the parent turn's model and prompt to reach the sub-agent executor per turn; the review found the tool invocation parameters can carry this, while the invocation context cannot as currently wired. Per-sub-agent cost reporting (TASK-289's R7) is a separate follow-up task that depends on this one.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A test reproduces the current failure first: with no default configured and an empty Ollama model list, a sub-agent gets a model with no name
- [ ] #2 Model resolution follows: explicit per-slot model, then the configured sub-agent default, then the parent turn's model; a failure to resolve is reported clearly and no other provider is probed
- [ ] #3 The sub-agent model dropdown offers "Same as main chat" as its empty entry, and its help text states that sub-agents multiply model usage
- [ ] #4 The changelog discloses that sub-agents now inherit the main chat's model by default
- [ ] #5 A test shows that a marker present only in the parent's editor/project context never reaches a sub-agent that uses a different provider
- [ ] #6 Any context passed to a sub-agent is explicit, and the whole assembled sub-agent input is capped, with a test at the cap
<!-- AC:END -->
