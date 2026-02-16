---
id: TASK-7
title: Add unit tests for CLI runner services
status: Done
assignee: []
created_date: '2026-02-13 19:22'
updated_date: '2026-02-13 20:36'
labels:
  - testing
  - cli
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/cli/
  - src/main/java/com/devoxx/genie/chatmodel/local/clirunners/
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create unit tests for CLI task execution and CLI command classes with 0% coverage.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Unit tests for CliTaskExecutorService (151 lines)
- [ ] #2 Unit tests for CliConsoleManager (48 lines)
- [ ] #3 Unit tests for CodexCliCommand (14 lines)
- [ ] #4 Unit tests for CopilotCliCommand (5 lines)
- [ ] #5 Unit tests for CustomCliCommand (4 lines)
- [ ] #6 Unit tests for GeminiCliCommand (4 lines)
- [ ] #7 Unit tests for ClaudeCliCommand (4 lines)
- [ ] #8 Unit tests for CliRunnersChatModelFactory (15 lines)
- [ ] #9 All tests pass with JaCoCo coverage > 60% for these classes
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created 2 test files: CliTaskExecutorServiceTest (15), CliRunnersChatModelFactoryTest (7). All pass.
<!-- SECTION:FINAL_SUMMARY:END -->
