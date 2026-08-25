---
id: TASK-258
title: >-
  Fix crash rendering code blocks whose text contains an unmatched comment
  terminator
status: Done
assignee: []
created_date: '2026-08-25 16:19'
updated_date: '2026-08-25 16:39'
labels:
  - bug
  - ui
  - compose
dependencies: []
references:
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/UserBubble.kt
  - build.gradle.kts
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The Compose conversation panel dies with `IllegalArgumentException: Reversed range is not supported` whenever an assistant or user message contains a fenced code block whose text has a `*/` occurring before the first `/*`. This happens routinely when an LLM streams a snippet that starts mid-Javadoc or shows a diff hunk containing the tail of a comment block.

Root cause: the syntax highlighter (`dev.snipme:highlights:1.0.0`, `MultilineCommentLocator.locate`) pairs the Nth `/*` index with the Nth `*/` index positionally, without checking that the end follows the start. For input such as `*/ /* */` it emits a highlight location with `end < start`. The markdown renderer (`com.mikepenz:multiplatform-markdown-renderer-code:0.38.1`, `MarkdownHighlightedCode.kt:166`) passes that location straight to `AnnotatedString.Builder.addStyle`, which rejects a reversed range. The exception is thrown from a `produceState` coroutine on `Dispatchers.Default` and goes unhandled, tearing down the Compose frame clock — so the entire chat panel stops rendering, not just the offending code block.

Both `AiBubble.kt` and `UserBubble.kt` render code through the affected library composables, so both are impacted.

Upgrading the renderer or highlighter is out of scope: the build pins these versions to the IntelliJ 253 Compose/Skiko toolchain for documented ABI and classloader reasons.

**User value:** a malformed or partial code snippet in a reply degrades to unstyled code instead of killing the conversation view.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A code block whose text contains `*/` before the first `/*` renders without throwing, in both the AI bubble and the user bubble
- [x] #2 Highlight ranges that fall outside the code text, or whose end does not follow their start, are discarded rather than applied
- [x] #3 An unexpected failure anywhere in the syntax-highlighting pass degrades that code block to unstyled text and leaves the rest of the conversation panel rendering
- [x] #4 A unit test pins the upstream highlighter behaviour that triggers the crash, so a future dependency bump that fixes it is detected
- [x] #5 A unit test covers the reversed, out-of-bounds, and well-formed highlight cases against the code path used by the bubbles
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added `SafeHighlightedCode.kt` in `com.devoxx.genie.ui.compose.components`: a local reimplementation of the renderer's `MarkdownHighlightedCodeFence` / `MarkdownHighlightedCodeBlock` built on the library's public `MarkdownCodeFence`, `MarkdownCodeBlock`, `MarkdownCodeBackground` and `MarkdownBasicText`, so no library internals are copied beyond the composition shape.

Two guards:
- `buildSafeHighlightedAnnotatedString(code, highlights)` clamps each location into `[0, code.length]` and drops anything left with `end <= start`, which covers the reversed, empty and fully out-of-range cases.
- An overload taking a highlights provider wraps the whole highlighting pass, falling back to unstyled `AnnotatedString(code)` on any `Throwable`. `CancellationException` is rethrown so `produceState` disposal still works.

`AiBubble.kt` and `UserBubble.kt` now call the safe composables; no unguarded use of the library composables remains in `src/main`.

Verified with `./gradlew test`: 8 new tests in `SafeHighlightedCodeTest` pass, including a characterization test pinning that upstream emits `end < start`, and one asserting the unguarded code path still throws `IllegalArgumentException: Reversed range is not supported` for the same input. Full suite green.

Branch: `fix/task-258-reversed-highlight-range`, cut from `master` (this repo has no `develop` branch).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Code blocks whose text contains a multiline-comment terminator before the first opener no longer crash the conversation panel.

`dev.snipme:highlights` pairs the Nth `/*` index with the Nth `*/` index positionally, so such a snippet — an LLM streaming a Javadoc excerpt or a diff hunk — yields a highlight location whose end precedes its start. The renderer passed it straight to `AnnotatedString.Builder.addStyle`, which threw `IllegalArgumentException: Reversed range is not supported` from a `produceState` coroutine on `Dispatchers.Default` where nothing caught it, tearing down the Compose frame clock and stopping the entire panel from rendering.

New `SafeHighlightedCode.kt` reimplements `MarkdownHighlightedCodeFence` / `MarkdownHighlightedCodeBlock` on top of the renderer's public `MarkdownCodeFence`, `MarkdownCodeBlock`, `MarkdownCodeBackground` and `MarkdownBasicText`, adding two guards: highlight locations are clamped into `[0, code.length]` and dropped when `end <= start`, and the whole highlighting pass falls back to unstyled text on any `Throwable` (rethrowing `CancellationException` so `produceState` disposal still works). `AiBubble` and `UserBubble` now use the safe composables; no unguarded use of the library composables remains.

No dependency upgrade: the build pins the renderer and highlighter to the IntelliJ 253 Compose/Skiko toolchain for documented ABI and classloader reasons.

Verified with `./gradlew test` on the rebased branch: 3646 tests, 0 failures. `SafeHighlightedCodeTest` adds 8 tests, including a characterization test pinning the upstream defect (it fails if a future bump fixes it) and one asserting the unguarded code path still throws the reported exception for the same input.

Known follow-up, out of scope here: `UserBubble` shares one mutable `Highlights.Builder` across every code block in a message, and `.code()` mutates it, so concurrent highlighting can compute highlights against the wrong block's text. The new guard downgrades that from a crash to at-worst mis-coloring.
PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1277
<!-- SECTION:FINAL_SUMMARY:END -->
