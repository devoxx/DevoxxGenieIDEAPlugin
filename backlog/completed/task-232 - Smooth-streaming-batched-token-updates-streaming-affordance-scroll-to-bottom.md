---
id: TASK-232
title: Smooth streaming: batched token updates, in-flight affordance,
  scroll-to-bottom button
status: Done
assignee: []
created_date: '2026-06-10 12:00'
updated_date: '2026-06-10 18:53'
labels:
  - enhancement
  - UX
  - streaming
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ChatScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ThinkingIndicator.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/main/java/com/devoxx/genie/ui/panel/log/AgentMcpLogPanel.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Streaming currently posts one EDT update per token: `StreamingResponseHandler.onPartialResponse()` (lines 61-91) appends the partial to the accumulator and immediately calls `ApplicationManager.invokeLater { conversationViewController.updateAiMessageContent(context) }`. Each update replaces the bubble's full markdown string, so the mikepenz `Markdown` composable in `AiBubble.kt` re-parses the entire response on every token. With fast providers (Groq, local Ollama on small models) this floods the EDT and causes visible jank and high CPU.

Three sub-problems, all in the streaming path:

1. **Batch token updates.** Buffer partials in `StreamingResponseHandler` and flush to the UI on a fixed cadence (~75ms) instead of per token. `AgentMcpLogPanel` already batches incoming log lines in 100ms windows with a `javax.swing.Timer` (see its batching block around lines 460-470) — reuse that pattern. The accumulator already exists; only the `invokeLater` cadence changes. Final flush must happen unconditionally in `onCompleteResponse()` and in `stop()` so no trailing tokens are lost.
2. **In-flight affordance.** Once the first token arrives, `ThinkingIndicator` disappears (`AiBubble.kt` shows it only while `aiResponseMarkdown.isBlank()`) and nothing indicates the response is still streaming. Add a `isStreaming` flag to `MessageUiModel` and render a subtle blinking caret (or small inline pulse, reusing the `infiniteTransition` style from `ThinkingIndicator.kt`) at the end of the bubble while true. Set false on complete/error/stop.
3. **Scroll-to-bottom button.** `ChatScreen.kt` (lines 21-46) auto-follows only while the user is near the bottom — good — but once the user scrolls up mid-stream there is no way back except manual scrolling. Show a small floating "↓" button (bottom-right of the list, `AnimatedVisibility` fade) when `shouldAutoScroll == false` and content is still growing; clicking it calls `listState.animateScrollToItem(messages.lastIndex)` and re-enables following.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 During streaming, UI updates are flushed at a fixed cadence (50-100ms) instead of once per token; a stream of N tokens results in far fewer than N `invokeLater` posts
- [x] #2 No token loss: the final rendered text after `onCompleteResponse()` equals the full accumulated response, including when the user stops mid-stream (partial text preserved as before)
- [x] #3 While a response is streaming, the AI bubble shows a visible in-flight affordance (blinking caret or pulse) after the text tail; it disappears on completion, error, and stop
- [x] #4 When the user scrolls up during streaming, a floating scroll-to-bottom button appears; clicking it scrolls to the latest message with animation and resumes auto-follow; the button is hidden while already at the bottom
- [x] #5 Existing behavior preserved: auto-scroll on new message, no auto-scroll while user reads scrolled-up content
- [x] #6 Unit test for the batching logic in `StreamingResponseHandler` (e.g., feed 100 partials rapidly, assert accumulated text intact and UI-update callback invoked a bounded number of times; assert final flush on complete and on stop)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Keep the batching inside `StreamingResponseHandler` (single writer thread from langchain4j) rather than in the ViewModel, so both Compose and any future renderers benefit. A `volatile boolean flushScheduled` + `java.util.Timer`/`EdtScheduledExecutorService` one-shot is enough; avoid a permanently running timer when idle.
- `onIntermediateResponse()` (turn-boundary separator, lines 100-109) must keep working — it only touches the accumulator, so batching is unaffected, but add a test covering a flush straddling a turn boundary.
- The caret belongs in `AiBubble.kt` after the `Markdown` composable, not inside the markdown string — never append fake characters to the model text (it would end up in chat memory/persistence).
- `isStreaming` should be derived/cleared in the same places that currently call `hideLoadingIndicator` (`ConversationViewModel`), keeping one lifecycle for both indicators.
- EDT constraint: flush callback must do its work via `invokeLater` exactly as today; the timer itself must not touch UI off the EDT.
- Out of scope: markdown incremental parsing/caching (separate optimization), token-by-token typewriter effects.

