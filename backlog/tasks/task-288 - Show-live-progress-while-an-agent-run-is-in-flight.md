---
id: TASK-288
title: Show live progress while an agent run is in flight
status: In Progress
assignee: []
created_date: '2026-11-28T09:14:00Z'
updated_date: '2026-11-28T09:19:00Z'
labels:
  - agent
  - ui
  - ux
  - local-models
dependencies: []
references:
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
modified_files:
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AgentProgressLine.kt
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/components/AgentProgressLineTest.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
priority: medium
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
With a slow local model, an agent run looks stuck: on 2026-11-28 a run made 29 tool calls in 9 minutes, and later steps took 68–96 s each. The AI bubble's live status line ("Running <tool>… (step n/max)") only shows while a tool call is running, which takes milliseconds. For the 60–90 s the model spends thinking between calls, nothing says the run is still alive.

Decision (2026-11-28): no time limit for streaming agent runs (the unused agentMaxExecutionTimeSeconds setting stays non-streaming only). Show progress instead, and the user stops the run if they want.

Show a one-line progress status in the AI bubble while the run is in flight, ticking every second:
- a tool is running: "Running run_script… · step 29/50 · 6:12"
- between tool calls: "Waiting for the model · 29/50 tool calls · 6:12"
- no tool events available (agent debug logs off, since tool events are only published then): "Working… · 6:12"
It appears after about 5 s and disappears when the run completes or is stopped. Elapsed time counts from the prompt submission.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 While a response is in flight for more than ~5 s, the AI bubble shows a progress line with the elapsed time, updated every second
- [x] #2 While a tool call runs, the line names the tool and the step (n/max); between tool calls it says the agent is waiting for the model and shows the tool calls used so far (n/max)
- [x] #3 Without tool events (debug logs off) the line still shows the elapsed time
- [x] #4 The line disappears when the run completes, errors or is stopped; quick replies (under ~5 s) never show it
- [x] #5 Sub-agent tool events don't change the main agent's step count
- [x] #6 Tests cover the line's text for each state, the threshold and the elapsed formatting
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation 2026-11-28:
- New AgentProgressLine.kt: progressLineText(message, nowMs), a pure function (tested), and the AgentProgressLine composable, which re-evaluates it every second while the message is in flight (a LaunchedEffect keyed on the in-flight state, like ActivitySection's ticker).
- States: a running tool shows "Running <tool>… (step n/max)" right away, with " · m:ss" once 5 s have passed; pending approval shows "Waiting for your approval… (<tool>)"; between tool calls "Waiting for the model · n/max tool calls · m:ss"; no tool events "Working… · m:ss". Nothing under 5 s without a running tool, and nothing once terminalState isn't COMPLETED or the message is no longer in flight. Only the main agent's tool events count (subAgentId == null).
- AiBubble: the old LiveStatusLine (shown only while a tool ran) is replaced by AgentProgressLine.
- Elapsed time runs from MessageUiModel.timestamp, which is set in addUserPromptMessage when the prompt is submitted.
- Tool events (and so the step count) are only published with agent debug logs on (AgentLoopTracker.publishLogEvent); without them the line shows elapsed time only. Changing that is out of scope.
- Tests: AgentProgressLineTest (9). Full ./gradlew test --rerun passes (JDK 21). Not yet checked in the IDE.
<!-- SECTION:NOTES:END -->
