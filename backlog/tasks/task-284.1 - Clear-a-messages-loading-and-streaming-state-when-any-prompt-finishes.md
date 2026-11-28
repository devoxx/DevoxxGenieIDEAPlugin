---
id: TASK-284.1
title: Clear a message's loading and streaming state when any prompt finishes
status: To Do
assignee: []
created_date: '2026-11-28T11:32:00Z'
labels:
  - chat
  - ui
  - agent
dependencies: []
references:
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/AbstractPromptExecutionStrategy.java
  - src/main/java/com/devoxx/genie/controller/PromptExecutionController.java
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/components/ConversationToolbar.kt
  - >-
    backlog/tasks/task-288 -
    Show-live-progress-while-an-agent-run-is-in-flight.md
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1457
parent_task_id: TASK-284
priority: medium
type: bug
ordinal: 49000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found by the TASK-284 review (2026-11-28), traced from source; not yet reproduced in the IDE.

Every prompt bubble starts with `isLoadingIndicatorVisible = true`, and in streaming mode with `isStreaming = true`. Only the streaming handler, the non-streaming strategy and the error branches clear them, through `hideLoadingIndicator` or `setTerminalState`. These paths finish without clearing them:
- web search, on success
- `/find`, with results and with no results
- CLI runners, on success and when stopped
- ACP runners, on success and when stopped, plus the ACP error path that completes normally

The stale flags are invisible in the bubble itself, because the "thinking" dots only show while the reply is empty. They still matter elsewhere:
- **TASK-288's live progress line** shows while either flag is set, so it probably keeps ticking ("Working…" with a growing elapsed time) under those finished responses.
- **TASK-284's "View agent run" button** needed a narrower in-flight check (only the latest turn counts) to avoid being disabled for the rest of a conversation.

Any other code that reads these flags to mean "a response is running" is misled the same way.

The fix should clear the state on every completion path, ideally in one place. Clearing it too early would cut off the last streamed tokens or the streaming caret: handlers must be deactivated before the loading indicator is hidden (see the race described in the prompt execution pipeline notes).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A test reproduces the bug first: after a successful web search turn the message still has isLoadingIndicatorVisible or isStreaming set
- [ ] #2 After web search, /find (with and without results), CLI and ACP turns complete, their message has isLoadingIndicatorVisible = false and isStreaming = false; each path has a test
- [ ] #3 A stopped CLI or ACP run, and the ACP error path that completes normally, also leave both flags cleared
- [ ] #4 Streaming responses still show all their tokens and the streaming caret until the stream ends: the flags are not cleared before the streaming handler finishes (existing streaming tests pass, plus a test for the ordering)
- [ ] #5 With TASK-288's progress line present, it stops after web search, /find, CLI and ACP turns (verified manually or by a test)
<!-- AC:END -->
