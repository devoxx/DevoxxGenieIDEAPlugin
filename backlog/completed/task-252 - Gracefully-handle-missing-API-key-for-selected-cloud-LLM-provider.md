---
id: TASK-252
title: Gracefully handle missing API key for selected cloud LLM provider
status: Done
assignee: []
created_date: '2026-07-07 18:45'
updated_date: '2026-07-07 18:53'
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
- [x] #1 Submitting a prompt with a key-based cloud provider selected and no API key configured shows a user-friendly notification instead of an IDE error
- [x] #2 No IllegalArgumentException reaches the EDT / IntelliJ error reporter in this scenario
- [x] #3 Regression test reproduces the blank-key scenario and passes with the fix
- [x] #4 Existing test suite stays green
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Guard added in ActionButtonsPanelController.handlePromptSubmission(): before building the chat model, isApiKeyMissing() checks the selected provider via new static LLMProviderService.requiresApiKey(provider) (backed by the authoritative providerKeyMap, unlike the stale DefaultLLMSettingsUtil.isApiKeyBasedProvider) and LLMProviderService.getApiKey(provider). Blank key -> NotificationUtil balloon pointing to Settings and submission aborted (returns false). Key validation intentionally stays at submit time because the provider combo deliberately never reads the credential store at startup (macOS keychain prompt avoidance, documented in LLMProviderService). Regression test: ActionButtonsPanelControllerTest.testHandlePromptSubmission_CloudProviderWithoutApiKey_ShowsNotificationInsteadOfCrashing (reproduced the original IllegalArgumentException before the fix). Full test suite green.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed the unhandled IllegalArgumentException ("apiKey cannot be null or blank") that surfaced as an "IDE error occurred" balloon when submitting a prompt with a cloud provider selected but no API key configured. Added a submit-time guard in ActionButtonsPanelController.handlePromptSubmission() that checks the selected provider via the new LLMProviderService.requiresApiKey() (backed by providerKeyMap) and shows a friendly notification pointing to Settings instead of crashing. Validation happens at submission time because the provider combo deliberately never reads the credential store at startup (macOS keychain prompt avoidance). Includes a regression test that reproduced the original crash; full test suite green.
<!-- SECTION:FINAL_SUMMARY:END -->
