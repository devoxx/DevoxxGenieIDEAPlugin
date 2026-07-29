---
id: TASK-255
title: 'Mid-task steering: send chat messages to a running agent task (issue #1241)'
status: In Progress
assignee: []
created_date: '2026-07-29 08:48'
updated_date: '2026-07-29 10:47'
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

UI sequence fix (commit 54f91940): replaced trailing-bubble rendering with freeze-and-split in ConversationViewModel.addSteeringMessage — frozen copy (new id, resolved activity rows, isSteeringFrozen) stays in place, steering user bubble (isSteeringOnly) follows, active message moves to the end as continuation (same id, keeps activeMessageId/streaming target, unresolved activity rows). Because StreamingResponseHandler re-posts the FULL accumulated text every flush, the continuation subtracts a stored aiContentOffset/thinkingContentOffset (invariant: offset + shown length == full text length; leading newlines at the cut are consumed into the offset). MessagePair.shouldHideAiBubble hides empty header-only AI frames for split messages (frozen or offset>0) unless loading or a non-COMPLETED terminal state must render. Covered by ConversationViewModelSteeringTest (6) + MessagePairVisibilityTest (7); full suite green. AC #5 ready for manual re-validation.

Fix round 3 (manual testing feedback): (1) Models pivoted to the steering question and never answered the original request — the injected message arrived right after tool results, before the model wrote its answer. SteeringMessageInjector now prepends STEERING_CONTEXT_NOTE instructing the model to complete the original request (including its final answer) and additionally address the new input. Note: the framed text (note + raw text) is what lands in chat memory. (2) Leftover steering messages resubmitted at run end rendered the question twice (stale steering bubble + new prompt bubble) — PromptExecutionController now calls ConversationPanel.removeSteeringMessage(text) per leftover before publishing the resubmission; ConversationViewModel.removeSteeringMessage removes the last matching isSteeringOnly bubble. 47 steering-related tests green, full suite green.

Round 4 (commit 9bc323c0) — queue-by-default per user decision: extra prompts typed during a run are usually independent NEXT questions, not corrections, so injecting them made models conflate requests. New PendingPromptQueue (per memory key): submissions while running are queued and executed sequentially after the current run, one per run, via the PROMPT_SUBMISSION_TOPIC resubmit path (queued bubble added immediately, swapped for the real prompt bubble on its turn; stop discards queue + bubbles). Steering is now explicit-only: while running the submit button becomes Stop and two labeled buttons appear — Queue (ClockIcon, default, Enter equivalent) and Steer (SubmitIcon paper plane, mid-loop injection, falls back to queue when no tool loop). Rich HTML tooltips on both. Enter route preserves spec-runner one-slot queue when SpecTaskRunnerService is running. This effectively also implements TASK-189's core (prompt queue). 58 steering/queue tests + full suite green (2m56s rerun).

Round 5 (commit 0f603132): glow regression — endPromptExecution resubmitted the queued prompt before enableButtons(), so the new run's synchronous startGlowing landed on the EDT before the old run's pending stopGlowing (scheduled inside enableButtons' invokeLater), which killed the fresh glow for every queued follow-up run. Fixed by calling enableButtons() before resubmitUnconsumedSteeringMessages(); InOrder regression test added (enableButtons before the prompt-submission publish). Full suite green.
<!-- SECTION:NOTES:END -->
