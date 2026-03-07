---
id: TASK-189
title: 'Prompt Queue: Allow submitting prompts while another is still processing'
status: To Do
assignee: []
created_date: '2026-03-07 15:09'
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
