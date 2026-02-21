---
id: TASK-167
title: 'Fix java:S3776 in MCPMarketplaceDialog.java at line 209'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 12:14'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPMarketplaceDialog.java`
- **Line:** 209
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 33 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 209 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPMarketplaceDialog.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 33 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `MCPMarketplaceDialog.java:209` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `applyFilters()` in `MCPMarketplaceDialog.java` to fix java:S3776 (cognitive complexity 33 → well below 15). The original method contained a deeply nested lambda with multiple boolean conditions across three filter axes.

Changes made:
1. **MCPMarketplaceDialog.java** – Extracted filter logic from the lambda in `applyFilters()` into a new static nested class `ServerEntryFilter` with four methods: `matches()`, `matchesText()`, `matchesLocation()`, `matchesType()`. Each method has low cognitive complexity (≤4). The `applyFilters()` method itself now has complexity ~2.
2. **MCPMarketplaceDialogFilterTest.java** – Created new unit tests for `ServerEntryFilter`. The class is tested independently without needing IntelliJ platform (no `DialogWrapper` instantiation required). 23 tests covering all three filter axes plus combined matching pass successfully.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` (Cognitive Complexity) in `MCPMarketplaceDialog.java` at line 209. The `applyFilters()` method had a cognitive complexity of 33 (limit: 15) due to a deeply nested lambda with multiple boolean guards across three filtering dimensions.\n\n### Approach\n\nExtracted the filter logic from the lambda into a static nested class `ServerEntryFilter` with four focused methods:\n\n- `matches()` \u2013 orchestrates all three filters (complexity ~2)\n- `matchesText()` \u2013 case-insensitive text search on name and description (complexity ~3)\n- `matchesLocation()` \u2013 Local/Remote/All location filtering based on `remotes` list (complexity ~4)\n- `matchesType()` \u2013 server type filtering via `MCPRegistryService.getServerType()` (complexity ~2)\n\nThe refactored `applyFilters()` method now has a cognitive complexity of ~2. No logic was changed.\n\n### Files Modified\n\n| File | Change |\n|------|--------|\n| `src/main/java/.../MCPMarketplaceDialog.java` | Replaced complex lambda in `applyFilters()` with `ServerEntryFilter` static nested class |\n| `src/test/java/.../MCPMarketplaceDialogFilterTest.java` | New test class with 23 tests covering all filter methods |\n\n### Test Results\n\nAll 23 new tests pass:\n- `MatchesText` \u2013 7 tests (empty query, name match, desc match, case-insensitivity, no match, null handling)\n- `MatchesLocation` \u2013 7 tests (null/All/Remote/Local filters, empty remotes list)\n- `MatchesType` \u2013 5 tests (null/All/npm/remote types)\n- `Matches` \u2013 4 tests (null server, all pass, text mismatch, location mismatch)"
<!-- SECTION:FINAL_SUMMARY:END -->
