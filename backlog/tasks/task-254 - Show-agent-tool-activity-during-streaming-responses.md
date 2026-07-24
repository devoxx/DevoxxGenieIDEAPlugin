---
id: TASK-254
title: Show agent tool activity during streaming responses
status: In Progress
assignee: []
created_date: '2026-07-24 12:23'
updated_date: '2026-07-24 12:29'
labels:
  - bug
  - agent
  - streaming
  - ui
dependencies: []
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

## Comments

<!-- COMMENTS:BEGIN -->
author: Codex
created: 2026-07-24 12:29
---
Investigation reproduced the issue: matching AGT tool request/result events appear in DevoxxGenie Logs but not the conversation when streaming is enabled; disabling streaming renders them in chat.
---
<!-- COMMENTS:END -->
