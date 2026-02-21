---
id: task-106
title: Fix java:S3776 in FindSymbolsToolExecutor.java at line 53
status: Done
priority: high
assignee: []
created_date: '2026-02-20 21:48'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 106000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java`
- **Line:** 53
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 53 in `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `FindSymbolsToolExecutor.java:53` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

**File modified:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java`

**Root cause:** The `findSymbols` method had a deeply nested lambda passed to `processElementsWithWord`. The lambda contained 4 levels of `if` nesting, each level adding an extra point to cognitive complexity. Total was 19 (limit 15).

Complexity breakdown before fix:
- `if (projectBase == null)` → +1
- `if (parent instanceof ...)` inside lambda → +2 (nesting level 1)
- `if (nameId != null && ...)` → +3 + 1(&&) = +4 (nesting level 2)
- `if (matchesKind(...))` → +4 (nesting level 3)
- `if (formatted != null)` → +5 (nesting level 4)
- 3 more control-flow statements → +3
- **Total: 19**

**Fix:** Extracted the lambda body to a new private method `processFoundElement(PsiElement, List<String>, String, VirtualFile)` using guard clauses (early returns) to flatten nesting. The lambda in `findSymbols` is now a single method call with no nested control flow.

Complexity after fix:
- `findSymbols`: 4 (4 flat control-flow statements)
- `processFoundElement`: 6 (`if`+1, `if`+`||`+2, `if`+1, nested `if`+2)
- Both well under the 15 limit.

## Final Summary

Reduced cognitive complexity of `findSymbols` from 19 to 4 by extracting the `processElementsWithWord` lambda body into a new private method `processFoundElement`. The new method uses guard clauses (inverted conditions with early returns) instead of nested `if` blocks, resulting in a flat structure with complexity of 6. No behaviour changes — the logic is identical, just restructured. No new imports needed. All existing tests continue to pass.

status: Done
