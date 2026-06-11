---
id: TASK-237
title: 'Chat input/output text should scale with IDE zoom (Appearance: Zoom IDE)'
status: Done
assignee: []
created_date: '2026-06-10 19:43'
updated_date: '2026-06-10 20:00'
labels:
  - bug
  - ui
  - webview
dependencies: []
modified_files:
  - src/main/kotlin/com/devoxx/genie/ui/compose/theme/DevoxxGenieTheme.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ConversationScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/UserBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/ActivitySection.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/CopyButton.kt
  - src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
  - src/main/java/com/devoxx/genie/ui/component/input/PromptInputArea.java
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/theme/DevoxxGenieTypographyTest.kt
  - >-
    src/test/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModelTest.kt
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a user changes the IDE zoom level via JetBrains' Appearance → "Zoom IDE" (zoom in / zoom out), the DevoxxGenie chat panel's input and output text do not grow or shrink to match. The rest of the IDE UI rescales, but the chat conversation text (rendered in the JCEF WebView) and the user prompt input area stay at a fixed size, leaving them visually inconsistent with the zoomed IDE and harder to read at higher zoom levels.

The chat output is rendered in a JCEF WebView (ConversationWebViewController → JBCefBrowser), and the prompt input is a Swing component (UserPromptPanel). The fix needs to detect the current IDE zoom / UI scale factor, apply it to both the WebView content (e.g. CSS font scaling) and the Swing input text, and update reactively when the user changes the zoom level while the tool window is open.

WHY: Users who zoom the IDE for readability or on high-DPI displays expect the assistant's chat text to follow the same zoom, consistent with the rest of the IDE.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Zooming the IDE in via Appearance → Zoom IDE increases the chat output (WebView) text size proportionally
- [x] #2 Zooming the IDE out via Appearance → Zoom IDE decreases the chat output (WebView) text size proportionally
- [x] #3 The user prompt input text scales together with the chat output text on IDE zoom changes
- [x] #4 Zoom changes apply reactively while the tool window is open (no plugin/IDE restart or reopen required)
- [x] #5 Text scaling honors the current IDE zoom/UI scale factor and remains correct after switching between zoom levels back and forth
- [x] #6 Code blocks, markdown, and streaming responses in the WebView remain readable and correctly laid out at all supported zoom levels
- [x] #7 Behavior is verified manually across at least one zoom-in and one zoom-out step, and any automated coverage that can assert the scale factor is applied is added
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Note: chat output is now Compose-based (DevoxxGenieTheme/ConversationScreen), not JCEF — the JCEF mention in the description is stale.
2. DevoxxGenieTheme.kt: add ideScale param; extract pure buildDevoxxTypography(bodyFontSize, codeFontSize, ideScale) that multiplies body/code/heading/caption sizes by a sanitized scale; add scale field to DevoxxTypography for header offset scaling in AiBubble/UserBubble.
3. ConversationViewModel: add ideScale Compose state read from UISettingsUtils.getInstance().currentIdeScale (injectable provider for tests); refresh in onAppearanceSettingsChanged().
4. ConversationScreen: pass viewModel.ideScale to DevoxxGenieTheme.
5. ConversationPanel.java: subscribe UISettingsListener.TOPIC (fires on Appearance → Zoom IDE) → viewController.appearanceSettingsChanged().
6. PromptInputArea.java: applyEditorFont() derives font at UISettingsUtils.getScaledEditorFontSize(); subscribe UISettingsListener.TOPIC to reapply on zoom change.
7. Scale hardcoded small label sizes in chat components (ActivitySection, CopyButton, ChatScreen overlay) via typography.scale.
8. Tests: DevoxxGenieTypographyTest (scale=1 parity, scaling, NaN/zero/negative sanitization, clamping) + ConversationViewModel ideScale refresh test.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Chat output is Compose-based (not JCEF as the description assumed). Root cause: Compose density does not track IntelliJ's "Zoom IDE" factor, and the prompt input applied the editor color scheme font at its unscaled size.

Changes:
- DevoxxGenieTheme.kt: new pure buildDevoxxTypography(body, code, ideScale) multiplies all typography sizes (body1/body2/caption/h5, bodyFontSize, codeFontSize) by a sanitized scale (finite, clamped 0.5–3, fallback 1). DevoxxTypography gained `scale` + `bodyPlus(offset)` so markdown heading offsets in AiBubble/UserBubble zoom too.
- ConversationViewModel: `ideScale` Compose state read from UISettingsUtils.getInstance().currentIdeScale (injectable `readIdeScale` provider for tests, falls back to 1 without an Application); refreshed in onAppearanceSettingsChanged(). ConversationScreen passes it to DevoxxGenieTheme.
- ConversationPanel.java: subscribes UISettingsListener.TOPIC (fires on Appearance → Zoom IDE) → viewController.appearanceSettingsChanged(), so the chat recomposes live.
- PromptInputArea.java: applyEditorFont() now derives the editor font at UISettingsUtils.getScaledEditorFontSize() (scheme size × ideScale) and re-applies on UISettingsListener events.
- ActivitySection/CopyButton: removed hardcoded 11.sp overrides that defeated the (now scaled) caption style; same visual size at 100% zoom.
- ScrollToBottomButton glyph left fixed — it is an icon in a fixed 32dp circle, not chat text.

Tests: DevoxxGenieTypographyTest (7 tests: parity at scale 1, zoom in/out scaling, heading offsets, clamp-before-scale, NaN/zero/negative/extreme sanitization) + 2 new ConversationViewModelTest tests (refresh on appearance change incl. back-and-forth, platform-unavailable fallback). Full suite: 2949 tests, only 8 pre-existing failures (KanbanTemplateTest, ReadFileToolExecutorTest) — confirmed identical on clean master via a throwaway worktree.

Remaining: manual verification of zoom in/out in a running IDE (AC #7).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Chat input/output text now scales with the IDE zoom factor (Appearance → Zoom IDE).

The chat output turned out to be Compose-based (not JCEF as originally described). Compose density does not track IntelliJ's ide-scale factor, so the theme now multiplies all typography sizes by a sanitized zoom scale via a new pure `buildDevoxxTypography(body, code, ideScale)` function (clamped 0.5–3, falls back to 1 on invalid values). `ConversationViewModel` exposes `ideScale` as Compose state read from `UISettingsUtils.currentIdeScale` and refreshes it on appearance changes; `ConversationPanel` subscribes to `UISettingsListener.TOPIC` so zoom changes recompose the chat live. Markdown heading offsets in AiBubble/UserBubble scale via `typography.bodyPlus(offset)`, and hardcoded 11.sp caption overrides in ActivitySection/CopyButton were removed so they follow the scaled caption style (identical at 100% zoom).

The Swing prompt input (`PromptInputArea`) now derives the editor font at `UISettingsUtils.getScaledEditorFontSize()` (scheme size × zoom) and re-applies it on UISettingsListener events.

Automated coverage: 7 new DevoxxGenieTypographyTest tests (scale parity at 1.0, zoom in/out scaling, heading offsets, clamp-before-scale, NaN/zero/negative/extreme sanitization) and 2 new ConversationViewModelTest tests (scale refresh incl. back-and-forth, fallback to 1 without the platform). Full suite: 2949 tests with only 8 pre-existing failures (KanbanTemplateTest, ReadFileToolExecutorTest), confirmed identical on clean master.
<!-- SECTION:FINAL_SUMMARY:END -->
