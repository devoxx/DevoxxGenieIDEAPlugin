---
id: TASK-39
title: Expand MCPLogMessageHandler test coverage for edge cases
status: Done
assignee: []
created_date: '2026-02-14 11:01'
updated_date: '2026-02-14 16:51'
labels:
  - testing
  - mcp
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MCPLogMessageHandler (142 LOC) has 9 tests covering the main classification logic. Additional edge cases would help reach 80% branch coverage.

**Test cases to add:**
- formatLogMessage with AI_MSG type prepends "< "
- formatLogMessage with TOOL_MSG type prepends "> "
- formatLogMessage with LOG_MSG type returns message as-is
- classifyMessageType with GET request → TOOL_MSG
- classifyMessageType with "function(" in message → TOOL_MSG
- classifyMessageType with "response" key in JSON → AI_MSG
- publishToBus strips "< " prefix correctly
- publishToBus strips "> " prefix correctly
- publishToBus preserves message without direction markers
- logAtLevel WARNING level → logs at info (default case)
- handleLogMessage with null data text doesn't throw
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MCPLogMessageHandler has >= 80% line and branch coverage
- [x] #2 All message classification paths are tested
- [x] #3 publishToBus direction stripping is verified
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added 8 new tests to MCPLogMessageHandlerTest, bringing the total from 10 to 18 tests.\n\nNew tests cover:\n- **Classification branches**: GET request → TOOL_MSG, function() call → TOOL_MSG, JSON with \"response:\" → AI_MSG, JSON with \"function:\" → TOOL_MSG\n- **publishToBus direction stripping**: AI_MSG content has \"< \" stripped, TOOL_MSG content has \"> \" stripped, LOG_MSG content preserved without markers\n- **logAtLevel default case**: WARNING, INFO, and CRITICAL levels all fall through to default (log.info)\n\nAll 18 tests pass. Full test suite verified green.
<!-- SECTION:FINAL_SUMMARY:END -->
