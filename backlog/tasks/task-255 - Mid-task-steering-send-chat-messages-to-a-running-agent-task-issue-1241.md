---
id: TASK-255
title: 'Mid-task steering: send chat messages to a running agent task (issue #1241)'
status: In Progress
assignee: []
created_date: '2026-07-29 08:48'
labels: []
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1241'
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Users currently cannot interact with the LLM while an agent task is running — the prompt input is locked, and pressing submit cancels the run. GitHub issue #1241 requests Claude Code-style mid-task steering: while a task runs, the user can keep typing and send messages that are queued and injected into the LLM's message context on the next agent-loop iteration, so the model can correct course (e.g. "use snake_case for the API JSON") without an expensive correction run afterwards.\n\nFeasibility confirmed: langchain4j 1.18.0 re-reads ChatMemory between tool round-trips and exposes AiServices.chatRequestTransformer which is applied to every round-trip request — a safe injection point that avoids dangling tool_use ordering issues. Related but distinct: TASK-189 covers FIFO queue-then-execute-after-completion, not mid-loop injection.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A steering message submitted while an agent task is running is delivered to the LLM in the next agent-loop round trip (both streaming and non-streaming paths)
- [ ] #2 The injected message never produces an invalid message sequence (no dangling tool_use / provider 400s); it lands after completed tool results
- [ ] #3 Steering messages are persisted in chat memory so later turns and saved conversations include them
- [ ] #4 Submitting while a task runs no longer cancels the run when the input contains text; stopping the task remains possible
- [ ] #5 A user bubble for the steering message appears in the conversation UI without disrupting the in-flight streaming response
- [ ] #6 Queued messages that were never consumed (task finished first) are not silently lost
- [ ] #7 Unit tests cover queue behavior, round-trip injection, and ordering safety
<!-- AC:END -->
