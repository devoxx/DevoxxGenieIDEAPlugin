---
id: TASK-290
title: >-
  Improve agent loop efficiency: targeted reads, traceable wrap-up, and turn
  telemetry
status: To Do
assignee: []
created_date: '2026-11-28T10:46:00Z'
labels:
  - agent
  - tools
  - trace
  - steering
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/ReadFileToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/steering/SteeringMessageInjector.java
  - src/main/java/com/devoxx/genie/service/trace/TraceEvent.java
  - src/main/java/com/devoxx/genie/service/trace/AgentTraceRecorder.java
  - src/main/java/com/devoxx/genie/service/trace/view/AgentRunModelBuilder.java
  - >-
    backlog/tasks/task-289 -
    Improve-parallel-sub-agents-based-on-Codex-multi-agent-architecture.md
  - >-
    backlog/tasks/task-255 -
    Mid-task-steering-send-chat-messages-to-a-running-agent-task-issue-1241.md
  - >-
    backlog/tasks/task-279 -
    Persist-full-agent-traces-tool-calls-results-reasoning-via-AiServices-listeners.md
priority: medium
type: enhancement
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Context

Analysis of a 34m 13s, 50-tool-call codebase audit trace showed that only ~2m 05s was spent inside tool execution (2m of which was a `parallel_explore` timeout); the remaining ~32m 08s occurred between visible tool calls (prompt processing, generation, and scheduling). Optimizing search speed will not fix this—the agent needs targeted reads, a deterministic wrap-up path, duplicate-read guards, and turn-level timing/token telemetry.

## Scope

1. **Targeted line-range reads (`ReadFileToolExecutor` / `BuiltInToolProvider`)**
   - Add optional `start_line` and `end_line` parameters (1-based, inclusive) to `read_file`.
   - Return line-numbered output with explicit truncation and continuation metadata (e.g. total lines, returned range, next `start_line`) so the agent stops falling back to `run_command`/`cat`/`run_script` and hitting the 10k/20k output caps.

2. **Traceable steering & deterministic wrap-up (`SteeringMessageInjector` / `AgentTraceRecorder` / `AgentLoopTracker`)**
   - Record mid-task steering messages injected by `SteeringMessageInjector` directly into `AgentTraceBuffer` with timestamps (currently invisible in trace exports because `chatRequestTransformer` bypasses `AiServiceStartedEvent`).
   - Add an explicit wrap-up mode distinct from hard cancel: when triggered, stop scheduling further tool calls (short-circuit in `AgentLoopTracker` and/or omit tool specs on the next round trip) and instruct the model to synthesize its final answer with any unknowns explicitly marked.

3. **Turn-level trace telemetry & span IDs (`TraceEvent` / `AgentTraceRecorder` / `AgentRunModelBuilder`)**
   - Persist `inputTokens` and `outputTokens` from `ChatResponse.tokenUsage()` on assistant trace events.
   - Record `requestStartedMs`, `firstTokenMs` (when streaming), and `responseReceivedMs` per model turn (plus provider-specific prompt-eval/eval timings when available).
   - Add `spanId` / `parentSpanId` to link nested `run_script` tool calls and `parallel_explore` sub-agent runs explicitly instead of relying solely on timestamp-window correlation in `AgentRunModelBuilder`.

4. **Lightweight duplicate-read guard (`AgentLoopTracker`)**
   - Detect identical repeated read/search calls (same tool + normalized arguments) within a turn and append a concise nudge telling the model to reuse prior context or narrow its read rather than looping.

## Coordination with Existing Tasks

- **TASK-289** already covers `parallel_explore` shared deadlines (B1), typed per-worker outcomes and partial results on timeout (B6), aggregate output caps (B10), and exposing read-only PSI tools in `ReadOnlyToolProvider` (R3). Keep sub-agent changes in TASK-289 follow-ups rather than duplicating them here; ensure the parallel partial-failure regression test (one fast worker succeeds, one times out, parent receives the successful row and unresolved status) is included there.
- **TASK-255** (mid-task steering) and **TASK-279 / TASK-283** (agent traces & run view) are the touchpoints for items 2 and 3.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 read_file supports optional 1-based start_line and end_line parameters, returns line-numbered output, and reports total lines plus truncation/continuation metadata when capped
- [ ] #2 Mid-task steering messages injected by SteeringMessageInjector are recorded in AgentTraceBuffer with timestamps and appear in trace exports
- [ ] #3 Triggering wrap-up mode prevents any further tool execution and produces a final synthesized response with zero additional tool calls
- [ ] #4 TraceEvent records per-turn inputTokens, outputTokens, request/first-token/response timestamps, and spanId/parentSpanId for nested run_script and parallel_explore calls
- [ ] #5 AgentLoopTracker detects identical repeated read/search calls within a run and appends a concise reuse/reassess nudge
- [ ] #6 Unit tests cover ReadFileToolExecutor line ranges and truncation, SteeringMessageInjector trace recording, zero tool calls after wrap-up, and duplicate-read detection
<!-- AC:END -->
