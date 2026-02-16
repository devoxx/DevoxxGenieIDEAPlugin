---
id: TASK-22
title: Improve ReadFileToolExecutor test coverage (26% → 70%+)
status: Done
assignee: []
created_date: '2026-02-14 08:24'
updated_date: '2026-02-14 08:59'
labels:
  - testing
  - agent-tools
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/ReadFileToolExecutor.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/ReadFileToolExecutorTest.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ReadFileToolExecutor has only 26% instruction coverage and 25% branch coverage (17 missed complexity, 8 missed branches). Current tests only cover missing/empty path validation and invalid JSON.

Need to add tests covering:
- Successful file reading (small and large files)
- Reading files with line number ranges (offset/limit)
- Binary file detection and handling
- File encoding handling (UTF-8, other encodings)
- Non-existent file error handling
- Permission denied errors
- Response formatting with line numbers
- Content truncation for very large files
- Reading empty files
- Path traversal security
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Instruction coverage reaches 70%+
- [ ] #2 Branch coverage reaches 50%+
- [ ] #3 Tests cover actual file reading operations
- [ ] #4 Tests cover line range reading
- [ ] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Refactored ReadFileToolExecutor to extract the ReadAction.compute() lambda logic into a package-private readFile() method, and extracted getProjectBaseDir(), findFile(), isAncestor() as overridable methods. Added MockedStatic<ReadAction> tests for end-to-end execute() coverage. Grew from 3 tests to 19 tests covering: input validation (5), execute end-to-end with MockedStatic (2), readFile logic (12 - null base, file not found, access denied, directory, valid file, empty file, multiline, unicode, special chars, large content, IO exception). Coverage improved from 26% to 75% instruction / 70% branch.
<!-- SECTION:FINAL_SUMMARY:END -->
