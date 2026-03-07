---
id: TASK-190
title: Fix remaining nondeterministic cloud factory model-list tests
status: Done
assignee:
  - Codex
created_date: '2026-03-07 15:10'
updated_date: '2026-03-07 15:14'
labels:
  - tests
  - chatmodel
dependencies: []
references:
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/openai/OpenAiChatModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/kimi/KimiChatModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/groq/GroqChatModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/mistral/MistralChatModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/anthropic/AnthropicChatModelFactoryTest.java
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The remaining OpenAI, Kimi, Groq, and Mistral chatmodel factory tests still rely on the real LLMModelRegistryService for getModels() assertions, which makes them depend on environment-specific cached model config. The fix should make these tests deterministic by mocking the registry data in test setup.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 OpenAiChatModelFactoryTest passes consistently without depending on external model cache state.
- [x] #2 KimiChatModelFactoryTest passes consistently without depending on external model cache state.
- [x] #3 GroqChatModelFactoryTest passes consistently without depending on external model cache state.
- [x] #4 MistralChatModelFactoryTest passes consistently without depending on external model cache state.
- [x] #5 Running `./gradlew -q test --stacktrace` no longer fails on these four cloud factory tests.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Update OpenAI, Kimi, Groq, and Mistral factory tests to replace LLMModelRegistryService with deterministic mocks in setUp.
2. Supply provider-specific LanguageModel fixtures sized to satisfy each test's getModels() assertion while preserving existing createChatModel coverage.
3. Run the four targeted tests to verify the deterministic setups.
4. Rerun the full Gradle test suite to confirm these four failures are gone and identify whether any unrelated failures remain.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
User approved proceeding with a dedicated follow-up task for the remaining four failing cloud factory tests.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Made the remaining OpenAI, Kimi, Groq, and Mistral factory tests deterministic by replacing `LLMModelRegistryService` with test-local mocks in each test setup. Each test now provides provider-specific `LanguageModel` fixtures sized to satisfy its `getModels()` assertion, so results no longer depend on cached remote model config in the local IntelliJ sandbox.

Validation:
- `./gradlew -q test --tests com.devoxx.genie.chatmodel.cloud.openai.OpenAiChatModelFactoryTest --tests com.devoxx.genie.chatmodel.cloud.kimi.KimiChatModelFactoryTest --tests com.devoxx.genie.chatmodel.cloud.groq.GroqChatModelFactoryTest --tests com.devoxx.genie.chatmodel.cloud.mistral.MistralChatModelFactoryTest --stacktrace`
- `./gradlew -q test --stacktrace`
<!-- SECTION:FINAL_SUMMARY:END -->
