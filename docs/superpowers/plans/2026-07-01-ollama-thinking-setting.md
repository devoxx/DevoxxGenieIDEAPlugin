# Ollama Thinking Setting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an opt-in setting that lets users show Ollama model thinking/reasoning in plugin responses.

**Architecture:** Persist a new boolean in `DevoxxGenieStateService`, expose it as a checkbox near the existing stream mode setting, wire it into Ollama LangChain4j builders with `think(true)` and `returnThinking(true)`, and capture streaming `PartialThinking` tokens in the existing streaming response handler. Keep the default disabled so existing behavior is unchanged.

**Tech Stack:** Java 17, IntelliJ settings UI/Swing, LangChain4j 1.17.1 Ollama, JUnit 5, Mockito, AssertJ.

---

### Task 1: Persist and expose the settings checkbox

**Files:**
- Modify: `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java`
- Test: `src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurableTest.java`
- Test: `src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceTest.java`

- [ ] Write failing tests that the new setting defaults to false, is modified when the checkbox changes, applies to state, and resets from state.
- [ ] Run the targeted tests and verify failures mention missing `getOllamaThinkingEnabled` / checkbox accessors.
- [ ] Add `private Boolean ollamaThinkingEnabled = false` plus getter/setter in `DevoxxGenieStateService`.
- [ ] Add `ollamaThinkingCheckBox` to `LLMProvidersComponent` near stream mode with a short hint.
- [ ] Include the checkbox in `isModified()`, `apply()`, and `reset()`.
- [ ] Run the targeted tests and verify they pass.

### Task 2: Wire the setting into Ollama model creation

**Files:**
- Modify: `src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java`
- Test: `src/test/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactoryTest.java`

- [ ] Write failing tests that disabled thinking leaves `think()` null and enabled thinking sets `think()` true for both chat and streaming models.
- [ ] Use reflection in tests to assert the private LangChain4j `returnThinking` flag is false/true as appropriate.
- [ ] Run targeted tests and verify they fail before implementation.
- [ ] Read `DevoxxGenieStateService.getOllamaThinkingEnabled()` in the factory and call `.think(true).returnThinking(true)` only when enabled.
- [ ] Run targeted tests and verify they pass.

### Task 3: Capture streaming thinking tokens in responses

**Files:**
- Modify: `src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java`
- Test: existing streaming handler tests if present, otherwise add focused test coverage in `src/test/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandlerTest.java`.

- [ ] Write a failing test that calls `onPartialThinking(new PartialThinking("reason"))`, then normal response tokens, and verifies the final persisted/displayed AI message contains both a clearly labeled thinking section and the answer.
- [ ] Run targeted test and verify it fails because `onPartialThinking` is not handled.
- [ ] Add a thinking accumulator and override `onPartialThinking(...)` to append tokens without replacing answer tokens.
- [ ] Render the accumulated thinking before the answer as a Markdown details block: `<details><summary>Thinking</summary>\n\n...\n\n</details>\n\nanswer`.
- [ ] Ensure final `AiMessage` uses the combined visible text when streaming occurred, while token usage handling remains unchanged.
- [ ] Run targeted tests and verify they pass.

### Task 4: Verify and update Backlog

**Files:**
- Modify: `backlog/tasks/task-240 - Enable-Ollama-thinking-from-settings.md` via MCP tools only.

- [ ] Run `./gradlew test --tests ...` for changed test classes.
- [ ] Run `./gradlew test` if targeted tests pass and time permits.
- [ ] Mark TASK-240 acceptance criteria complete via Backlog MCP after verification.
