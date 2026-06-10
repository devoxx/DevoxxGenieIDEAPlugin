---
id: TASK-232
title: 'Smooth streaming: batched token updates, in-flight affordance, scroll-to-bottom button'
status: To Do
assignee: []
created_date: '2026-06-10 12:00'
updated_date: '2026-06-10 12:00'
labels:
  - enhancement
  - UX
  - streaming
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ChatScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ThinkingIndicator.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/MessageUiModel.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
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
- [ ] #1 During streaming, UI updates are flushed at a fixed cadence (50-100ms) instead of once per token; a stream of N tokens results in far fewer than N `invokeLater` posts
- [ ] #2 No token loss: the final rendered text after `onCompleteResponse()` equals the full accumulated response, including when the user stops mid-stream (partial text preserved as before)
- [ ] #3 While a response is streaming, the AI bubble shows a visible in-flight affordance (blinking caret or pulse) after the text tail; it disappears on completion, error, and stop
- [ ] #4 When the user scrolls up during streaming, a floating scroll-to-bottom button appears; clicking it scrolls to the latest message with animation and resumes auto-follow; the button is hidden while already at the bottom
- [ ] #5 Existing behavior preserved: auto-scroll on new message, no auto-scroll while user reads scrolled-up content
- [ ] #6 Unit test for the batching logic in `StreamingResponseHandler` (e.g., feed 100 partials rapidly, assert accumulated text intact and UI-update callback invoked a bounded number of times; assert final flush on complete and on stop)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Keep the batching inside `StreamingResponseHandler` (single writer thread from langchain4j) rather than in the ViewModel, so both Compose and any future renderers benefit. A `volatile boolean flushScheduled` + `java.util.Timer`/`EdtScheduledExecutorService` one-shot is enough; avoid a permanently running timer when idle.
- `onIntermediateResponse()` (turn-boundary separator, lines 100-109) must keep working — it only touches the accumulator, so batching is unaffected, but add a test covering a flush straddling a turn boundary.
- The caret belongs in `AiBubble.kt` after the `Markdown` composable, not inside the markdown string — never append fake characters to the model text (it would end up in chat memory/persistence).
- `isStreaming` should be derived/cleared in the same places that currently call `hideLoadingIndicator` (`ConversationViewModel`), keeping one lifecycle for both indicators.
- EDT constraint: flush callback must do its work via `invokeLater` exactly as today; the timer itself must not touch UI off the EDT.
- Out of scope: markdown incremental parsing/caching (separate optimization), token-by-token typewriter effects.
<!-- SECTION:NOTES:END -->
