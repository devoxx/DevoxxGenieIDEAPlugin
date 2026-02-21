---
id: TASK-168
title: 'Fix java:S3776 in MCPServerDialog.java at line 537'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 12:19'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPServerDialog.java`
- **Line:** 537
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 35 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 537 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPServerDialog.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 35 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `MCPServerDialog.java:537` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored EnvVarDialog constructor in MCPServerDialog.java to reduce cognitive complexity from 35 to ~2.

Changes made:
- Extracted `EnvVarSensitivityChecker` as a package-private static inner class (follows the ServerEntryFilter pattern in MCPMarketplaceDialog) containing SENSITIVE_KEYWORDS and isSensitive() method
- Extracted `initValueField(boolean)` - creates the right field type and sets up the toggle listener
- Extracted `initKeyAndValue(String, String)` - sets initial key/value after init()
- Extracted `setValueFieldText(String)` - sets value on either JTextField or JPasswordField
- Extracted `togglePasswordVisibility()` - handles the show/hide password checkbox action
- Extracted `updateFieldType()` - now a proper class method (was nested private method inside anonymous DocumentListener)
- Extracted `replaceValueField(JComponent)` - common field-swap UI logic shared by toggle and update
- Extracted `createKeyDocumentListener()` - factory for the DocumentListener that calls updateFieldType()

The constructor now has cognitive complexity of ~1 (just the ternary for the title).

New test class added: MCPServerDialogEnvVarTest.java — 13 tests covering all sensitivity keywords, null/empty handling, case-insensitivity, and non-sensitive keys. All tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed `java:S3776` (Cognitive Complexity) in `MCPServerDialog.java` at line 537.\n\n### Problem\nThe `EnvVarDialog` constructor had a cognitive complexity of 35 (limit is 15), caused by deeply nested lambdas, anonymous inner classes with private methods, and multiple nested conditionals.\n\n### Solution\nFollowed the established `MCPMarketplaceDialog.ServerEntryFilter` pattern to decompose the monolithic constructor into focused, single-purpose methods:\n\n1. **`EnvVarSensitivityChecker`** — New package-private static inner class holding `SENSITIVE_KEYWORDS` and `isSensitive(String)`. Extracted for testability (same pattern as `ServerEntryFilter`).\n2. **`initValueField(boolean)`** — Creates the correct field type (JTextField/JPasswordField) and wires the toggle listener.\n3. **`initKeyAndValue(String, String)`** — Populates key/value fields and disables editing when in edit mode.\n4. **`setValueFieldText(String)`** — Type-safe setter for both JTextField and JPasswordField.\n5. **`togglePasswordVisibility()`** — Handles show/hide password checkbox action.\n6. **`updateFieldType()`** — Moved from anonymous DocumentListener private method to proper class method.\n7. **`replaceValueField(JComponent)`** — Common UI field-swap logic shared by toggle and update.\n8. **`createKeyDocumentListener()`** — Factory method for the DocumentListener; delegates all events to `updateFieldType()`.\n\nThe constructor itself now has cognitive complexity of ~1.\n\n### Files Modified\n- `src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPServerDialog.java`\n\n### Files Added\n- `src/test/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPServerDialogEnvVarTest.java` — 13 tests covering all 9 sensitivity keywords, null/empty key handling, case-insensitivity, and non-sensitive keys.\n\n### Test Results\nAll 13 new tests pass. All existing MCP dialog tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
