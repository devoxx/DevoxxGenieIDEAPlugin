---
id: TASK-188
title: Fix nondeterministic cloud factory model-list tests
status: Done
assignee:
  - Codex
created_date: '2026-03-07 15:03'
updated_date: '2026-03-07 15:04'
labels:
  - tests
  - chatmodel
dependencies: []
references:
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/google/GeminiChatModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/glm/GLMChatModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/deepinfra/DeepInfraChatModelFactoryTest.java
  - >-
    /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/test/java/com/devoxx/genie/chatmodel/cloud/anthropic/AnthropicChatModelFactoryTest.java
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Several cloud chatmodel factory tests rely on the real LLMModelRegistryService for getModels() assertions, which makes the tests depend on environment-specific cached model config and causes failures for Gemini, GLM, Bedrock, and DeepInfra. The fix should make these tests deterministic by controlling the registry data in test setup.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 GeminiChatModelFactoryTest passes consistently without depending on external model cache state.
- [x] #2 GLMChatModelFactoryTest passes consistently without depending on external model cache state.
- [x] #3 BedrockModelFactoryTest passes consistently without depending on external model cache state.
- [x] #4 DeepInfraChatModelFactoryTest passes consistently without depending on external model cache state.
- [x] #5 Running `./gradlew -q test --tests 'com.devoxx.genie.chatmodel.cloud.*' --stacktrace` completes with those four model-list failures resolved.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Update the four failing tests to replace LLMModelRegistryService with a deterministic mock in setUp, following the established pattern in AnthropicChatModelFactoryTest.
2. Provide each test with provider-specific LanguageModel fixtures that satisfy its getModels() size assertions while keeping createChatModel coverage unchanged.
3. Run the four targeted tests first to verify the deterministic setups.
4. Run the broader cloud chatmodel test slice to confirm the previous four failures are resolved and no new regressions are introduced.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
User requested a follow-up task rather than expanding TASK-187 scope.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Made the four failing cloud factory tests deterministic by replacing `LLMModelRegistryService` with test-local mocks in Gemini, GLM, Bedrock, and DeepInfra test setup. Each test now supplies provider-specific `LanguageModel` fixtures sized to satisfy its `getModels()` assertions, so results no longer depend on cached remote model config in the local IntelliJ sandbox.

Validation:
- `./gradlew -q test --tests com.devoxx.genie.chatmodel.cloud.google.GeminiChatModelFactoryTest --tests com.devoxx.genie.chatmodel.cloud.glm.GLMChatModelFactoryTest --tests com.devoxx.genie.chatmodel.cloud.bedrock.BedrockModelFactoryTest --tests com.devoxx.genie.chatmodel.cloud.deepinfra.DeepInfraChatModelFactoryTest --stacktrace`
- `./gradlew -q test --tests 'com.devoxx.genie.chatmodel.cloud.*' --stacktrace`
<!-- SECTION:FINAL_SUMMARY:END -->
