---
id: TASK-277.7
title: Make the web search test in Settings readable and give clear feedback
status: Done
assignee:
  - '@claude'
created_date: '2026-11-18T10:43:00Z'
updated_date: '2026-11-18T11:16:00Z'
labels: []
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/common/SearchTestPanel.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RagSearchTestAdapter.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchTestSearch.java
modified_files:
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchSnippetFormatterTest.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchSnippetFormatter.java
  - src/main/java/com/devoxx/genie/ui/settings/common/SearchTestOutcome.java
  - src/main/java/com/devoxx/genie/ui/settings/common/SearchTestPanel.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchTestSearch.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersComponent.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchTestSearchAccess.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchProvidersConfigurableTest.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RagSearchTestAdapter.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RagSearchResultsPanel.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/RagSearchResultsPanelTest.java
  - src/test/java/com/devoxx/genie/ui/settings/rag/RagSearchTestAdapterTest.java
  - src/test/java/com/devoxx/genie/ui/settings/common/SearchTestPanelTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/websearch/WebSearchSettingsScreenshotGenerator.java
  - docusaurus/static/img/web-search-settings.png
  - docusaurus/docs/features/web-search.md
  - docusaurus/docs/features/rag.md
parent_task_id: TASK-277
priority: medium
type: enhancement
ordinal: 32000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The "Test web search" area in Settings → Web Search Providers dumps raw provider output into a monospace text area: titles, URLs and whole page snippets run together, with raw Markdown (`**Added**`, `[text](url)`) and no separation between results. The user can't scan what came back, can't tell where one result ends and the next begins, and gets little feedback on whether the search is running, succeeded, or failed. This is the main way users check that a provider and key work, so it should read like search results, not a log dump.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Web search test results are listed one per line (title and source domain), and the selected result's snippet and full URL appear in a preview below
- [x] #2 Snippets are shortened to a few lines and do not show raw Markdown syntax
- [ ] #3 Double-clicking a web search result, or clicking its URL in the preview, opens it in the browser
- [x] #4 While a search runs the user sees a busy state and cannot start a second search
- [x] #5 After a web search the user sees a one-line status: provider used, number of results and elapsed time
- [x] #6 Failures (missing key, HTTP error, no results) show a clear message that is visually distinct from results
- [x] #7 Pressing Enter in the query field runs the test; an empty query cannot be run
- [x] #8 The panel follows the IDE theme (light and dark) and does not freeze the Settings dialog
- [x] #9 The RAG 'Test semantic search' section uses the same shared test-search component, with its existing rows (score, file, folder), chunk preview and messages preserved
- [ ] #10 Double-clicking a RAG match opens that file in the editor
- [x] #11 Tests cover snippet formatting (truncation, Markdown stripping), the shared component's states, and the existing RAG result helpers
- [x] #12 Docs describing the web search and RAG test sections are updated
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. New `ui/settings/common`: `SearchTestOutcome<T>` (kind RESULTS/EMPTY/ERROR + message + items) and `SearchTestPanel<T>` (query field with placeholder + button, status line with AnimatedIcon/OK/info/error icon, single-line JBList + preview with ActionLink title and text area; hidden until first search). The adapter provides the placeholder, busy text, `prepare(query)` (called on the EDT so the form is snapshotted there, and returns the background supplier), row rendering via SimpleColoredComponent (no HTML interpretation), preview text/monospace/rows, and open(item). The panel adds elapsed time to the results summary; error text stays the adapter's responsibility.
2. Web search: `WebSearchSnippetFormatter` (strip Markdown, collapse whitespace, cut at a word boundary of about 280 chars) and `WebSearchTestSearch` (adapter + static `search(config, query)` returning `SearchTestOutcome<Hit>`; same readiness checks and redacted errors as today; maxResults cap; through WebSearchService.search). Open only http(s) URLs via BrowserUtil.
3. RAG: `RagSearchTestAdapter` replaces RagSearchResultsPanel (keeps the score/file/folder rows, monospace chunk preview, summary/noMatches texts and helpers). Double-click/link opens the file via LocalFileSystem + FileEditorManager, with a null-safe message when the file is missing; the editor opens behind the modal Settings dialog (tooltip says so).
4. Tests: formatter tests first; the RagSearchResultsPanelTest helpers move to RagSearchTestAdapterTest; SearchTestPanel state tests; existing performTestSearch tests switch to the test-side WebSearchTestSearchAccess text view (errors start with the message, stub snippets kept verbatim).
5. Docs: web-search.md step 6 and the RAG test-section docs.
6. Visual check: paint both panels offscreen in light and dark, or runIde with the user.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Scope agreed 2026-11-18: extract a shared Settings test-search component (query row, status line, single-line JBList + preview) reused by Web Search and RAG settings; follow the RAG master-detail pattern (single-line rows avoid JBR flicker). RAG double-click opens the file in the editor. Out of scope: the API-key row alignment from createTextWithPasswordButton.

