---
id: TASK-254
title: Show agent tool activity during streaming responses
status: Done
assignee:
  - Codex
created_date: '2026-07-24 12:23'
updated_date: '2026-07-24 12:43'
labels:
  - bug
  - agent
  - streaming
  - ui
dependencies: []
documentation:
  - docs/superpowers/specs/2026-07-24-streaming-agent-activity-design.md
modified_files:
  - >-
    src/main/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcher.java
  - src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/ConversationViewController.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/ComposeConversationViewController.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - >-
    src/test/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcherTest.java
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModelTest.kt
priority: high
ordinal: 3000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Agent tool calls are recorded in DevoxxGenie Logs but their matching rows disappear from the conversation output when streaming is enabled. With streaming disabled, the same activity renders in chat. Restore consistent in-chat visibility without exposing raw request/response payloads.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Agent tool requests and results appear in the matching streaming conversation when chat tool activity is enabled
- [x] #2 Agent tool activity continues to appear for non-streaming conversations
- [x] #3 Raw request and response payloads remain excluded from the conversation output
- [x] #4 Automated regression coverage verifies the streaming activity lifecycle
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
# TASK-254 Implementation Plan

## Scope
A single focused change that serializes in-chat activity delivery with streaming UI updates. No provider, tool-execution, or raw-log changes.

## Steps
1. Add a focused test seam for dispatching activity messages to the IntelliJ UI queue; verify delivery is deferred and preserves the original activity event.
2. Update the conversation-panel activity subscription to route accepted messages through that dispatcher before calling the Compose conversation controller.
3. Add regression coverage for a streamed message lifecycle in the view model: tool request opens an entry, a streamed response update preserves it, and tool response resolves it.
4. Run the focused Compose and dispatcher tests, then the relevant Gradle test suite.

## Files
- Create: src/main/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcher.java
- Create: src/test/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcherTest.java
- Modify: src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
- Modify: src/test/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModelTest.kt
- Create: docs/superpowers/plans/2026-07-24-streaming-agent-activity.md
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on `fix/task-254-show-agent-tool-activity-during-streaming` in commits `bf2af431` and `9fb4a83a`. Activity bus events now transition to the IntelliJ EDT before mutating Compose state and retain the active prompt generation; stale queued events are dropped after a prompt switch. Focused regression tests and `./gradlew buildPlugin` pass. Independent review identified the prompt-switch race; it was fixed with a queued interleaving test. In-IDE streaming verification remains pending.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: Codex
created: 2026-07-24 12:29
---
Investigation reproduced the issue: matching AGT tool request/result events appear in DevoxxGenie Logs but not the conversation when streaming is enabled; disabling streaming renders them in chat.
---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary
- Serialized in-chat activity delivery onto the IntelliJ EDT so streaming Compose updates cannot race background agent events.
- Captured and validated the active prompt generation to discard stale queued activity rather than attach it to a later prompt.
- Preserved RAW payload filtering and added dispatcher, streaming lifecycle, and prompt-switch regression coverage.

## Validation
- Manual in-IDE streaming verification confirmed agent activity now appears in the conversation (user confirmation).
- `./gradlew test --rerun-tasks --tests com.devoxx.genie.ui.panel.conversation.ActivityMessageDispatcherTest --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest` (40 passing tests).
- `./gradlew buildPlugin`.
- Independent code review completed with no remaining findings.
<!-- SECTION:FINAL_SUMMARY:END -->
