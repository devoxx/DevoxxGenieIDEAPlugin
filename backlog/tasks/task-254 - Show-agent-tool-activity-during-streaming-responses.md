---
id: TASK-254
title: Show agent tool activity during streaming responses
status: In Progress
assignee:
  - Codex
created_date: '2026-07-24 12:23'
updated_date: '2026-07-24 12:31'
labels:
  - bug
  - agent
  - streaming
  - ui
dependencies: []
documentation:
  - docs/superpowers/specs/2026-07-24-streaming-agent-activity-design.md
priority: high
ordinal: 3000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Agent tool calls are recorded in DevoxxGenie Logs but their matching rows disappear from the conversation output when streaming is enabled. With streaming disabled, the same activity renders in chat. Restore consistent in-chat visibility without exposing raw request/response payloads.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Agent tool requests and results appear in the matching streaming conversation when chat tool activity is enabled
- [ ] #2 Agent tool activity continues to appear for non-streaming conversations
- [ ] #3 Raw request and response payloads remain excluded from the conversation output
- [ ] #4 Automated regression coverage verifies the streaming activity lifecycle
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

## Comments

<!-- COMMENTS:BEGIN -->
author: Codex
created: 2026-07-24 12:29
---
Investigation reproduced the issue: matching AGT tool request/result events appear in DevoxxGenie Logs but not the conversation when streaming is enabled; disabling streaming renders them in chat.
---
<!-- COMMENTS:END -->