### Progress (2026-06-10)

- Batching implemented in `StreamingResponseHandler` with an injectable `FlushScheduler` (default: `AppExecutorUtil` shared scheduled executor, 75ms one-shot, `AtomicBoolean` guard against timer stacking). First token paints immediately; unconditional final flush on complete; explicit buffer flush in `stop()`. Accumulator access synchronized.
- `isStreaming` cleared in `ConversationViewModel.hideLoadingIndicator()` (complete/error/stop all funnel there). `StreamingCaret` composable rendered after `Markdown` in `AiBubble`.
- User feedback round 1: full-text flicker during streaming — mikepenz `Markdown` defaults `retainState=false` (blank loading state on every content change); fixed with `retainState=true` in `AiBubble`.
- User feedback round 2: view pinned at top of growing response — `animateScrollToItem(lastIndex)` targets the item TOP. Replaced with a 1px bottom-anchor list item as the tail-follow scroll target; `autoFollow` flag disengaged via `NestedScrollConnection` on upward user scroll (programmatic scrolls bypass it), re-engaged at bottom; scroll-to-bottom button visibility = `!autoFollow`.
- Tests: 8 new tests in `StreamingResponseHandlerTest` (TDD: batching bound for 100 tokens, re-arm after flush, unflushed buffer on complete, stop flush, stale flush no-ops after stop/complete, turn-boundary separator) + ViewModel tests (isStreaming lifecycle, duplicate TOOL_RESPONSE suppression).
- Bonus testability fix: guarded `ThemeDetector` access in `ConversationViewModel` init — un-broke 3 pre-existing `ConversationViewModelTest` failures.
- Same-branch extras requested mid-task: removed `AgentMcpLogPanel` hover popups (`setExpandableItemsEnabled(false)`, dropped tooltip + `toHtmlTooltip`; flickered with big logs; double-click view remains); suppressed duplicate AGENT `TOOL_RESPONSE` rows in chat Activity section.
- Full suite: 8 failures, all pre-existing on master (KanbanTemplateTest ×7, ReadFileToolExecutorTest ×1 — platform `Application` missing in unit-test JVM); baseline before this branch was 11.
- AC #3/#4/#5 implemented, pending visual verification via `runIde`.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented smooth streaming end-to-end:

1. **Batched token updates** — `StreamingResponseHandler` buffers partials and flushes on a 75ms one-shot via an injectable `FlushScheduler` (default: `AppExecutorUtil` shared executor). First token paints immediately; unconditional final flush on complete; explicit buffer flush on stop; accumulator access synchronized.
2. **In-flight affordance** — new `StreamingCaret` composable (blinking block) after the Markdown content while `MessageUiModel.isStreaming`; flag cleared in `ConversationViewModel.hideLoadingIndicator()` (complete/error/stop).
3. **Scroll-to-bottom** — 1px bottom-anchor list item as the tail-follow target (scrolling to the message item pinned the viewport at its top); `autoFollow` disengaged via `NestedScrollConnection` on upward user scroll, re-engaged at bottom; floating button scoped to in-flight streams via testable `shouldShowScrollToBottom()`.
4. **Flicker fix** — `retainState = true` on the mikepenz `Markdown` composable (default false swaps to a blank loading state on every content change).

Extras shipped on this branch: removed `AgentMcpLogPanel` hover popups (expandable-items + tooltip) that flickered with big logs; suppressed duplicate AGENT `TOOL_RESPONSE` rows in the chat Activity section; guarded `ThemeDetector` init in `ConversationViewModel` (fixed 3 pre-existing test failures). 13 new unit tests, all green; remaining 8 suite failures are pre-existing on master.
PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1104
<!-- SECTION:FINAL_SUMMARY:END -->
