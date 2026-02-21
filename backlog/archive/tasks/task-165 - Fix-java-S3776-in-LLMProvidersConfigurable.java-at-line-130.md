---
id: TASK-165
title: 'Fix java:S3776 in LLMProvidersConfigurable.java at line 130'
status: Done
assignee: []
created_date: '2026-02-21 11:14'
updated_date: '2026-02-21 12:05'
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
- **File:** `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java`
- **Line:** 130
- **Severity:** High impact on Maintainability
- **Issue:** Refactor this method to reduce its Cognitive Complexity from 35 to the 15 allowed.

## Task

Fix the SonarQube issue `java:S3776` at line 130 in `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java`.
<!-- SECTION:DESCRIPTION:END -->

# Fix `java:S3776`: Refactor this method to reduce its Cognitive Complexity from 35 to the 15 allowed.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Issue `java:S3776` at `LLMProvidersConfigurable.java:130` is resolved
- [x] #2 No new SonarQube issues introduced by the fix
- [x] #3 Verify that all relevant tests still pass (DO NOT run the full test suite)
- [x] #4 If the modified code lacks tests, add new ones to cover the changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed java:S3776 in LLMProvidersConfigurable.java (both worktree and main source).

Worktree file `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java` had a large inline boolean expression in `apply()` (lines 203-219) with 16 `||` and multiple `&&` operators inside an `if` block, making cognitive complexity ~35.

The main source file `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java` was already fixed (committed earlier) with the helper method approach.

For the worktree file, applied the same fix: extracted the complex inline `hasKey` boolean into 4 private helper methods:
- `isAnyApiKeyEnabled(DevoxxGenieStateService)` — top-level delegator
- `hasEnabledMainCloudKey(DevoxxGenieStateService)` — OpenAI, Anthropic, Mistral, Gemini, Groq, DeepInfra
- `hasEnabledAuxCloudKey(DevoxxGenieStateService)` — DeepSeek, OpenRouter, Grok, Kimi, Glm, CustomOpenAI
- `hasEnabledAwsOrAzureKey(DevoxxGenieStateService)` — AWS and Azure keys

Each helper method has cognitive complexity ≤ 11, and `apply()` now has complexity of 2.

Added `src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurableTest.java` with 9 tests covering `isAnyApiKeyEnabled` logic. All tests pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Fix java:S3776 in LLMProvidersConfigurable.java\n\n### Problem\nThe `apply()` method at line 130 of `LLMProvidersConfigurable.java` had a cognitive complexity of ~35 (exceeding the allowed 15). This was caused by a large inline boolean expression with 16 `||` and multiple `&&` operators nested inside an `if (isModified)` block.\n\n### Solution\nExtracted the complex inline boolean expression into 4 focused private helper methods:\n\n1. `isAnyApiKeyEnabled(DevoxxGenieStateService)` — top-level orchestrator, complexity: 2\n2. `hasEnabledMainCloudKey(DevoxxGenieStateService)` — covers OpenAI, Anthropic, Mistral, Gemini, Groq, DeepInfra; complexity: 11\n3. `hasEnabledAuxCloudKey(DevoxxGenieStateService)` — covers DeepSeek, OpenRouter, Grok, Kimi, Glm, CustomOpenAI; complexity: 11\n4. `hasEnabledAwsOrAzureKey(DevoxxGenieStateService)` — covers AWS and Azure keys; complexity: 8\n\nThe `apply()` method now delegates to `isAnyApiKeyEnabled()` and has complexity reduced to 2.\n\n### Files Modified\n- `.claude/worktrees/sunny-exploring-lemon/src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java` — primary fix target\n- The main source file `src/main/java/...` already had the fix applied in a previous commit\n\n### Files Added\n- `src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurableTest.java` — 9 new tests covering `isAnyApiKeyEnabled` and its delegate methods\n\n### Test Results\nAll 9 new tests pass. No existing tests broken."]
<!-- SECTION:FINAL_SUMMARY:END -->
