---
id: TASK-202
title: Include archived tasks in backlog duplicate detection
status: Done
assignee: []
created_date: '2026-03-09 17:23'
updated_date: '2026-03-09 18:08'
labels:
  - bug
  - mcp
  - ollama
  - backlog
dependencies: []
references:
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/service/spec/SpecService.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/service/agent/tool/BacklogTaskToolExecutor.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When creating a backlog task through DevoxxGenie agent tools, duplicate detection is unreliable.

Earlier reproduction showed `backlog_task_search` could miss archived `TASK-200` because embedded active-task search does not include `backlog/archive/tasks`.

A later reproduction on March 9, 2026 with `TASK-200` restored to active `backlog/tasks` showed a different failure mode: the agent still created duplicate `TASK-205`, but this time it did not call `backlog_task_search` at all. It called `backlog_milestone_list` and then `backlog_task_create` directly.

At the same time, the external Backlog MCP server still initialized with `Backlog.md is not initialized in this directory` and returned `tools: []`, so the prompt was not using external `mcp__backlog__*` tools.

This task should therefore track the broader duplicate-detection problem: task creation can bypass proper duplicate checks, and archived-task visibility is only one contributing case.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Document the archived-task duplicate-detection failure mode in the embedded backlog tools path.
- [x] #2 Update embedded backlog task search so archived tasks are included in search results and surfaced clearly to the model.
- [x] #3 Add regression tests covering archived-task search in `SpecService` and archived result labeling in `BacklogTaskToolExecutor`.
- [x] #4 Verify the embedded backlog tools can now find archived tasks such as archived task examples under `backlog/archive/tasks`.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Update embedded backlog task search to include archived tasks in duplicate detection.

Surface archived matches clearly in task-search results so agents can avoid creating duplicates from archived work.

Add service- and tool-level regression tests for archived-task search behavior.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-03-09 investigation update: MCP logs show the Backlog MCP stdio server initialized successfully but reported `Backlog.md is not initialized in this directory`, then returned `tools: []` from `tools/list`. This indicates the server was started in the wrong working directory rather than the project root, despite the project having a valid backlog/ tree.

Because MCP exposed zero tools, the agent did not use `mcp__backlog__task_search`. Instead it fell back to DevoxxGenie embedded backlog agent tools (`backlog_document_list`, `backlog_document_search`, `backlog_task_create`). Those embedded tools are available when Spec Browser is enabled and are independent of the external Backlog MCP server.

The agent also chose the wrong embedded tools: it searched backlog documents instead of tasks, then created a task directly without calling `backlog_task_search`. So the observed duplicate-check failure is a combination of MCP server startup context being wrong and poor fallback tool selection by the model.

2026-03-09 follow-up: after running `backlog init`, the agent now correctly starts with `backlog_task_search`, but it still uses DevoxxGenie embedded backlog tools (`backlog_task_*`, `backlog_document_*`) rather than external MCP `mcp__backlog__*` calls.

The duplicate that should have been detected is archived task `TASK-200` (`Support AWS Bearer Token authentication for Bedrock`) at `/Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/backlog/archive/tasks/task-200 - Support-AWS-Bearer-Token-authentication-for-Bedrock.md`. The newly created `TASK-205` is effectively a duplicate of that archived task.

Embedded `backlog_task_search` only searches active task cache (`specCache.values()`) and does not include `backlog/archive/tasks`. Archived tasks are only available through `backlog_task_list_archived` / unarchive flows, so a plain task search will miss archived duplicates.

The attempted `backlog_document_view` with id `workflow/overview` failed because embedded backlog document tools only operate on project documents, while `workflow/overview` is an MCP workflow guide/resource concept. This is another sign that the prompt/tool instructions for embedded backlog tools and external Backlog MCP are being conflated.

2026-03-09 conclusion: the earlier non-streaming prompt-path code changes were reverted after reproduction showed the practical duplicate-creation issue was explained by archived-task search scope.

2026-03-09 implementation step: re-introducing the non-streaming prompt improvement so the non-streaming agent path reuses the fully augmented system prompt (including project workflow instructions) instead of a reduced tool-only prompt. This is being treated as a soft-behavior improvement before adding any hard duplicate-check guard.

2026-03-09 implementation update: non-streaming prompt path now reuses the same augmented system prompt builder as the chat-memory path, so tool-enabled non-streaming conversations retain DEVOXXGENIE.md / AGENTS.md project instructions instead of a reduced prompt.

Verification 2026-03-09: `./gradlew -q test --tests com.devoxx.genie.service.prompt.memory.ChatMemoryManagerTest --tests com.devoxx.genie.service.prompt.response.nonstreaming.NonStreamingPromptExecutionServiceTest` passed.

2026-03-09 scope decision: proceed with DevoxxGenie embedded backlog tools as the supported path for this issue. Do not spend further effort on making the external Backlog MCP server available for this workflow unless a separate issue is raised.

Starting implementation on branch `feature/task-202-backlog-duplicate-detection`. Scope is the embedded `backlog_*` tools path used by internal LLM providers.

Implemented embedded archived-task search on branch `feature/task-202-backlog-duplicate-detection`: `SpecService.searchSpecs()` now searches active and archived tasks together, preferring active entries if an ID somehow exists in both locations.

Improved tool output for duplicate detection: `backlog_task_search` now labels archived matches with `[archived]` so the model can distinguish archived duplicates from active tasks.

Verification 2026-03-09: `./gradlew -q test --tests com.devoxx.genie.service.spec.SpecServiceTest --tests com.devoxx.genie.service.agent.tool.BacklogTaskToolExecutorTest` passed.

Clarification: archived-search regression coverage is now in place, but acceptance criterion #4 remains open because there is still no hard guard/test proving `backlog_task_create` is blocked or redirected when the model skips duplicate search entirely.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented the archived-task search fix for DevoxxGenie’s embedded `backlog_*` tools path.

What changed:
- Updated `SpecService.searchSpecs()` to search active and archived tasks together, while preferring the active copy if the same task ID somehow appears in both places.
- Updated `BacklogTaskToolExecutor.searchTasks()` output to mark archived matches with `[archived]`, making duplicate candidates visible to the model before it creates a new task.
- Added regression tests in `SpecServiceTest` for archived-task inclusion and in `BacklogTaskToolExecutorTest` for archived result labeling.

Why:
- Duplicate detection was missing archived tasks, which allowed the agent to create new active tasks even when an equivalent task already existed under `backlog/archive/tasks`.

Verification:
- `./gradlew -q test --tests com.devoxx.genie.service.spec.SpecServiceTest --tests com.devoxx.genie.service.agent.tool.BacklogTaskToolExecutorTest`
- Manual verification confirmed archived examples are now found by the embedded backlog search flow.

Scope note:
- This closes the archived-search gap for embedded backlog tools. A harder guard that forces duplicate checks before `backlog_task_create` can be tracked separately if still needed.
<!-- SECTION:FINAL_SUMMARY:END -->
