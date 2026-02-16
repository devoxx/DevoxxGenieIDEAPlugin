---
id: TASK-12
title: Add unit tests for chatmodel factories (uncovered)
status: Done
assignee: []
created_date: '2026-02-13 19:22'
updated_date: '2026-02-13 20:36'
labels:
  - testing
  - chatmodel
dependencies: []
references:
  - src/main/java/com/devoxx/genie/chatmodel/
  - src/test/java/com/devoxx/genie/chatmodel/
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create unit tests for the remaining chatmodel factory classes that have 0% coverage.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Unit tests for CustomOpenAIChatModelFactory (33 lines)
- [ ] #2 Unit tests for GrokChatModelFactory (24 lines)
- [ ] #3 Unit tests for DeepSeekChatModelFactory (24 lines)
- [ ] #4 Unit tests for LlamaChatModelFactory (23 lines)
- [ ] #5 Unit tests for LocalLLMProviderUtil (30 lines)
- [ ] #6 All tests pass with JaCoCo coverage > 60% for these classes
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created 4 test files: DeepSeekChatModelFactoryTest (6), GrokChatModelFactoryTest (7), CustomOpenAIChatModelFactoryTest (9), LlamaChatModelFactoryTest (9). All pass.
<!-- SECTION:FINAL_SUMMARY:END -->
