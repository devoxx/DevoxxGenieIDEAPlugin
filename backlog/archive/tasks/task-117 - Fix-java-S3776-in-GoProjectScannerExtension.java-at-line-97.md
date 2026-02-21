---
id: task-117
title: 'Fix java:S3776 in GoProjectScannerExtension.java at line 97'
status: Done
assignee: []
created_date: '2026-02-20 21:58'
updated_date: '2026-02-21 08:04'
labels:
  - sonarqube
  - java
dependencies: []
priority: high
ordinal: 117000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java`
- **Line:** 97
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 97 in `src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `GoProjectScannerExtension.java:97` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 All existing tests continue to pass
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fix already applied via TASK-110 (now archived). The file `GoProjectScannerExtension.java` was modified to extract go.sum inspection logic from `detectGoFrameworks` into a new private method `detectGoFrameworksFromGoSum`. This reduced the cognitive complexity of `detectGoFrameworks` (line 97) from 19 to 6, well below the threshold of 15. No further code changes needed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
TASK-117 is a duplicate of TASK-110, which was already completed and archived. The fix was already applied to `GoProjectScannerExtension.java`:\n\n**Root cause:** `detectGoFrameworks` (line 97) had cognitive complexity 19 due to a deeply nested try/catch block inside an `if (goSum != null)` block, containing 5 if-else-if framework checks plus 3 more if blocks for ORM and GraphQL.\n\n**Fix applied (in TASK-110):** Extracted the go.sum content inspection into a new private method `detectGoFrameworksFromGoSum(VirtualFile goSum, Map<String, Object> goInfo)`. The original `detectGoFrameworks` now delegates to this helper.\n\n**Result:**\n- `detectGoFrameworks` complexity: 19 → 6 (if-null check + 5-branch if-else-if chain)\n- `detectGoFrameworksFromGoSum` complexity: 9 (try/catch + 5-branch chain + 2 independent ifs)\n- Both well below the 15-point threshold\n- No functional changes; all logic preserved
<!-- SECTION:FINAL_SUMMARY:END -->
