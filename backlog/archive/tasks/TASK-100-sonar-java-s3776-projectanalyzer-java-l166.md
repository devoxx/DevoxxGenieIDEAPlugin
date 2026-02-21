---
id: TASK-100
title: Fix java:S3776 in ProjectAnalyzer.java at line 166
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:50'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 100000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/analyzer/ProjectAnalyzer.java`
- **Line:** 166
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 30 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 166 in `src/main/java/com/devoxx/genie/service/analyzer/ProjectAnalyzer.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `ProjectAnalyzer.java:166` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Refactored `detectLanguages()` in `ProjectAnalyzer.java` (line 166) by extracting four helper methods:

1. `detectLanguagesFromProjectFiles(Set, Map)` – handles the 6 top-level project-file checks (Cargo.toml, pom.xml, build.gradle, go.mod, package.json, CMakeLists.txt). Complexity: ~8.
2. `scanSourceFilesForLanguages(Set, Map)` – wraps the `VfsUtil.visitChildrenRecursively` call; the visitor body is minimal. Complexity: ~2.
3. `updateLanguagesFromFile(VirtualFile, Set, Map)` – safely checks `isInContent` and delegates extension lookup. Complexity: ~3.
4. `getLanguageForExtension(String)` – pure switch expression mapping file extension → language constant. Complexity: ~1.
5. `determinePrimaryLanguage(Set, Map)` – finds the language with the most files. Complexity: ~2.

The refactored `detectLanguages()` method now has a cognitive complexity of ~1 (a single `if` guard).
No behavioral changes were made — all logic is preserved identically across the extracted methods.

## Final Summary

Fixed SonarQube rule `java:S3776` in `ProjectAnalyzer.java` at line 166 by decomposing the monolithic `detectLanguages()` method (cognitive complexity 30) into five focused helper methods, each well below the 15-point threshold. The refactoring is purely structural: no logic was changed, and all existing tests continue to pass.
