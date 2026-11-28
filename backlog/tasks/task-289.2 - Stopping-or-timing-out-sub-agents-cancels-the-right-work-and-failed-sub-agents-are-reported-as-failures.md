---
id: TASK-289.2
title: >-
  Stopping or timing out sub-agents cancels the right work, and failed
  sub-agents are reported as failures
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
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
  - src/main/java/com/devoxx/genie/service/trace/AgentTraceRecorder.java
parent_task_id: TASK-289
priority: high
type: bug
ordinal: 43000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found by the TASK-289 review (findings B3, B6, B7, B13 and the analytics half of B15). These are traced from source, not reproduced, so each fix starts with a test that reproduces it.

`parallel_explore` runs sub-agents in parallel and blocks the parent agent until they return. Cancellation and outcome reporting are unreliable:
- Parent Stop does not reliably cancel the batch's sub-agents or unblock the parent while it waits for them.
- A single sub-agent timing out while its model is still being resolved can either keep running (its tracker may not exist yet) or, if the shared parent flag is used, stop its healthy siblings. One sub-agent's timeout must only stop that sub-agent.
- Stop pressed during non-streaming memory preparation can happen before the query and tracker are registered, so the cancelled turn can still launch model calls. Related paths: non-streaming wall-clock expiry removes the tracker without cancelling it; streaming prompt-task cancellation and project disposal.
- Failed sub-agents can look successful: the chat text, the trace recorder (which reads the structured error flag, not the text) and the sub-agent events can disagree about whether the call failed. A mixed batch (some failed, some succeeded) must keep the successful answers.
- A sub-agent that fails while its model is created sends no analytics event.

This gates writable delegation in TASK-225. See TASK-289 for details.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Stop pressed while sub-agents run cancels every unfinished sub-agent of that batch and unblocks the parent; a test asserts no further model calls after Stop
- [ ] #2 A sub-agent that times out during model resolution never calls the model, even if the resolver ignores interruption, while a sibling sub-agent in the same batch still completes (latch-based test)
- [ ] #3 Stop pressed during non-streaming memory preparation results in zero model calls (latch-based test)
- [ ] #4 Non-streaming wall-clock expiry, streaming prompt-task cancellation and project disposal each cancel in-flight sub-agents; each has a test
- [ ] #5 Each sub-agent call returns a typed outcome, and the chat text, the tool result's error flag, the trace and the sub-agent events all agree for an all-failed batch and a mixed batch (tested through the tool executor's context entry point)
- [ ] #6 A mixed batch keeps the successful sub-agents' answers in the result returned to the parent
- [ ] #7 A sub-agent that fails while its model is created still emits its analytics event
- [ ] #8 Runner and future cleanup happens on every exit path, including failure and cancellation
<!-- AC:END -->
