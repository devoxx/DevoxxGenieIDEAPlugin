---
id: TASK-173
title: 'Fix java:S3776 in CppProjectScannerExtension.java at line 21'
status: Done
assignee: []
created_date: '2026-02-21 12:33'
updated_date: '2026-02-21 13:01'
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
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtension.java`
- **Line:** 21
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 21 in `src/main/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtension.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `CppProjectScannerExtension.java:21` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored CppProjectScannerExtension.java to reduce cognitive complexity from 19 to ~4.

Files modified:
- src/main/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtension.java
  - Extracted detectBuildSystem() private method (handles CMake/Make/Bazel/VS + Conan detection)
  - Extracted detectQualityTools() private method (handles .clang-tidy and .clang-format detection)
  - Extracted detectTestFramework() private method (handles GoogleTest/Catch2/Boost.Test detection)
  - enhanceProjectInfo() now has complexity ~4 (if/||, if, catch)
  - Each extracted method has complexity well below 15

- src/test/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtensionTest.java (NEW)
  - 16 tests covering all refactored branches
  - Pattern: mock VfsUtilCore.loadText(any(VirtualFile.class)) not VfsUtil.loadText (VfsUtil delegates to VfsUtilCore; using specific mock ref causes Mockito recorder-state issue in IntelliJ JBR)
  - Tests: guard tests, build system detection (CMake/Make/Bazel), quality tools (clang-tidy/clang-format), test frameworks (GoogleTest/Catch2/Boost.Test), build commands

All 16 tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nFixed SonarQube issue `java:S3776` in `CppProjectScannerExtension.java` at line 21 by extracting private helper methods to reduce cognitive complexity from 19 to ~4.\n\n### Changes\n\n**`CppProjectScannerExtension.java`** — Refactored `enhanceProjectInfo()` by extracting three private methods:\n- `detectBuildSystem(VirtualFile, Map)` — CMake/Make/Bazel/VS detection plus Conan dependency manager\n- `detectQualityTools(VirtualFile, Map)` — clang-tidy and clang-format detection\n- `detectTestFramework(VirtualFile, Map)` — GoogleTest/Catch2/Boost.Test detection\n\nThe original method now has complexity ~4; each extracted method stays well under 15.\n\n**`CppProjectScannerExtensionTest.java`** (new) — 16 tests covering all detection branches. Key pattern: mock `VfsUtilCore.loadText(any(VirtualFile.class))` instead of `VfsUtil.loadText` because `VfsUtil.loadText` delegates to `VfsUtilCore.loadText`, and using the specific mock reference directly in the `MockedStatic.when()` lambda triggers a Mockito recorder-state bug in IntelliJ's JBR environment.\n\nAll 16 tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
