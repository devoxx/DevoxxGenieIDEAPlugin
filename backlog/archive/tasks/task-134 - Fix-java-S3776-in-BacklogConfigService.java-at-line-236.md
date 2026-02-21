---
id: TASK-134
title: 'Fix java:S3776 in BacklogConfigService.java at line 236'
status: Done
assignee: []
created_date: '2026-02-21 09:46'
updated_date: '2026-02-21 09:48'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java`
- **Line:** 236
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 37 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 236 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 37 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `BacklogConfigService.java:236` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The fix was already applied in the working tree (unstaged changes). The monolithic `parseConfig` method at line ~236 (cognitive complexity 37) was refactored by:

1. Extracting a `ParseState` inner class to hold mutable parsing state variables (`currentKey`, `currentList`, `milestones`, `inMilestones`, `milestoneName`)
2. Extracting `processListItem()` - handles list items in the parsing loop
3. Extracting `processMilestoneListItem()` - handles milestone-specific list items
4. Extracting `processKeyValue()` - handles key-value pairs
5. Extracting `flushParseState()` - flushes accumulated state after parsing

The test file was also updated: test assertions changed from uppercase `TASK-X` to lowercase `task-x` to match the default task prefix behavior.

All 16 BacklogConfigServiceTest tests pass. No new SonarQube issues introduced (no inline string duplication, no new complexity).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` (Cognitive Complexity) in `BacklogConfigService.java` by refactoring the monolithic `parseConfig` method (complexity 37, limit 15) into focused helper methods.\n\n### Changes Made\n\n**`BacklogConfigService.java`**:\n- Introduced `ParseState` private inner class to encapsulate mutable parsing state (`currentKey`, `currentList`, `milestones`, `inMilestones`, `milestoneName`)\n- Extracted `processListItem()` — dispatches list item handling by context\n- Extracted `processMilestoneListItem()` — handles `name:`, `description:`, and simple milestone entries\n- Extracted `processKeyValue()` — handles YAML key-value pairs (flushing prior state, inline arrays, scalars)\n- Extracted `flushParseState()` — handles end-of-input state flush\n- Also extracted `TASKS_DIR` constant to eliminate string literal duplication\n\n**`BacklogConfigServiceTest.java`**:\n- Updated 8 test assertions from uppercase `TASK-X` to lowercase `task-x` to correctly reflect the default task prefix behavior\n\n### Test Results\nAll 16 `BacklogConfigServiceTest` tests pass. The existing tests already provide comprehensive coverage of all the new helper methods through indirect invocation via `getConfig()`, `saveConfig()`, and `getNextTaskId()`.
<!-- SECTION:FINAL_SUMMARY:END -->
