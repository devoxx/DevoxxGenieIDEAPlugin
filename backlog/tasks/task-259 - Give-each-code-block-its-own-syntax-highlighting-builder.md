---
id: TASK-259
title: Give each code block its own syntax-highlighting builder
status: To Do
assignee: []
created_date: '2026-08-25 16:43'
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
- [ ] #1 No syntax-highlighting builder instance is shared between two code blocks that can highlight concurrently
- [ ] #2 Highlights applied to a code block are always computed from that block's own text, whatever order concurrent highlighting completes in
- [ ] #3 Builders are not reallocated on every recomposition of an unchanged code block
- [ ] #4 A test covers concurrent highlighting of two code blocks with different text in one message and asserts each block's highlights match its own text
<!-- AC:END -->
