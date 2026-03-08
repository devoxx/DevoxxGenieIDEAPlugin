---
id: TASK-189
title: 'Prompt Queue: Allow submitting prompts while another is still processing'
status: To Do
assignee: []
created_date: '2026-03-07 15:09'
updated_date: '2026-03-08 17:30'
labels:
  - enhancement
  - UX
  - architecture
dependencies: []
references:
  - src/main/java/com/devoxx/genie/controller/PromptExecutionController.java
  - src/main/java/com/devoxx/genie/ui/panel/UserPromptPanel.java
  - src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java
  - src/main/java/com/devoxx/genie/service/prompt/PromptExecutionService.java
  - src/main/java/com/devoxx/genie/service/ChatService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
  - 'https://code.claude.com/docs/en/interactive-mode'
  - 'https://github.com/anthropics/claude-code/issues/1101'
  - 'https://github.com/openai/codex/issues/2791'
  - 'https://github.com/openai/codex/issues/4312'
  - 'https://github.com/openai/codex/issues/5123'
  - 'https://github.com/openai/codex/issues/3369'
  - 'https://docs.langchain4j.dev/'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Currently, the user cannot submit a new prompt while a previous one is still being processed — the UI blocks input until the response completes. This limits the interactive experience compared to CLI-based AI tools like Claude Code and Codex, where users can type and queue follow-up prompts while the model is still responding.

The goal is to allow users to submit additional prompts while one is actively being processed. Submitted prompts should be queued and executed sequentially once the current prompt finishes. This creates a more fluid, conversational experience similar to CLI tools.

**Investigation areas:**
- **Current blocking mechanism:** `PromptExecutionController` and `UserPromptPanel` disable input during execution. Understand the full chain: `ActionButtonsPanel` submit button state, `PromptSubmissionListener`, and how streaming/non-streaming strategies signal completion.
- **Queue architecture:** Design a prompt queue (FIFO) that holds pending prompts. When a prompt completes, the next queued prompt should automatically start execution. Consider using a `LinkedBlockingQueue` or similar structure managed by a new `PromptQueueService`.
- **UI feedback:** Show the user that their prompt was queued (e.g., display it in the conversation panel with a "queued" indicator). Show queue position or pending count. Allow cancelling individual queued prompts.
- **Chat memory continuity:** Queued prompts must execute with the correct chat memory context — each prompt should see the full conversation history including the response from the previous prompt.
- **Agent/MCP mode considerations:** In agent mode with tool calls, the model may make multiple rounds of tool use. Queued prompts must wait until the full agent loop completes, not just the first response chunk.
- **Cancellation semantics:** If the user cancels the currently running prompt, should queued prompts still execute? Consider offering "cancel current" vs "cancel all" options.
- **Reference implementations:** Study how Claude Code and Codex CLI handle interactive prompt queuing for design inspiration.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 User can type and submit a new prompt while a previous prompt is still being processed
- [ ] #2 Queued prompts are displayed in the conversation panel with a visual indicator (e.g., 'queued' or 'pending')
- [ ] #3 Queued prompts execute sequentially in FIFO order after the current prompt completes
- [ ] #4 Each queued prompt executes with full conversation history including all prior responses
- [ ] #5 Queued prompts work correctly in all execution modes: streaming, non-streaming, agent, and MCP
- [ ] #6 User can cancel individual queued prompts before they start executing
- [ ] #7 User can cancel the currently running prompt without discarding queued prompts (cancel current vs cancel all)
- [ ] #8 The submit button and prompt input remain enabled while a prompt is processing
- [ ] #9 Queue count or pending indicator is visible to the user when prompts are queued
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Investigation summary (2026-03-08): current single-flight behavior is enforced in two places. PromptExecutionController.handlePromptSubmission() treats any second submit as stop-current instead of enqueue, while ActionButtonsPanel.disableButtons() disables the input area during execution. Relevant code: PromptExecutionController.java, ActionButtonsPanel.java, CommandAutoCompleteTextField.java.

There is already a narrow precedent for deferred execution: ActionButtonsPanel keeps a one-slot pendingSpecPrompt for the spec runner and submits it after enableButtons(). This suggests the clean refactor is to replace that ad hoc field with a real per-tab prompt queue service instead of bolting more state onto the panel.

Recommended queue shape: store queued submissions as lightweight request objects (prompt text, tabId, submission source, snapshot metadata for display) rather than fully-built ChatMessageContext instances. Build ChatMessageContext only when the item starts executing so each queued prompt sees the latest chat memory and avoids stale model/context assumptions.

Chat memory sequencing is compatible with FIFO execution if queue dispatch happens strictly after completion callback. Streaming adds AI memory in StreamingResponseHandler.onCompleteResponse(); non-streaming adds AI memory before PromptExecutionService cleanup callback returns; CLI and ACP also add AI memory on success. So queue dequeue should hook after the existing enableButtons/cleanup path, not before.

UI path is favorable for queued placeholders. Compose conversation rendering already models user prompts with loading state and mutable message state via ConversationViewModel.addUserPromptMessage()/updateAiMessageContent(). Extending MessageUiModel with queue state (QUEUED/RUNNING/CANCELLED) plus queuePosition would be lower risk than inventing a second renderer.

Persistence should remain execution-only. ChatService only writes history after AppTopics.CONVERSATION_TOPIC receives a completed ChatMessageContext, so queued placeholders should stay UI-only until execution starts. Do not persist queued entries into conversation storage.

Cancellation should likely split into cancelCurrent() and cancelAll()/cancelQueued(id). Today the submit button becomes Stop and current submit action stops the running task. For queue support, stopping the active prompt should not clear queued items by default. A small queue affordance near the submit button is likely cleaner than overloading the main button further.

Execution service needs a semantic change: executePrompt() currently cancels any existing execution for the tab before starting a new one. That is safe today because controller prevents concurrent submit, but queued dispatch must ensure only the dispatcher starts the next item; otherwise future callers could accidentally purge active work.

CLI/ACP modes can participate in the same queue, but ACP has a separate follow-up issue: AcpPromptStrategy adds AI memory on success yet does not publish AppTopics.CONVERSATION_TOPIC, unlike streaming/non-streaming/CLI. Queueing can still work, but restored conversation history may diverge for ACP until that is fixed.

External reference notes: Claude Code docs confirm interactive cancellation controls and task-list oriented UX. Public Codex issues show prompt queues, editable queued messages, and queue-specific bugs around message editing/loss. They also show slash commands are intentionally not universally queueable. This supports a scoped v1 here: queue normal prompts first, then decide separately whether prompt commands like /compact or plugin-style actions should join the same queue.

Suggested implementation split: (1) introduce PromptQueueService per tab with enqueue/dequeue/cancel APIs and queue events, (2) update controller/panel so submit while running enqueues instead of stops, (3) add Compose queue indicators and cancel affordances, (4) wire dispatcher on successful/failed/cancelled completion, (5) add mode coverage tests for streaming/non-streaming/CLI/ACP and queue-order/memory-order cases.

Additional constraint from follow-up review: keep the design aligned with the existing LangChain4j-based execution stack. The queue should sit above the strategy layer and dispatch into the current PromptExecutionStrategyFactory / StreamingPromptStrategy / NonStreamingPromptStrategy path, rather than bypassing LangChain4j or creating a parallel execution abstraction for standard providers. CLI/ACP runners can remain special strategies behind the same queue dispatcher.
<!-- SECTION:NOTES:END -->
