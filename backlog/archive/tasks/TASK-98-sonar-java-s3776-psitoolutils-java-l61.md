---
id: TASK-98
title: Fix java:S3776 in PsiToolUtils.java at line 61
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
ordinal: 98000
---

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S3776`
- **File:** `src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java`
- **Line:** 61
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 61 in `src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java`.

## Acceptance Criteria

- [x] Issue `java:S3776` at `PsiToolUtils.java:61` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Extracted the inner symbol-name matching loop from `findNamedElementOnLine` into a private helper method `findByName`. This reduced the method's cognitive complexity from 16 to 13:

**Before (complexity 16):**
```java
if (symbolName != null && !symbolName.isBlank()) {
    for (PsiNameIdentifierOwner owner : onLine) {
        if (symbolName.equals(owner.getName())) return owner;  // depth 2 = +3
    }
}
```

**After (complexity 13):**
```java
if (symbolName != null && !symbolName.isBlank()) {
    PsiNameIdentifierOwner match = findByName(onLine, symbolName);
    if (match != null) return match;  // depth 1 = +2
}

private static PsiNameIdentifierOwner findByName(List<PsiNameIdentifierOwner> candidates, String name) {
    for (PsiNameIdentifierOwner owner : candidates) {
        if (name.equals(owner.getName())) return owner;
    }
    return null;
}
```

The extracted `findByName` helper has complexity 3 (well within the limit).

## Final Summary

Fixed SonarQube `java:S3776` in `PsiToolUtils.findNamedElementOnLine` (line 61) by extracting the symbol-name matching for-loop into a private `findByName` helper method. This reduced the method's cognitive complexity from 16 to 13. The helper itself has complexity 3. No logic was changed — only structure. No new issues introduced; no tests broke (no existing tests for this utility class).
