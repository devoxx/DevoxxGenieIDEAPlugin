---
id: TASK-21
title: Improve EditFileToolExecutor test coverage (25% → 70%+)
status: Done
assignee: []
created_date: '2026-02-14 08:24'
updated_date: '2026-02-14 08:57'
labels:
  - testing
  - agent-tools
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/EditFileToolExecutor.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/EditFileToolExecutorTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
EditFileToolExecutor has only 25% instruction coverage and 29% branch coverage (49 missed complexity, 20 missed branches). Current tests cover argument validation and path traversal security but not the actual edit operations.

Need to add tests covering:
- Successful string replacement in files
- Multiple occurrences handling (replace first vs replace all)
- String not found in file
- File read before edit
- Preserving file encoding and line endings
- Edge cases: replacement at start/end of file
- Replacement creating/removing lines
- Large file editing
- Regex vs literal replacement
- Response formatting with diff preview
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Instruction coverage reaches 70%+
- [ ] #2 Branch coverage reaches 50%+
- [ ] #3 Tests cover actual file editing operations
- [ ] #4 Tests cover string not found scenario
- [ ] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Refactored EditFileToolExecutor to extract the inline WriteCommandAction lambda into a package-private editFile() method, and extracted getProjectBaseDir(), findFile(), isAncestor() as overridable methods. Made countOccurrences() package-private static. Wrote 33 comprehensive tests covering: input validation (10), execute end-to-end with MockedStatic (2), editFile logic (15 - null base, file not found, directory, access denied, not found string, single/multiple replacements, replaceAll, boundary cases, delete via empty replacement, multiline, exceptions), countOccurrences (6). Coverage improved from 25% to 87% instruction / 78% branch.
<!-- SECTION:FINAL_SUMMARY:END -->
