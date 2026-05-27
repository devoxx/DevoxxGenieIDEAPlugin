---
id: TASK-222
title: Remove per-session RAG toggle; rely on ragEnabled as single source of truth
status: Done
assignee: []
created_date: '2026-05-27 11:44'
updated_date: '2026-05-27 12:34'
labels:
  - rag
  - ui
  - refactor
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/panel/SearchOptionsPanel.java
  - src/main/java/com/devoxx/genie/util/ChatMessageContextUtil.java
  - src/main/java/com/devoxx/genie/ui/component/input/PromptInputArea.java
  - src/main/java/com/devoxx/genie/service/MessageCreationService.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
  - src/main/java/com/devoxx/genie/service/prompt/command/FindCommand.java
  - src/main/java/com/devoxx/genie/ui/window/AgentMcpLogToolWindowFactory.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

`SearchOptionsPanel` (chat input area) shows a "RAG" switch that flips `ragActivated`. After task-221 (semantic_search agent tool) and the existing RAG settings master switch (`ragEnabled`), this per-session toggle is redundant:

- Users already have a master "Enable feature" checkbox in RAG settings.
- In agent mode, they can disable the `semantic_search` tool individually under Agent Mode → Built-in Tools.
- Passive injection (non-agent mode) is gated on RAG settings — no need for a second gate.

Two flags doing the same job creates UX confusion: people enable RAG in settings, don't see results, because the chat-area switch is still off.

## Approach

Collapse `ragActivated` into `ragEnabled` as the single source of truth. The chat-area switch is removed. Everywhere the code reads `getRagActivated()`, it reads `getRagEnabled()` instead. The `ragActivated` field on `ChatMessageContext` is populated from `getRagEnabled()` so per-message analytics still work without conditional changes downstream.

## Why this matters

- Removes the most common "I enabled RAG but nothing happens" UX trap.
- Matches the model already used for agent tools (one switch in settings, optional per-tool override).
- Simplifies the state machine — one flag, one place to change.

## Out of scope

- Web search switch (`webSearchActivated`) is left alone — same pattern, but a separate UX decision.
- Removing the deprecated `ragActivated` field from `DevoxxGenieStateService` (keep for backwards-compat with existing user state files; treat as vestigial).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The RAG switch is removed from `SearchOptionsPanel` (the chat input area). The Web search switch is unchanged.
- [x] #2 Anywhere code previously called `getRagActivated()`, it now calls `getRagEnabled()` (or reads the per-message `ChatMessageContext.isRagActivated()` snapshot which is itself sourced from `getRagEnabled()`).
- [x] #3 `ChatMessageContextUtil` populates `ChatMessageContext.ragActivated` from `getRagEnabled()` so analytics + downstream consumers keep working with no signature changes.
- [x] #4 When a user enables RAG in settings (and has an index), the `semantic_search` agent tool is registered, the `<RAG_INSTRUCTION>` system-prompt nudge is added, and (when agent mode is off) passive `<SemanticContext>` injection runs — with no additional toggle required.
- [x] #5 `PromptInputArea` placeholder text reflects `ragEnabled` instead of `ragActivated`; the existing `RAG_ACTIVATED_CHANGED_TOPIC` continues to fire from RAG settings changes so the placeholder still updates live.
- [x] #6 The `/find` command checks `getRagEnabled()` instead of `getRagActivated()` and errors out appropriately when RAG is not enabled.
- [x] #7 All existing unit tests pass after the refactor; tests that mocked `getRagActivated()` are updated to mock `getRagEnabled()` where appropriate.
- [x] #8 Manual verification: with RAG enabled in settings, agent mode on, no chat-area toggle visible, the agent calls `semantic_search` for a conceptual query.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Implementation

### UI
- `SearchOptionsPanel.java`: removed the RAG `InputSwitch`, its listener, the
  `RAG_ACTIVATED_CHANGED_TOPIC` publish call, and the mutual-exclusion `deactivateOther`
  logic (no longer meaningful with one switch). The Web switch and panel-visibility
  machinery are unchanged.
- `PromptInputArea.java`: placeholder-text reads switch to `getRagEnabled()`. The existing
  `RAG_ACTIVATED_CHANGED_TOPIC` is still published by `RAGSettingsConfigurable` whenever
  the master flag flips, so the placeholder still updates live.

### Behavior gates (collapse `ragActivated` → `ragEnabled`)
- `MessageCreationService.shouldInjectPassiveRagContext` — checks `ragEnabled` instead of
  `ragActivated`.
- `BuiltInToolProvider` — semantic_search registration simplified from `ragEnabled &&
  ragActivated` to just `ragEnabled`.
- `ChatMemoryManager.buildAugmentedSystemPrompt` — `<RAG_INSTRUCTION>` block now gated on
  `agentModeEnabled && ragEnabled`.
- `FindCommand` — removed the redundant `ragActivated` branch; only the `ragEnabled`
  check remains. Updated `FindCommandTest` to drop the now-impossible "enabled but not
  activated" scenario and the obsolete `getRagActivated()` stub.
- `AgentMcpLogToolWindowFactory` — `getRagEnabled() OR getRagActivated()` simplified to
  just `getRagEnabled()`.
- `ChatMessageContextUtil` — per-message `ragActivated` snapshot now sourced from
  `getRagEnabled()` so analytics + downstream consumers keep working without signature
  changes.

### Field & state
- `DevoxxGenieStateService.ragActivated` field is kept (vestigial, no harm) so existing
  user state files don't trip deserialization. No code paths read it any more.

### Tests
- `SearchOptionsPanelTest.java`: rewritten for the single-switch panel — removed RAG-only
  tests, kept Web-switch visibility / size tests, dropped mutual-exclusion test.
