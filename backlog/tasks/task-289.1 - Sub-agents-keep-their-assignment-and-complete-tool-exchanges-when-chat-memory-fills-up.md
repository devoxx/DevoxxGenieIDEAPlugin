---
id: TASK-289.1
title: >-
  Sub-agents keep their assignment and complete tool exchanges when chat memory
  fills up
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
    src/main/java/com/devoxx/genie/service/prompt/memory/TaskPreservingMessageWindowChatMemory.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/test/java/com/devoxx/genie/service/agent/SubAgentMemoryTest.java
parent_task_id: TASK-289
priority: high
type: bug
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found by the TASK-289 review (finding B14) and reproduced with an offline probe of the compiled sub-agent chat memory (10-message window).

A sub-agent's memory holds the system prompt, its assignment (the delegated query), then the assistant's tool requests and their results. When one assistant message requests several tools at once, the eviction drops the wrong things:
- 7 tool requests in one assistant message: the assignment survives.
- 8: the assignment is evicted, so the sub-agent keeps working without knowing what it was asked.
- 9: only the system message remains.
- 10: the system message is followed by a tool result whose request was evicted (an orphan), which providers can reject.

Sequential single-tool turns do not trigger this; it needs the requests in one assistant message. This must be fixed before sub-agents are given write access (TASK-225), since a sub-agent that forgets its assignment while able to write is unsafe.

See TASK-289 for the full review, Codex comparison and related findings.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A regression test reproduces the current failure first: one assistant message with 8, 9 and 10 tool requests loses the assignment or leaves an orphan tool result
- [ ] #2 With 7, 8, 9 and 10 tool requests in one assistant message, the assignment message is never evicted
- [ ] #3 A tool result is never kept without the assistant message that requested it, and the active request/result batch is kept or dropped together
- [ ] #4 The behaviour for a single batch too large to fit the window is defined and covered by a test (it must not silently produce an invalid message sequence)
- [ ] #5 Existing sub-agent memory tests still pass, and the parent chat memory is unaffected
<!-- AC:END -->
