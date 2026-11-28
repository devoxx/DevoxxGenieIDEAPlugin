---
id: TASK-287
title: >-
  Keep agent context small for local models: cap tool output and shrink old tool
  results
status: To Do
assignee: []
created_date: '2026-11-28T09:11:00Z'
labels:
  - agent
  - performance
  - local-models
  - design
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
priority: medium
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while investigating a stopped agent run on 2026-11-28 (conversation 1795856776459, Ollama inkling-2-mini:26b-a5b-nvfp4, the ReadAccess prompt). The run made 29 tool calls, all run_script, without hitting the tool-call limit (50). Its steps took 5–13 s each for the first 25 calls, then 68–96 s each: 181,000 characters (~45k tokens) of tool output had accumulated in the conversation, and five run_script results hit their 20,000-character output cap. A local model re-processes the whole context on every step, so each step gets slower as results pile up, and the run looks stuck.

The trace also shows the model reading files in small sed slices and retrying greps that found nothing, so much of that context was low value.

Ideas to evaluate (this changes what the model sees, so design first):
- A smaller per-call output cap for local providers (run_script, run_command, read_file), configurable, telling the model how to page ("print less" / ranges).
- Shrinking older tool results in chat memory once the context passes a threshold: keep the last N results in full and replace older ones with a short summary or a "(result truncated, re-run the tool to see it)" stub.
- Showing context growth to the user during a run (the context-window label only updates when a response completes).

Related: the run's 50-call limit and the round-trip cap (limit + AGENT_LOOP_ROUND_TRIP_GRACE, issue #1188) bound the number of steps, but not the time per step.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A design note in this task compares the options above on measured runs (at least the ReadAccess prompt on a local model) before code changes
- [ ] #2 Agent runs on local models keep a bounded context: tool output per call and the total kept in memory stay within configurable limits
- [ ] #3 The model is told when a result was truncated or shrunk and how to get the rest
- [ ] #4 Re-running the ReadAccess prompt on the same local model shows step times no longer growing past the first steps' range
<!-- AC:END -->
