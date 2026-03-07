---
id: TASK-195
title: 'Fix: Links in chat output must be clickable and open in browser'
status: To Do
assignee: []
created_date: '2026-03-07 15:37'
labels:
  - bug
  - ui
  - webview
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/webview/ConversationWebViewController.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Currently, URLs/links displayed in the chat output panel are not clickable. When the LLM response contains URLs (e.g., documentation links, GitHub issues, etc.), users should be able to click on them to open the link in their default browser.

This applies to the JCEF WebView-based chat output (`ConversationWebViewController`). Links rendered in the HTML/markdown output should have proper `<a>` tags with `target="_blank"` or use JCEF's navigation handler to intercept link clicks and open them via `BrowserUtil.browse()` instead of navigating within the embedded browser.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 URLs in LLM chat responses are rendered as clickable hyperlinks
- [ ] #2 Clicking a link opens it in the user's default system browser
- [ ] #3 Links do not navigate inside the embedded JCEF WebView panel
- [ ] #4 Markdown-formatted links [text](url) render correctly as clickable links
- [ ] #5 Plain-text URLs (e.g. https://example.com) are also detected and made clickable
<!-- AC:END -->
