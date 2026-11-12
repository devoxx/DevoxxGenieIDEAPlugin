---
id: TASK-275
title: Show the full error in the chat's error card and let users copy it
status: Done
assignee:
  - '@Claude'
created_date: '2026-11-12T13:04:00Z'
updated_date: '2026-11-12T13:19:00Z'
labels:
  - ui
  - errors
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/prompt/error/PromptErrorHandler.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
modified_files:
  - src/test/kotlin/com/devoxx/genie/ui/compose/components/ErrorCardTextTest.kt
  - >-
    src/test/java/com/devoxx/genie/service/prompt/error/PromptErrorHandlerMessageTest.java
  - src/main/java/com/devoxx/genie/service/prompt/error/PromptErrorHandler.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandlerTest.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ErrorCardText.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
priority: medium
type: enhancement
ordinal: 22000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a prompt fails, the chat shows a red "Request failed" card, but long error messages end in "…" and the rest is lost. PromptErrorHandler.userFacingMessage() cuts any message over 300 characters before it reaches the chat, so the full text never gets to the UI and can't be read or copied there.

This made provider errors hard to diagnose: a Jan error explaining which GGUF file it could not open was cut off mid-path. Users then have to dig through idea.log, or retype the message, to report or search for it. The card should keep its short summary for the common case, make the full message reachable, and offer a one-click copy, using the existing CopyButton.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A failed prompt's error card shows the short message by default, and the user can expand it to read the full, untruncated error text
- [ ] #2 A Copy button on the error card copies the full error text to the clipboard, not the shortened version
- [x] #3 Errors short enough not to be truncated show no expand control, and their Copy button still works
- [x] #4 Translated provider errors (ProviderErrorTranslator) keep their current wording in the card
- [x] #5 The Retry affordance still works as before, including its one-retry limit
- [x] #6 Tests cover keeping the full text alongside the short one, and the card's expand and copy behaviour
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. PromptErrorHandler.userFacingMessage() returns the full message instead of cutting it at 300 characters; translated provider errors stay as they are. The full text flows through the existing errorText path, so MessageUiModel, the view model and the controller interface are unchanged. errorText is not persisted, so no storage change.
2. New Kotlin helper ErrorCardText: the collapsed text (first 300 characters plus an ellipsis) and whether a message is long enough to expand.
3. ErrorCard (AiBubble.kt): collapsed by default; a Show full error / Show less link only when shortened; selectable text; a CopyButton next to Retry that copies the full text. Retry keeps its one-retry limit.
4. Tests: new PromptErrorHandlerTest (long message returned whole, translated error unchanged) and ErrorCardTextTest (short vs long, collapsed text, copy text is the full message). There's no Compose UI test setup, so clicking expand and Copy is a manual IDE check.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on feature/task-275-full-error-card-copy (uncommitted).

- Plan change: userFacingMessage() also feeds the balloon notification (showNotification), so instead of dropping its 300-character cut, PromptErrorHandler gained fullUserFacingMessage(exception, modelName): the translated provider error, or the classified message, or the exception type, never shortened. userFacingMessage() now shortens that for notifications, and translated errors still skip the cut in both. The two error-card call sites (PromptErrorHandler.updateChatTerminalState and StreamingResponseHandler.onError) use fullUserFacingMessage, so the full text reaches the card through the unchanged errorText path. errorText isn't persisted.
- New ErrorCardText (Kotlin): COLLAPSED_LENGTH = 300, isExpandable, displayed(full, expanded) and copyText (always the full text).
- ErrorCard (AiBubble.kt): shows ErrorCardText.displayed(...) inside a SelectionContainer, so the text is selectable. A "Show full error" / "Show less" link appears only when the error is long enough to be shortened, and it collapses again when a different error lands in the card. A CopyButton next to Retry copies the full text. Retry is unchanged, including its one-retry limit.
- Tests: new PromptErrorHandlerMessageTest (full text kept, notification still shortened, translated wording kept in both, empty message falls back) and ErrorCardTextTest (short vs at-limit vs long, collapsed text, copy is full). StreamingResponseHandlerTest now stubs fullUserFacingMessage. The existing PromptErrorHandlerTest was overwritten by mistake during the work, restored from git, and passes unchanged.
- Full suite passes.
- Manual IDE check still needed for AC #1 and #2 (no Compose UI test setup in this project): trigger a long error, for example Jan's broken gemma4-e2b-it.gguf model; expand and collapse; copy and paste the full text.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The chat's error card now gives access to the whole error. Long errors are still shortened at 300 characters by default. A "Show full error" / "Show less" link expands them, the text is selectable, and a Copy button next to Retry copies the full error.

PromptErrorHandler gained fullUserFacingMessage(exception, modelName): the translated provider error, or the classified message, or the exception type, never shortened. The two error-card call sites (updateChatTerminalState and StreamingResponseHandler.onError) now use it, so the full text reaches the card through the unchanged errorText path. userFacingMessage() still shortens to 300 characters for the balloon notification, and translated provider errors keep their wording in both. A new ErrorCardText helper decides the collapsed text, whether to offer expansion, and the copy text (always the full error). ErrorCard in AiBubble.kt uses it.

Tests: PromptErrorHandlerMessageTest (full text kept, notification still shortened, translated wording kept, empty-message fallback) and ErrorCardTextTest (short, at-limit and long errors; copy is the full text). StreamingResponseHandlerTest stubs the new method. Full suite passes.

Not verified: AC #1 and #2 in the IDE (expanding the card, and what Copy puts on the clipboard); the project has no Compose UI test setup.
<!-- SECTION:FINAL_SUMMARY:END -->
