---
id: TASK-19
title: Improve ListFilesToolExecutor test coverage (17% → 70%+)
status: Done
assignee: []
created_date: '2026-02-14 08:24'
updated_date: '2026-02-14 08:52'
labels:
  - testing
  - agent-tools
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/ListFilesToolExecutor.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/ListFilesToolExecutorTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ListFilesToolExecutor has only 17% instruction coverage and 2% branch coverage (37 missed complexity, 25 missed branches). Current tests only cover empty arguments and invalid JSON.

Need to add tests covering:
- Listing files in a specific directory
- Recursive vs non-recursive listing
- File type filtering
- Hidden file handling
- Result formatting (tree-like output)
- Maximum depth limiting
- Large directory handling
- Permission errors
- Non-existent directory path handling
- Symlink handling
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Instruction coverage reaches 70%+
- [ ] #2 Branch coverage reaches 50%+
- [ ] #3 Tests cover directory listing with various options
- [ ] #4 Tests cover error handling for invalid paths
- [ ] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Refactored ListFilesToolExecutor to extract VfsUtilCore static calls into overridable instance methods (isAncestor, getRelativePath, getProjectBaseDir), enabling pure Mockito testing without IntelliJ platform dependencies. Wrote 32 comprehensive tests covering resolveTargetDir, listFiles, listDirectory, appendDirectory, truncation, constants, execute method, and null relative path handling. Coverage improved from 17% to 84% instruction / 87% branch.
<!-- SECTION:FINAL_SUMMARY:END -->
