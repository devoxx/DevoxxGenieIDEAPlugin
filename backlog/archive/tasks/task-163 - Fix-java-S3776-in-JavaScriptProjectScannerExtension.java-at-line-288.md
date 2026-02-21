---
id: TASK-163
title: 'Fix java:S3776 in JavaScriptProjectScannerExtension.java at line 288'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 11:51'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java`
- **Line:** 288
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 288 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `JavaScriptProjectScannerExtension.java:288` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation Notes

### Files Modified
- `src/test/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtensionTest.java` (CREATED)

### Files Unchanged
- `src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java` — the `enhanceBuildSystem` method at line 288 was already refactored (cognitive complexity reduced from 27 to ~2) in an earlier commit. The refactoring extracted 6 helper methods: `getRunCommand`, `getInstallCommand`, `addTestCommands`, `addE2eTestCommands`, `addLintFormatCommands`, `addFrameworkCommands`.

### What Was Done
Added a comprehensive test class with 23 tests covering all the refactored helper methods:
- Guard tests (skip non-JS projects, null languages, null baseDir)
- Default npm commands when no package.json present
- Yarn/pnpm detection via lock file presence
- Jest, Mocha test framework detection
- Cypress, Playwright e2e framework detection
- ESLint, Prettier lint/format tool detection
- React, Next.js, Vue, Nuxt.js, Express, SvelteKit, Angular framework detection
- Angular-specific yarn vs npx generate command selection

### Technical Note on Mocking
`MockedStatic<VfsUtil>` cannot intercept `VfsUtil.loadText` in this IntelliJ test environment due to classloader isolation. The fix is to mock `VfsUtilCore.loadText(any(VirtualFile.class))` instead (since VfsUtil.loadText delegates to VfsUtilCore.loadText). Using `any()` as the argument matcher avoids referencing the VirtualFile mock in the lambda, preventing Mockito recorder-state issues.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary\n\nThe `java:S3776` issue at `JavaScriptProjectScannerExtension.java:288` was already resolved in an earlier commit on the `sonarlint-fixes` branch. The `enhanceBuildSystem` method had been refactored from a single complex method (cognitive complexity 27) into a clean orchestrator calling 6 focused helper methods: `getRunCommand`, `getInstallCommand`, `addTestCommands`, `addE2eTestCommands`, `addLintFormatCommands`, and `addFrameworkCommands` — reducing the complexity to ~2.\n\n### Action Taken\n\nCreated `JavaScriptProjectScannerExtensionTest.java` with 23 tests covering all refactored helper methods. All 23 tests pass.\n\n### Key Technical Finding\n\n`MockedStatic<VfsUtil>` cannot intercept `VfsUtil.loadText` in this IntelliJ classloader-isolated test environment. The workaround is to mock `VfsUtilCore.loadText(any(VirtualFile.class))` instead (VfsUtil.loadText delegates to VfsUtilCore.loadText), using `any()` to avoid Mockito recorder-state issues.\n\n### Tests Coverage\n- Guard paths (non-JS, null languages, null base dir)\n- Default npm commands\n- Yarn/pnpm detection via lock file\n- Jest and Mocha test framework detection with appropriate commands\n- Cypress and Playwright e2e framework detection\n- ESLint and Prettier tool detection\n- React, Next.js, Vue, Nuxt.js, Express.js, SvelteKit, Angular framework detection\n- Angular `ng generate` command with yarn vs npx variation
<!-- SECTION:FINAL_SUMMARY:END -->