- `MessageCreationServiceTest.java`: all `getRagActivated()` mocks switched to
  `getRagEnabled()`. The (ragActivated × agentModeEnabled) matrix test stays — same
  logic, new property name.
- `FindCommandTest.java`: collapsed the two-flag scenarios into one positive case.

### Docs
- `docusaurus/docs/features/rag.md`: rewrote the "Searching with RAG" section to describe
  three modes (passive injection / `semantic_search` tool / `/find` command) and added a
  "Why two modes?" explanation covering the agent + RAG interaction and the new
  `<RAG_INSTRUCTION>` system-prompt nudge. Added two new agent-mode example queries.
- `RAGSettingsComponent.java`: added a second help-text paragraph under the "Enable
  feature" checkbox explaining what RAG does at chat time in each mode (agent off →
  passive injection, agent on → `semantic_search` tool, can be individually disabled
  under Agent Mode → Built-in Tools).

### Verification
- Full `./gradlew test` green (JDK 21).

### Follow-up: /find ran the LLM after semantic search (streaming bug)

Real-world report: `/find foo` showed the files popup correctly but ALSO fired an empty LLM call. Diagnosis: only `NonStreamingPromptStrategy.executeStrategySpecific()` had a FIND_COMMAND early-return; `StreamingPromptStrategy` and `WebSearchPromptStrategy` ran their normal LLM pipeline.

Fix: lifted the FIND_COMMAND short-circuit into `AbstractPromptExecutionStrategy.execute()`, before `executeStrategySpecific` is invoked. Moved `executeSemanticSearch` from `NonStreamingPromptStrategy` to the abstract parent as a `protected` method. All three strategies (streaming, non-streaming, web-search) now skip the LLM call for `/find` queries — semantic search runs, files popup shows, and the prompt task completes cleanly with no model invocation.

Files touched: `AbstractPromptExecutionStrategy.java`, `NonStreamingPromptStrategy.java` (removed dead branch + method + unused imports). Full `./gradlew test` green.

### Follow-up #2: empty AI bubble left under /find prompt

Real-world report (after the LLM-trigger fix): `/find` opens the Find Results dialog correctly, but the placeholder AI bubble created by `addUserPromptMessage()` stays in the chat with no content and no loading border. Cause: `PromptOutputPanel.addChatResponse()` only opened the dialog for FIND and never called `conversationPanel.updateUserPromptWithResponse(...)`, so the pending AI bubble was never filled.

Fix:
- `AbstractPromptExecutionStrategy.executeSemanticSearch()`: set a one-line AI summary on the context before calling `panel.addChatResponse(...)` — e.g. `Found 3 relevant files for `<query>` — see the Find Results dialog.`
- `PromptOutputPanel.addChatResponse()`: in the FIND branch, also call `updateUserPromptWithResponse()` so the pending AI bubble gets filled with the summary alongside the dialog popup.

Result: the chat history now shows the /find query and a coherent one-line response; the dialog still pops up for the full hit list. No empty borderless bubble. Full `./gradlew test` green.
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## What shipped

Collapsed the dual RAG control surface (master `ragEnabled` + per-session `ragActivated`) into a single `ragEnabled` switch, removed the redundant chat-area toggle, and fixed two `/find` UX bugs that surfaced during real-world testing.

## Files

**UI**
- `ui/panel/SearchOptionsPanel.java` — removed the RAG `InputSwitch`, its listener, the topic publish, and the mutual-exclusion logic. Web switch unchanged.
- `ui/component/input/PromptInputArea.java` — placeholder reads `ragEnabled`. `RAG_ACTIVATED_CHANGED_TOPIC` continues to fire from RAG settings, so live updates still work.
- `ui/settings/rag/RAGSettingsComponent.java` — added help-text paragraph under "Enable feature" describing both modes (agent off → passive injection, agent on → `semantic_search` tool).

**Behavior gates**
- `service/prompt/command/FindCommand.java` — dropped the redundant `ragActivated` branch.
- `ui/window/AgentMcpLogToolWindowFactory.java` — simplified `ragEnabled OR ragActivated` to just `ragEnabled`.
- `util/ChatMessageContextUtil.java` — per-message `ragActivated` snapshot is now sourced from `getRagEnabled()`.

**/find bug fixes (uncovered during testing)**
- `service/prompt/strategy/AbstractPromptExecutionStrategy.java` — lifted the `FIND_COMMAND` short-circuit and `executeSemanticSearch` into the abstract parent so all strategies (streaming, non-streaming, web-search) skip the LLM for `/find`. Also sets a one-line AI summary on the context so the chat bubble shows useful content.
- `service/prompt/strategy/NonStreamingPromptStrategy.java` — removed the now-duplicated FIND branch + private `executeSemanticSearch`.
- `ui/panel/PromptOutputPanel.java` — `addChatResponse` FIND branch now also calls `updateUserPromptWithResponse` so the pending AI bubble is filled (was leaving an empty borderless bubble).

**Tests**
- `test/.../SearchOptionsPanelTest.java` — rewritten for the single-switch panel.
- `test/.../FindCommandTest.java` — dropped the now-impossible "enabled but not activated" scenario and the obsolete `getRagActivated()` stub.
- `test/.../MessageCreationServiceTest.java` — all `getRagActivated()` mocks switched to `getRagEnabled()`.

**Docs**
- `docusaurus/docs/features/rag.md` — rewrote "Searching with RAG" as a three-mode list (passive / `semantic_search` tool / `/find`), added "Why two modes?" explainer.

## Verification

Full `./gradlew test` green (JDK 21).
<!-- SECTION:FINAL_SUMMARY:END -->

## PR

Shipped in https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1062 (bundled with task-221).
