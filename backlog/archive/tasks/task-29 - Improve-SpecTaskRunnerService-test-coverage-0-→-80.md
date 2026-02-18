---
id: TASK-29
title: Improve SpecTaskRunnerService test coverage (0% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 09:28'
updated_date: '2026-02-14 09:47'
labels:
  - testing
  - spec-service
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SpecTaskRunnerService has 0% instruction and 0% branch coverage. It is a complex orchestration service (542 lines, 37 methods) managing task execution via CLI or LLM.

IntelliJ dependencies (hardest to test): ApplicationManager.invokeLater(), project.getMessageBus().syncPublisher(), DevoxxGenieStateService.getInstance(), CliTaskExecutorService.getInstance(), ChatMemoryService.getInstance(), javax.swing.Timer for grace period handling.

Key areas to test:
- Lifecycle: runTasks(), runAllTodoTasks(), cancel(), dispose()
- Task submission: submitNextTask(), submitTaskViaLlm(), submitTaskViaCli()
- Completion callbacks: notifyCliTaskFailed(), notifyPromptExecutionCompleted()
- Task state: markTaskCompleted(), skipTask(), advanceToNextTask()
- State queries: isRunning(), getState(), getCurrentTask(), getTotalTasks(), getCurrentTaskIndex()
- Listener management: addListener(), removeListener(), notify* methods
- Spec change handling: onSpecsChanged(), findCliTool()
- Utility: startFreshConversation(), cancelGraceTimer(), finish()

Refactoring approach: Inject dependencies via constructor or extract static lookups into overridable methods. Extract Timer logic into testable components. Mock EDT calls with synchronous execution.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage reaches 80%+
- [x] #2 Branch coverage reaches 65%+
- [x] #3 Tests cover task orchestration lifecycle
- [x] #4 Tests cover CLI and LLM task submission
- [x] #5 Tests cover listener notification
- [x] #6 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created SpecTaskRunnerServiceTest with 25 tests covering SpecTaskRunnerService (previously 0% coverage).

**Test infrastructure:** Created `RunnerMockContext` managing MockedStatic for DevoxxGenieStateService, ChatMemoryService, FileListManager, plus mocked Project with SpecService, BacklogConfigService, CliTaskExecutorService, and MessageBus/PromptSubmissionListener.

**Initial state (1 test):** Verifies IDLE state, not running, no current task, zero counts.

**Task orchestration lifecycle (6 tests):** empty list no-op, already-Done task completes immediately, task not found skipped, multiple tasks (Done + To Do) handles correctly, unsatisfied dependencies skipped, already-running ignores second call.

**LLM task submission (1 test):** Verifies prompt submission via MessageBus, chat memory clearing, file list clearing, state transitions to WAITING_FOR_COMPLETION.

**CLI task submission (5 tests):** Executes CLI tool, tool not found skips, null tool name skips, disabled tool skips, null tools list skips.

**notifyCliTaskFailed (3 tests):** Skips task and stops run with ERROR state, no-op when not waiting, blank error output uses "Unknown error" fallback.

**notifyPromptExecutionCompleted (1 test):** No-op when not waiting.

**Cancel (3 tests):** No-op when idle, stops execution when running, cancels CLI process in CLI mode.

**Listener notifications (3 tests):** Full event sequence in order, multiple listeners all notified, add/remove listeners.

**Dispose (2 tests):** Resets state when idle, cleans up when running (removes spec change listener).

**RunnerState enum (1 test):** All values exist.

Note: javax.swing.Timer-based grace period testing was excluded due to EDT headless constraints. The grace timer logic is covered by the notifyCliTaskFailed and cancel paths which also exercise cancelGraceTimer().
<!-- SECTION:FINAL_SUMMARY:END -->
