---
id: TASK-255
title: 'Mid-task steering: send chat messages to a running agent task (issue #1241)'
status: In Progress
assignee: []
created_date: '2026-07-29 08:48'
updated_date: '2026-07-29 09:07'
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
- [x] #1 A steering message submitted while an agent task is running is delivered to the LLM in the next agent-loop round trip (both streaming and non-streaming paths)
- [x] #2 The injected message never produces an invalid message sequence (no dangling tool_use / provider 400s); it lands after completed tool results
- [x] #3 Steering messages are persisted in chat memory so later turns and saved conversations include them
- [x] #4 Submitting while a task runs no longer cancels the run when the input contains text; stopping the task remains possible
- [ ] #5 A user bubble for the steering message appears in the conversation UI without disrupting the in-flight streaming response
- [x] #6 Queued messages that were never consumed (task finished first) are not silently lost
- [x] #7 Unit tests cover queue behavior, round-trip injection, and ordering safety
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Prototype implemented on branch feature/issue-1241-mid-task-steering (commit c2cb4262), TDD, 26 new tests + full suite green.

Mechanism: SteeringMessageQueue (per-memory-key FIFO, null-safe, active-flag lifecycle) + SteeringMessageInjector wired as AiServices.chatRequestTransformer in StreamingPromptStrategy.buildAssistant and NonStreamingPromptExecutionService (only when a tool provider exists). langchain4j 1.18.0 applies the transformer on every round trip — verified by MidTaskSteeringRoundTripTest against the real dependency for both paths: injected UserMessage lands at request tail after tool results, exactly once, and persists in chat memory.

Entry points: submit button (non-blank falls through to steering; blank = stop) and Enter key. NOTE the Enter key submits via AppTopics.PROMPT_SUBMISSION_TOPIC → ActionButtonsPanel.onPromptSubmitted, which previously swallowed the text into the spec-runner one-slot queue while running — fixed by trying controller.steerRunningPrompt(text) first (found by user manual testing, then covered by steerRunningPromptWithRawText tests).

Leftover policy: run ends naturally → unconsumed messages resubmitted as a new prompt via PROMPT_SUBMISSION_TOPIC; user stops → discarded.

AC #5 (user bubble without disrupting stream): implemented via ConversationViewModel.addSteeringMessage (does not touch activeMessageId/activity handlers); needs manual IDE validation before checking. Known prototype limitation: after steering, the AI's continued output streams in the ORIGINAL bubble above the steering bubble — the issue's ideal is freezing the old area and opening a new AI area below (candidate follow-up).
<!-- SECTION:NOTES:END -->
