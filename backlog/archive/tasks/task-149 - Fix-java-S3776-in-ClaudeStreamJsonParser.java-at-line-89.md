---
id: TASK-149
title: 'Fix java:S3776 in ClaudeStreamJsonParser.java at line 89'
status: Done
assignee: []
created_date: '2026-02-21 10:41'
updated_date: '2026-02-21 10:52'
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
- **File:** `src/main/java/com/devoxx/genie/service/cli/ClaudeStreamJsonParser.java`
- **Line:** 89
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 89 in `src/main/java/com/devoxx/genie/service/cli/ClaudeStreamJsonParser.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `ClaudeStreamJsonParser.java:89` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `parseAssistant` in `ClaudeStreamJsonParser.java` to reduce cognitive complexity from 20 to ~5 by extracting the loop body into a new private helper method `parseAssistantContentItem`. The new method handles the `switch` on content type at nesting depth 0 (instead of depth 1 inside the for loop), which removes the accumulated nesting penalty. No logic was changed; only the structure was reorganised. All 16 existing tests in `ClaudeStreamJsonParserTest` pass. No new tests were required as existing coverage fully exercises the refactored paths.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\n**File modified:** `src/main/java/com/devoxx/genie/service/cli/ClaudeStreamJsonParser.java`\n\n### Problem\nThe `parseAssistant` method (line 89) had a SonarQube cognitive complexity of 20, exceeding the allowed maximum of 15. The complexity accumulated from:\n- Early-return guards at the top level\n- A `for` loop (nesting +1)\n- `if` guards inside the loop (nesting +2)\n- A `switch` inside the loop (nesting +2)\n- An `if` with `&&` inside a switch case (nesting +3, plus boolean operator penalty)\n- Ternary operators inside switch cases\n\n### Fix\nExtracted the loop body into a new private helper method `parseAssistantContentItem(JsonObject, String, List<AgentMessage>)`. \n\n- `parseAssistant` is now reduced to: two guard returns, one for-loop, one if-check — complexity ~5.\n- `parseAssistantContentItem` contains the switch and its cases at nesting depth 0 — complexity ~8.\n- Both methods are comfortably below the 15-point ceiling.\n- No logic was changed; only the structural decomposition differs.\n\n### Tests\nAll 16 tests in `ClaudeStreamJsonParserTest` pass. Existing tests fully cover the refactored `assistant/text` and `assistant/tool_use` paths, so no new tests were required.
<!-- SECTION:FINAL_SUMMARY:END -->
