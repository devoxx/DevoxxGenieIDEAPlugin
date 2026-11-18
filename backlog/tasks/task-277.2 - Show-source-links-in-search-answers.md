---
id: TASK-277.2
title: Show source links in /search answers
status: Done
assignee:
  - '@claude'
created_date: '2026-11-18T08:10:00Z'
updated_date: '2026-11-18T09:19:00Z'
labels:
  - web-search
dependencies:
  - TASK-277.1
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1423
modified_files:
  - src/test/java/com/devoxx/genie/service/websearch/WebSearchSourcesTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/strategy/WebSearchSourcesE2ETest.java
  - src/test/java/com/devoxx/genie/ui/compose/util/IdeUriHandlerTest.java
  - src/main/java/com/devoxx/genie/service/websearch/WebSearchSources.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionService.java
  - src/main/kotlin/com/devoxx/genie/ui/compose/util/IdeUriHandler.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - docusaurus/docs/features/web-search.md
parent_task_id: TASK-277
priority: medium
type: enhancement
ordinal: 27000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The /search command answers with a single paragraph generated from the search results, and the result URLs are dropped, so users can't open or verify the sources behind an answer. The agent's web_search tool already keeps URLs. Raised in #1423; applies to every provider.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A /search answer in the chat lists the title and original URL of the results it was based on, as clickable links
- [x] #2 Works the same for every registered provider
- [x] #3 When search returns no results the user sees a clear 'no results' message instead of an empty sources list
- [x] #4 Unit tests cover sources present, no results, and results without a title
- [x] #5 Web Search documentation mentions the sources list
- [x] #6 End-to-end test runs /search against a local stub search server and asserts the result URLs reach the chat message that is shown to the user
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Research (2026-11-18):
- /search runs through WebSearchPromptStrategy -> WebSearchPromptExecutionService.searchWeb, which uses AiServices + WebSearchContentRetriever. The retriever performs the search inside AiServices, so the service never sees the results and cannot list them.
- The answer is shown via PromptOutputPanel.addChatResponse -> Compose AiBubble, rendered by mikepenz multiplatform-markdown-renderer. Markdown links become LinkAnnotation.Url, opened through Compose's LocalUriHandler; DevoxxGenie never provides one, so links rely on the Compose Desktop default (java.awt.Desktop.browse), which is unreliable inside an IDE (notably on Linux).
- Today, no results -> retriever injects nothing -> LLM answers "I don't know" (an LLM call for nothing).

Plan:
1. New pure helper com.devoxx.genie.service.websearch.WebSearchSources: renders a "Sources" markdown list (1. [title](url)) from WebSearchOrganicResult; title falls back to the URL's host (then the URL) when missing/blank; escapes markdown link syntax in titles; skips results without an http(s) URL; returns "" when nothing is linkable.
2. WebSearchPromptExecutionService.searchWeb: search once via WebSearchService.search(config, query). If no results -> return an AiMessage "No web results found for ..." with a hint, without calling the LLM. Otherwise send the LLM the same system instruction plus a user message containing the numbered results (title, URL, snippet) and the question, then append the Sources list to the answer. Works for every provider because it only uses WebSearchResults.
3. AiBubble: provide a LocalUriHandler backed by IntelliJ BrowserUtil.browse (small IdeUriHandler class) so every chat markdown link opens in the IDE's configured browser.
4. Tests: WebSearchSourcesTest (sources present, missing/blank title, escaping, non-http/null URL skipped, none linkable); WebSearchPromptExecutionServiceTest updated (answer + Sources, no-results message without LLM call, Google via substituted engine); E2E through WebSearchPromptStrategy with the real Tavily engine against TavilyStubServer and a recording chat model, asserting the AiMessage passed to the chat panel contains the [title](url) links; IdeUriHandler test with BrowserUtil mocked.
5. Docs: web-search.md describes the Sources list and the no-results message; drop "source links" from Future Enhancements.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-18: branch feature/task-277.2-search-source-links stacked on feature/task-277.1-web-search-provider-registry (PR #1431 not merged yet; maintainer chose stacking). Open the PR against the 277.1 branch and retarget to master after #1431 merges.

2026-11-18 decisions (maintainer): plan approved; route ALL chat markdown links through an IntelliJ BrowserUtil-backed LocalUriHandler (not only /search); no-results is shown as a normal answer message without an LLM call (not an error card).

2026-11-18 implementation done:
- WebSearchSources renders '**Sources**' + numbered [title](url) list; title falls back to host; escapes \\ [ ] * _ `; flattens whitespace; percent-encodes ( ) and spaces in URLs; skips non-http(s) URLs and duplicates; '' when nothing linkable (no empty heading).
- WebSearchPromptExecutionService no longer uses AiServices/WebSearchContentRetriever: one WebSearchService.search, then a direct ChatModel call with the system instruction + numbered results (title, URL, snippet/content, 1000-char cap) + question, then the Sources list appended. No results -> WebSearchSources.noResultsMessage as a normal answer, no LLM call. groundedPrompt is an instance method because tests mockStatic this class for getInstance (a static helper got stubbed to null).
- IdeUriHandler (Kotlin) opens chat links via BrowserUtil; only http/https/mailto, everything else ignored (LLM-produced content). Provided to the markdown renderer in AiBubble via CompositionLocalProvider(LocalUriHandler).
- Discovery: langchain4j WebSearchOrganicResult.from rejects null URL and null/blank title, so 'no title' can only come from mocks or custom engines; the fallback is kept and tested with mocked results.
- Verification: WebSearchSourcesTest 10, WebSearchPromptExecutionServiceTest 9, WebSearchSourcesE2ETest 2 (real strategy + service + Tavily engine vs stub; asserts links in the message passed to PromptOutputPanel.addChatResponse; no-results shown as answer, error handler untouched), IdeUriHandlerTest 11; docs build OK; full suite 4298 passed, 24 skipped, 0 failed.
- Not verified: clicking a link in a running IDE (no manual UI run).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
/search answers now end with a "Sources" list: numbered [title](url) links to the results the answer was based on, for every provider.

- WebSearchPromptExecutionService runs one WebSearchService.search, asks the LLM to answer from the numbered results (title, URL, snippet), and appends WebSearchSources.toMarkdown(results). It replaces AiServices + WebSearchContentRetriever, which hid the results from the service.
- WebSearchSources: host fallback for missing titles, markdown escaping, ( ) and space encoding in URLs, http(s) only, de-duplication, no empty heading.
- No results: a clear "No web results found for ..." answer, without an LLM call or an error card.
- Chat links: IdeUriHandler (BrowserUtil-backed LocalUriHandler, http/https/mailto only) is provided to the markdown renderer in AiBubble, so links in every chat answer open in the IDE's configured browser instead of via java.awt.Desktop.
- Docs: web-search.md documents the Sources list and the no-results message.

Verification: WebSearchSourcesTest (10), WebSearchPromptExecutionServiceTest (9), WebSearchSourcesE2ETest (2; real strategy + service + Tavily engine against the stub, asserting the links reach the message passed to the chat panel), IdeUriHandlerTest (11); docs build OK; full suite 4298 passed, 24 skipped, 0 failed. Not verified manually: clicking a link in a running IDE.

Stacked on TASK-277.1 (PR #1431).
<!-- SECTION:FINAL_SUMMARY:END -->
