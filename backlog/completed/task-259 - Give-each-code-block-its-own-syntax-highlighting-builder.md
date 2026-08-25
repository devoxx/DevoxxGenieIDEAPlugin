---
id: TASK-259
title: Give each code block its own syntax-highlighting builder
status: Done
assignee: []
created_date: '2026-08-25 16:43'
updated_date: '2026-08-25 17:00'
labels:
  - bug
  - ui
  - compose
dependencies: []
references:
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/UserBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/components/SafeHighlightedCode.kt
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Code blocks in a single message can be syntax-highlighted against the wrong block's text, producing mis-coloured output.

`Highlights.Builder` (`dev.snipme:highlights`) is a data class whose fields are `var`; `code(...)` mutates the instance in place and returns it, and `build()` snapshots whatever the fields hold at that moment. `UserBubble` creates one builder per message and hands the same instance to every code fence and code block it renders. Each block then highlights inside its own `produceState` coroutine on `Dispatchers.Default`, so two blocks highlighting concurrently interleave `code(...)` and `build()` on the shared instance — one block can build highlights computed against the other block's text.

The offsets that result are simply wrong for the text they are applied to. Since TASK-258 those offsets are clamped and dropped rather than fatal, so the visible effect is mis-colouring rather than a crash, which is why this was split out instead of being fixed on that branch.

`AiBubble` does not share an instance — it constructs a fresh builder per code block — but it does so outside `remember`, so a new builder is allocated on every recomposition of every block. Worth addressing in the same pass.

Follow-up to TASK-258 (completed; see `backlog/completed/`), which added `SafeHighlightedCode.kt` and the range guards this behaviour now relies on.

**User value:** syntax colouring in a message with several code blocks is correct regardless of the order in which they finish highlighting.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 No syntax-highlighting builder instance is shared between two code blocks that can highlight concurrently
- [x] #2 Highlights applied to a code block are always computed from that block's own text, whatever order concurrent highlighting completes in
- [x] #3 Builders are not reallocated on every recomposition of an unchanged code block
- [x] #4 A test covers concurrent highlighting of two code blocks with different text in one message and asserts each block's highlights match its own text
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Replaced the `Highlights.Builder` parameter on `SafeMarkdownHighlightedCodeFence` / `SafeMarkdownHighlightedCodeBlock` with an immutable `SyntaxTheme`, and moved builder construction into a new pure function `computeHighlights(code, language, theme)` in `SafeHighlightedCode.kt`. The builder is created per call there and never escapes, so no instance can be shared between blocks — the isolation is structural rather than a matter of discipline.

This also removes builder allocation from composition entirely: nothing is constructed at composition time, and a builder is only created when `produceState`'s `code` key changes, which is stricter than wrapping the old builder in `remember`.

`AiBubble.createHighlightsBuilder(isDark)` became `syntaxThemeFor(isDark)`; `UserBubble`'s per-message shared builder became `remember(colors.isDark) { SyntaxThemes.default(...) }`. `SyntaxTheme` is a data class of `val Int` colours, so sharing one instance across blocks is safe.

Tests (`CodeHighlightingIsolationTest`, 4 tests, written red first):
- A deterministic characterization with no threads — `shared.code(A)`, `shared.code(B)`, `shared.build()` returns B's highlights — pinning the exact interleaving concurrent blocks hit, without timing dependence.
- A concurrency test running 200 iterations of two differently-texted blocks across an 8-thread pool, asserting each result equals the single-threaded highlights for its own text.

The concurrency test was mutation-tested rather than trusted: temporarily reintroducing a module-level shared builder made it fail, and reverting made it pass, so it will catch a reintroduction of the defect.

Verified with `./gradlew cleanTest test`: 3650 tests, 0 failures.

Note for whoever runs the suite next: an earlier full run showed 7 unrelated failures in `LocalChatModelFactoryTest` / `OpenRouterChatModelFactoryTest`, all `FileSystemException` from IntelliJ test fixtures nesting temp directories under `/private/tmp/test` past the filesystem path limit. Those classes pass in isolation and the full suite went green after clearing that directory; it accumulates across runs and is unrelated to this change.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Code blocks in one message no longer risk being syntax-highlighted against each other's text.

`Highlights.Builder` is a data class with `var` fields — `code(...)` mutates it in place and `build()` snapshots whatever the fields hold at that instant. `UserBubble` handed one builder to every code block in a message, and each block highlights in its own `produceState` coroutine on `Dispatchers.Default`, so two blocks could interleave and one could be styled with offsets computed from the other's text.

The mutable builder no longer crosses a block boundary: the composables now take an immutable `SyntaxTheme`, and `computeHighlights(code, language, theme)` constructs a builder per call that never escapes. Nothing is allocated at composition time, so recomposition of an unchanged block allocates nothing either.

Verified with `./gradlew cleanTest test`: 3650 tests, 0 failures. The new concurrency test was mutation-tested — reintroducing a shared builder makes it fail — so it will catch a regression.
<!-- SECTION:FINAL_SUMMARY:END -->
