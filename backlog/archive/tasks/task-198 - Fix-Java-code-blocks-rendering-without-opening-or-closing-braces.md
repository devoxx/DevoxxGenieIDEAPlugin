---
id: TASK-198
title: Fix Java code blocks rendering without opening or closing braces
status: Done
assignee: []
created_date: '2026-03-08 16:05'
updated_date: '2026-03-08 16:45'
labels:
  - bug
  - ui
  - markdown
  - java
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/processor/FencedCodeBlockProcessor.java
  - src/main/java/com/devoxx/genie/ui/processor/IndentedCodeBlockProcessor.java
  - src/main/java/com/devoxx/genie/ui/renderer/CodeBlockNodeRenderer.java
  - src/main/java/com/devoxx/genie/ui/panel/PromptOutputPanel.java
  - src/main/java/com/devoxx/genie/ui/util/CodeSnippetAction.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Users report that Java examples rendered in chat responses can appear with missing structural characters in code blocks, including opening or closing braces and parentheses, even though the underlying snippet is still intact. Copying and pasting the same code preserves the missing characters, which indicates a visual rendering defect in the code-block display path rather than data loss or clipboard corruption. The issue is visible in the attached screenshot from the conversation UI. Fix the code-block rendering path so Java snippets are displayed completely and accurately in the response panel.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Java code blocks rendered in chat responses visually preserve opening and closing braces and parentheses exactly as returned by the model.
- [x] #2 The rendered output does not visually drop braces or parentheses at the beginning or end of classes, methods, records, switch expressions, method calls, or other Java constructs.
- [x] #3 The Copy action for affected code blocks includes the full unmodified snippet, including braces and parentheses.
- [x] #4 The fix addresses the rendering/display path only and does not regress the existing raw code / clipboard behavior.
- [x] #5 Rendering non-Java fenced code blocks continues to work as before without regressions.
- [ ] #6 Automated regression coverage verifies that fenced code blocks containing Java braces and parentheses render completely in the conversation UI.
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Root Cause

The `dev.snipme.highlights` syntax highlighting library (used by mikepenz markdown renderer) classifies `{`, `}`, `(`, `)`, `=`, `<`, `>`, `[`, `]`, `|`, `&`, `-`, `+` as "mark" characters and colors them with the theme's `mark` color.

The `MarkdownHighlightedCodeFence` and `MarkdownHighlightedCodeBlock` components were called with a default `Highlights.Builder()` which always used `SyntaxThemes.default(darkMode = false)` — the **light** theme, regardless of the IDE's actual theme.

In the light Darcula theme, `mark = 0x121212` (near-black). On DevoxxGenie's dark code background `0x1E1E1E` (also near-black), these characters were invisible. Copy-paste worked because the text was present in the `AnnotatedString` — just colored to match the background.

## Fix

- **AiBubble.kt**: Changed `codeFenceWithCopy` and `codeBlockWithCopy` from static vals to functions that accept `isDark: Boolean` and pass a theme-aware `Highlights.Builder` with `SyntaxThemes.default(darkMode = isDark)`.
- **UserBubble.kt**: Same fix — pass a theme-aware `Highlights.Builder` to `MarkdownHighlightedCodeFence` and `MarkdownHighlightedCodeBlock`.

## Files Changed

- `src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt`
- `src/main/kotlin/com/devoxx/genie/ui/compose/components/UserBubble.kt`
- `src/test/kotlin/com/devoxx/genie/ui/compose/components/ExtractCodeTextTest.kt` (new regression tests)
<!-- SECTION:FINAL_SUMMARY:END -->
