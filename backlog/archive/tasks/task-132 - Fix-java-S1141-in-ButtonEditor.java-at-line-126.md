---
id: task-132
title: 'Fix java:S1141 in ButtonEditor.java at line 126'
status: Done
assignee: []
created_date: '2026-02-21 08:24'
updated_date: '2026-02-21 08:25'
labels:
  - sonarqube
  - java
dependencies: []
priority: medium
ordinal: 1000
---

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1141`
- **File:** `ButtonEditor.java`
- **Line:** 126
- **Severity:** Medium impact on Maintainability
- **Issue:** Extract this nested try block into a separate method.

Fix the SonarQube issue `java:S1141` at line 126 in `ButtonEditor.java`.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S1141` at `ButtonEditor.java:126` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 All existing tests continue to pass
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed java:S1141 (nested try-catch) in ButtonEditor.java.

The inner try-catch at line 126 (counting documents per collection) was nested inside the outer try-catch starting at line 110 (loading all collections). This violates SonarQube rule S1141 which disallows nested try-catch blocks.

Fix: extracted the inner try-catch into a new private helper method `getDocumentCount(String collectionId)` at line 108. The method returns 0 when an IOException occurs (preserving original behavior). The `safeLoadCollections()` loop now simply calls `getDocumentCount(collection.id())` directly.

File modified: src/main/java/com/devoxx/genie/ui/settings/rag/table/ButtonEditor.java
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Fix java:S1141 in ButtonEditor.java at line 126\n\n### Problem\nSonarQube rule `java:S1141` (nested try-catch blocks) was violated in `ButtonEditor.java`. The `safeLoadCollections()` method had an inner `try-catch (IOException)` nested inside an outer `try-catch (IOException)`:\n- **Outer** catch (line ~110): handles failures loading the full collection list\n- **Inner** catch (line ~126): handles failure counting documents for an individual collection\n\n### Fix\nExtracted the inner try-catch into a new private helper method `getDocumentCount(String collectionId)` that:\n- Returns the count of documents for the given collection ID\n- Catches `IOException` internally and returns 0 as a fallback (preserving original behavior)\n\nThe `for` loop in `safeLoadCollections()` now calls `getDocumentCount(collection.id())` directly, eliminating the nesting.\n\n### Files Modified\n- `src/main/java/com/devoxx/genie/ui/settings/rag/table/ButtonEditor.java`\n  - Added `private int getDocumentCount(String collectionId)` method (lines 108–115)\n  - Simplified the `for` loop in `safeLoadCollections()` to use the new helper (lines 134–141)\n\n### No Regressions\n- No new SonarQube issues introduced (no new catch blocks at wrong scope, no logic changes)\n- No existing test class for ButtonEditor; it is a Swing table cell editor without unit tests\n- Behavior is identical: failures counting documents for individual collections still result in 0 being shown
<!-- SECTION:FINAL_SUMMARY:END -->
