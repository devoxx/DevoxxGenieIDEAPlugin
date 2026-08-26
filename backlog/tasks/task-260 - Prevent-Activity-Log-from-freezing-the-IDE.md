---
id: TASK-260
title: Prevent Activity Log from freezing the IDE
status: Done
assignee:
  - Codex
created_date: '2026-08-26 11:28'
updated_date: '2026-08-26 11:48'
labels:
  - bug
  - performance
  - ui
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
  - src/main/java/com/devoxx/genie/service/mcp/MCPLogMessageHandler.java
  - /Users/stephan/Library/Logs/JetBrains/IntelliJIdea2026.2/idea.log
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The DevoxxGenie Activity Log can block IntelliJ's UI thread for tens of seconds after MCP and agent debug traffic accumulates. Keep diagnostic logging usable without degrading overall IDE responsiveness, while preserving access to complete log payloads.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Activity Log updates remain responsive after the configured retention limit is reached under sustained MCP or agent traffic
- [x] #2 Displayed row previews are bounded while complete log content remains available through existing detailed-view and copy workflows
- [x] #3 Automatic scrolling follows new entries only when the user is already following the log tail
- [x] #4 Log retention and filtering remain correct when entries are added or pruned in batches
- [x] #5 Pause, clear, filtering, source colors, and double-click behavior continue to work
- [x] #6 Automated regression tests cover preview bounding and the updated list behavior
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Bound the displayed preview for every Activity Log source while preserving each entry's full content for double-click and copy/export workflows.
2. Replace Swing HTML row rendering with a lightweight plain-text colored renderer and fixed single-line cells so list layout does not repeatedly parse HTML or measure variable-height entries.
3. Track whether the viewport is following the tail and autoscroll only in that state; preserve manual scroll position when the user scrolls upward.
4. Coalesce pending additions and retention pruning into bulk list-model notifications, keeping filtering and ordering correct.
5. Add regression tests for MCP preview bounding, bulk retention/filter behavior, and tail-follow decisions while preserving pause, clear, colors, filtering, and double-click behavior.
6. Run focused Activity Log tests followed by the relevant project test suite; document evidence and finalize TASK-260.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Investigation evidence: IntelliJ IDEA 2026.2.1 attributed a 26.625-second UI freeze to com.devoxx.genie. All 20 captured freeze reports contain AgentMcpLogPanel; the EDT stack runs through scrollToBottom, variable-height JList layout, the HTML cell renderer, and BasicHTML parsing. Both MCP and agent debug logging were enabled.

Implementation plan approved by the user on 2026-08-26.

Implemented bounded 500-character single-line previews for every source while retaining full copy/editor payloads.

Replaced Swing HTML and variable-height rows with plain-text fixed-height rendering; removed ensureIndexIsVisible from the append path.

Added tail-aware scrolling, a single disposable flush timer, and bulk add/remove/replace model operations.

Focused tests: AgentMcpLogPanelFormatTest and AgentMcpLogPanelPerformanceTest passed (26 tests).

Final verification: full ./gradlew test -q passed; focused Activity Log tests passed again after lowering default retention to 250; git diff --check passed.

Final review removed the redundant multiline agent-preview allocation so large agent results are bounded during the first formatting pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented the Activity Log freeze fix on branch `fix/task-260-activity-log-freeze`.

- Replaced variable-height Swing HTML rows with fixed-height plain-text rendering, eliminating repeated BasicHTML parsing and full-list row measurement on the EDT.
- Bounded every displayed preview to 500 characters in a single pass while preserving complete clipboard and double-click editor content.
- Changed automatic scrolling to follow new entries only when the visible viewport was already at the tail, using the scrollbar instead of `JList.ensureIndexIsVisible`.
- Added one disposable coalescing timer and bulk model add/remove/replace operations; retention remains correct for filtered views and oversized incoming batches.
- Reduced default in-memory retention from 1,000 to 250 entries while retaining the configurable override.
- Added regression tests covering million-character payload bounding, 1,000-row bulk retention events, filtered pruning, and tail-follow decisions; updated formatting tests for the single-line preview contract.

Verification:
- Focused Activity Log tests passed after the final change.
- Full `./gradlew test -q` suite passed.
- `git diff --check` passed.

Behavioral note: list rows intentionally show bounded single-line previews separated by ↵ markers; full multi-line payloads remain available through copy and double-click.
<!-- SECTION:FINAL_SUMMARY:END -->
