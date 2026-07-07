---
id: TASK-252
title: Gracefully handle missing API key for selected cloud LLM provider
status: In Progress
assignee: []
created_date: '2026-07-07 18:45'
labels:
  - bug
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Selecting a cloud provider (e.g. Anthropic) without a configured API key crashes with an unhandled IllegalArgumentException ("apiKey cannot be null or blank") thrown by the langchain4j model builder on the EDT when submitting a prompt, producing an "IDE error occurred" balloon.\n\nRoot cause: LLMProviderService.getEnabledCloudModelProviders() deliberately never reads the credential store (to avoid macOS keychain prompts at IDE startup), so key-less providers appear in the provider combo. At prompt submission, ChatMessageContextUtil.createContext() builds the chat model without validating the key; getApiKey() returns "" and the langchain4j builder throws.\n\nFix: validate the API key at prompt-submission time (user-initiated, so keychain access is acceptable) before creating the chat model; if blank for a key-based provider, show a friendly notification pointing to Settings and abort the submission instead of crashing.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Submitting a prompt with a key-based cloud provider selected and no API key configured shows a user-friendly notification instead of an IDE error
- [ ] #2 No IllegalArgumentException reaches the EDT / IntelliJ error reporter in this scenario
- [ ] #3 Regression test reproduces the blank-key scenario and passes with the fix
- [ ] #4 Existing test suite stays green
<!-- AC:END -->
