---
id: TASK-64
title: Fix java:S1602 in RAGSettingsComponent.java at line 235
status: Done
priority: low
assignee: []
created_date: '2026-02-20 18:22'
labels:
  - sonarqube
  - java
dependencies: []
references: []
documentation: []
ordinal: 64000
---

# Fix `java:S1602`: Remove useless curly braces around statement

## Description

SonarQube for IDE detected a code quality issue.

- **Rule:** `java:S1602`
- **File:** `src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java`
- **Line:** 235
- **Severity:** Low impact on Maintainability
- **Issue:** Remove useless curly braces around statement

## Task

Fix the SonarQube issue `java:S1602` at line 235 in `src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java`.

## Acceptance Criteria

- [x] Issue `java:S1602` at `RAGSettingsComponent.java:235` is resolved
- [x] No new SonarQube issues introduced by the fix
- [x] All existing tests continue to pass

## Implementation Notes

Changed the `SwingUtilities.invokeLater` lambda at line 235 from a block body with curly braces to a concise expression body, since it contained only a single statement.

**Before:**
```java
SwingUtilities.invokeLater(() -> {
    progressLabel.setText("Stopping indexing process...");
});
```

**After:**
```java
SwingUtilities.invokeLater(() -> progressLabel.setText("Stopping indexing process..."));
```

## Final Summary

Fixed SonarQube rule `java:S1602` ("Lambdas containing only one statement should not nest this statement in a block") in `RAGSettingsComponent.java` at line 235. The fix removes unnecessary curly braces from the single-statement lambda passed to `SwingUtilities.invokeLater()`, making the code more concise and idiomatic. No logic was changed.
