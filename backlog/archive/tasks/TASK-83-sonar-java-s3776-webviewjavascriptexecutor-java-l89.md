---
id: TASK-83
title: Fix java:S3776 in WebViewJavaScriptExecutor.java at line 89
status: Done
priority: high
assignee: []
created_date: '2026-02-20 18:49'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 83000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewJavaScriptExecutor.java`
- **Line:** 89
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 89 in `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewJavaScriptExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `WebViewJavaScriptExecutor.java:89` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Notes

Extracted the lambda body of `executeJavaScript` into a new private method `executeJavaScriptOnEDT(String script, int execNumber)`.

**Why:** The lambda inside `invokeLater()` added nesting level 1, and the nested `try/catch` + `if/else` blocks inside it pushed the cognitive complexity of `executeJavaScript` to 20 (limit: 15). By moving the lambda body to a separate method, the nesting resets to 0, reducing both methods well below the threshold (~7 for `executeJavaScript`, ~11 for `executeJavaScriptOnEDT`).

**Files modified:** `src/main/java/com/devoxx/genie/ui/webview/handler/WebViewJavaScriptExecutor.java`

## Final Summary

Resolved java:S3776 in `WebViewJavaScriptExecutor.java` by extracting the body of the `invokeLater` lambda in `executeJavaScript` (line 89) into a dedicated private method `executeJavaScriptOnEDT(String script, int execNumber)`.

The original method had a cognitive complexity of 20 because the lambda introduced a nesting level, and the try/catch/if/else structure inside it further compounded nesting. After extraction:
- `executeJavaScript` complexity ≈ 7 (two guard ifs with `||`, two ternaries for logging, one lambda reference)
- `executeJavaScriptOnEDT` complexity ≈ 11 (try/catch, if/else with `&&`, ternary, nested if/else)

Both are comfortably under the limit of 15. All 16 existing tests pass. No new complexity introduced — the logic is identical, only reorganized.