Progress 2026-11-18: shared SearchTestPanel/SearchTestOutcome in ui/settings/common; WebSearchTestSearch + WebSearchSnippetFormatter; RagSearchTestAdapter replaces RagSearchResultsPanel (open at chunk via OpenFileDescriptor, notification if file gone). Gotcha: JBScrollPane's Mac scrollbar calls Application.getDefaultModalityState(), so tests that build settings components against a mocked Application must stub it (ModalityState.nonModal()). Docs screenshot regenerated with a results state (GENERATE_DOCS_SCREENSHOTS=true); the offscreen render doesn't paint the lazily loaded status icon.

Verification 2026-11-18: full `./gradlew test --rerun` green. Offscreen renders checked for RAG results, error and empty states in light and Darcula: theme colours correct, and query/error text with <, >, & shown literally (dropped JBLabel.setCopyable, which renders text as HTML). Status icons are blank in offscreen renders because AllIcons resolve to DummyIconImpl (0 painted pixels) outside a running IDE; not a bug. NOT yet verified in a running IDE: browser open on double-click/link, opening a RAG file from the modal Settings dialog, Enter on the list, the live busy spinner.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The Settings "Test web search" area now reads like search results instead of a raw text dump, and shares one component with the RAG "Test semantic search" section.

- New `ui/settings/common/SearchTestPanel<T>` + `SearchTestOutcome<T>`: query field (placeholder, Enter runs, button disabled while the query is empty or a search is running), status line (spinner / OK / info / error icon; errors in the theme's error colour), single-line result list (avoids JBR multi-line-cell flicker) and a preview of the selected result with a link heading. All provider and query text is shown literally, never as HTML. Searches run on a SwingWorker; the form is snapshotted on the EDT.
- Web search: `WebSearchTestSearch` adapter (title + domain rows, URL link + cleaned snippet preview, "Keenable returned 3 results · 0.8 s" status, same readiness checks and redacted errors as before, opens only http/https URLs) and `WebSearchSnippetFormatter` (strips Markdown, collapses whitespace, word-boundary truncation at 280 chars).
- RAG: `RagSearchTestAdapter` replaces `RagSearchResultsPanel` (same score/file/folder rows, monospace chunk preview and messages). Double-click opens the file at the chunk (whole-chunk match, then the first substantial line), and shows a notification if the file is gone.
- Tests: formatter, shared panel states (busy blocks a second run, empty query, error/empty/results, failures don't leak the exception message, HTML-safe), RAG helpers + chunk offset; existing Test search E2E tests now use the outcome via WebSearchTestSearchAccess. Full `./gradlew test --rerun` green.
- Docs: web-search.md setup step, new "Testing retrieval from Settings" section in rag.md, regenerated web-search-settings.png with a results state.

Verified by the user in a running IDE: the web search test query and results. Not explicitly verified in the IDE (left unticked as #3 and #10): opening a web result in the browser, and opening a RAG match from the modal Settings dialog (it opens in the editor behind Settings).
<!-- SECTION:FINAL_SUMMARY:END -->
