---
id: TASK-35
title: Add unit tests for MCPListenerService
status: Done
assignee: []
created_date: '2026-02-14 11:01'
updated_date: '2026-02-14 15:31'
labels:
  - testing
  - mcp
  - refactoring
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MCPListenerService (93 LOC) has ZERO tests. It implements `ChatModelListener` and routes messages to either MCP panel or Agent Logs based on message type and agent mode.

**Refactoring needed:** The class has two hard dependencies: `DevoxxGenieStateService.getInstance()` and `ApplicationManager.getApplication().getMessageBus()`. Refactor by:
1. Extracting message routing logic into a package-private method that takes settings flags as parameters
2. Making `postMcpMessage` and `postAgentMessage` non-static and injectable (or accept MessageBus as parameter)

**Test cases needed:**
- onRequest with empty messages list does nothing
- onRequest with messages.size() <= 2 does nothing
- onRequest with ToolExecutionResultMessage at penultimate position (just logs)
- onRequest with AiMessage containing text in non-agent mode → posts MCP message
- onRequest with AiMessage containing text in agent mode → posts agent message
- onRequest with AiMessage containing tool requests in non-agent mode → posts TOOL_MSG
- onRequest with AiMessage containing tool requests in agent mode → does NOT post to MCP panel
- onRequest with AiMessage with null/empty text → skips text posting
- postAgentMessage when debug logs disabled → does nothing
- postAgentMessage when exception thrown → does not propagate
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MCPListenerService has >= 80% line coverage
- [x] #2 Message routing logic is testable
- [x] #3 Both agent mode and non-agent mode paths are tested
- [x] #4 Edge cases for empty/null messages are covered
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
**Refactored** `MCPListenerService` to accept injectable dependencies via a package-private constructor:
- `Supplier<Boolean>` for agent mode and agent debug logs enabled
- `Consumer<MCPMessage>` for MCP message publishing  
- `Consumer<AgentMessage>` for agent message publishing

The public no-arg constructor defaults to the real implementations (`DevoxxGenieStateService`, `ApplicationManager.getMessageBus()`), preserving backward compatibility.

Also fixed a pre-existing compile error in `AgentRequestHandler.dispatch()` (missing `IOException` in throws clause).

**Added** `MCPListenerServiceTest` with 13 tests covering:
- Early returns: empty list, single message, two messages (size <= 2)
- ToolExecutionResultMessage: only debug logging, no publishing
- AiMessage text routing: non-agent → MCP panel, agent → agent logs
- Agent debug logs disabled: suppresses agent message posting
- Null/empty text: skips text posting, still posts tool requests
- Tool requests: posted as TOOL_MSG in non-agent mode, suppressed in agent mode
- Combined text + tools: both AI_MSG and TOOL_MSG posted in non-agent mode
- Exception handling: agent publisher throws → does not propagate
- UserMessage at penultimate: no-op (not AiMessage/ToolResult)

All 74 MCP package tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
