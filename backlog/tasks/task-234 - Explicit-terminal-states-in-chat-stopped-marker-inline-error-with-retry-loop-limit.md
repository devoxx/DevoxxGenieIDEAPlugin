---
id: TASK-234
title: 'Explicit terminal states in chat: stopped marker, inline error card with retry, loop-limit notice'
status: To Do
assignee: []
created_date: '2026-06-10 12:00'
updated_date: '2026-06-10 12:00'
labels:
  - enhancement
  - UX
  - error-handling
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - src/main/java/com/devoxx/genie/service/prompt/error/PromptErrorHandler.java
  - src/main/java/com/devoxx/genie/controller/PromptExecutionController.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
All three "the run didn't finish normally" paths currently end in near-silence inside the conversation:

- **Stop**: clicking Stop calls `StreamingResponseHandler.stop()` which sets `isStopped`, cleans up, and hides the loading indicator — the spinner just vanishes. A response cut off mid-sentence is indistinguishable from a complete one.
- **Error**: `PromptErrorHandler.handleException()` fires a transient IntelliJ toast via `NotificationUtil` and the chat shows nothing. Once the toast fades, there is no record of what failed, and the user must re-type or re-submit manually.
- **Loop limit**: when `AgentLoopTracker` hits max tool calls it publishes `LOOP_LIMIT`, visible only in the Logs tool window. In chat the agent appears to simply give up.

Add a terminal-state model to the message UI:

1. Extend `MessageUiModel` with `terminalState: COMPLETED | STOPPED | ERROR | LOOP_LIMIT` (+ optional `errorText`).
2. **Stopped**: in the stop path, set STOPPED on the active message; `AiBubble.kt` renders a small muted footer "⏹ Stopped by user" under whatever partial text was kept.
3. **Inline error card with Retry**: on error, render a compact error block in the bubble (red-tinted background, error summary, "Retry" button). Retry re-submits the original user prompt with the same context — `ChatMessageContext` is still available at error time; route the retry through the same submission entry point (`PromptExecutionController.handlePromptSubmission()` flow) rather than a parallel path. Keep the existing toast for visibility, but the durable record lives in the chat.
4. **Loop limit**: subscribe to `LOOP_LIMIT` in `ConversationViewModel.onActivityMessage()` and set the LOOP_LIMIT terminal state, rendered as "Reached max tool calls (N) — you can raise the limit in Settings → Agent" with a link/affordance that opens the Agent settings page.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Stopping a streaming response leaves a visible "Stopped by user" marker on that message; partial text remains exactly as before (memory cleanup behavior unchanged)
- [ ] #2 A failed prompt shows an inline error card in the conversation with a human-readable summary; the card persists across scrolling (it is part of the message model, not an overlay)
- [ ] #3 The error card's Retry button re-submits the same prompt with the same attached context exactly once per click, using the normal submission flow (buttons disable, indicator shows, etc.)
- [ ] #4 Hitting the agent loop limit shows an in-chat notice naming the configured limit, with an affordance that opens Settings → Agent
- [ ] #5 Terminal states are mutually exclusive and final per message: a stopped message cannot later flip to completed by a straggling token (isStopped guard already blocks updates — assert it)
- [ ] #6 Restored conversations from history render terminal markers gracefully (persisted conversations without the field default to COMPLETED; no crash on old data)
- [ ] #7 Unit tests: stop mid-stream sets STOPPED and blocks further partials; error path sets ERROR with message; LOOP_LIMIT event sets state; retry invokes the submission entry point with the original prompt
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Wire stop through the existing chain: `PromptExecutionController.stopPromptExecution()` → `StreamingResponseHandler.stop()` already runs on a known context; add a `conversationViewController` callback there (same pattern as `hideLoadingIndicator`).
- Error text: reuse whatever `PromptErrorHandler` already derives for the toast — do not build a second classification layer. Truncate long provider stack traces; full detail stays in idea.log.
- Retry must guard against double-submission (disable the button after click) and against retrying while another prompt is executing.
- Persistence: check what `ConversationStorageService` stores per message before persisting terminalState; if schema change is non-trivial, keeping terminal state session-only is acceptable for v1 — note the decision in this task on completion.
- Non-streaming mode (`NonStreamingPromptExecutionService`) has its own completion path; make sure STOPPED/ERROR are set there too, not only in `StreamingResponseHandler`.
- Synergy: task-233's status line should clear when any terminal state is set.
- Out of scope: automatic retries/backoff, error categorization UI, partial-response regeneration ("continue").
<!-- SECTION:NOTES:END -->
