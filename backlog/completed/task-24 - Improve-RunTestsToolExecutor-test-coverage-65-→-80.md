---
id: TASK-24
title: Improve RunTestsToolExecutor test coverage (65% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 08:24'
updated_date: '2026-02-14 09:10'
labels:
  - testing
  - agent-tools
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/RunTestsToolExecutor.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/RunTestsToolExecutorTest.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
RunTestsToolExecutor has 65% instruction coverage and 48% branch coverage (28 missed complexity, 23 missed branches). Tests exist but branch coverage is low.

Need to add tests covering:
- Test execution with different build systems (Maven, npm, Cargo, Go)
- Custom test command with placeholder replacement
- Test target specification (specific class/method)
- Timeout handling during test execution
- Process interruption/cancellation
- Parsing structured vs unstructured test output
- Test result summary generation
- Error cases: build system not detected, command fails to start
- Working directory resolution
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Instruction coverage reaches 80%+
- [ ] #2 Branch coverage reaches 65%+
- [ ] #3 Tests cover multiple build systems
- [ ] #4 Tests cover timeout and cancellation
- [ ] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
RunTestsToolExecutor test coverage improved from 65% to 89% instruction / 76% branch. Refactored all private methods to package-private for testability. Wrote 38 comprehensive tests covering execute flow, resolveTestCommand, determineWorkingDirectory, getTimeout, formatResult, formatTimeoutResult, truncate, readProcessOutput. Key test pattern: override createProcess() to inject mock Process objects.
<!-- SECTION:FINAL_SUMMARY:END -->
