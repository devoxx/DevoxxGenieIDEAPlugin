---
id: TASK-81
title: Fix java:S3776 in CppProjectScannerExtension.java at line 21
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:46'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 81000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtension.java`
- **Line:** 21
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 21 in `src/main/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtension.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `CppProjectScannerExtension.java:21` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted test framework detection logic from `enhanceProjectInfo` into a new private helper method `detectTestFramework(VirtualFile baseDir)`.

**Root cause:** The `enhanceProjectInfo` method had a cognitive complexity of 19, exceeding the allowed limit of 15. The complexity came from:
- Multiple boolean assignments with `||` operators (5 × +1 = 5)
- Multiple `if`/`else if` branches (12 structural branches)
- A `catch` block (+1)

**Fix:** Extracted the three boolean assignments (`hasGTest`, `hasCatch`, `hasBoostTest`) and the if/else-if chain into a `detectTestFramework` method that returns the framework name as a `String` (or `null` if none detected). This removed 5 complexity points from `enhanceProjectInfo` (3 `||` operators + 3 structural branches = 6 removed, replaced by 1 simple `if (testFramework != null)` = +1), reducing the complexity from 19 to 14.

**Files modified:**
- `src/main/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtension.java`

## Final Summary

Resolved SonarQube rule `java:S3776` in `CppProjectScannerExtension.java` by extracting the C++ test framework detection logic into a dedicated `detectTestFramework(VirtualFile baseDir)` helper method.

The `enhanceProjectInfo` method previously computed three boolean variables (`hasGTest`, `hasCatch`, `hasBoostTest`) using `||` expressions and then used an if/else-if chain to set the test framework — all inline — contributing 6 complexity points that pushed the total to 19. By moving this logic to `detectTestFramework`, the main method's complexity drops to 14 (below the threshold of 15). The new helper method itself has a complexity of only 6, well within limits. No functional behaviour was changed.
