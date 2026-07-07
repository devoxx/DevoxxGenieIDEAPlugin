---
id: TASK-253
title: Block saving LLM settings when an enabled cloud provider has no API key
status: Done
assignee: []
created_date: '2026-07-07 18:59'
updated_date: '2026-07-07 19:07'
labels:
  - enhancement
  - settings-ui
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up to task-252. The Settings UI currently lets users enable a cloud LLM provider without entering its credential, saving a half-configured provider that can never work (the submit-time guard from task-252 catches it at prompt time).\n\nDesign (approved): validate in LLMProvidersConfigurable.apply() before persisting; if any enabled cloud provider lacks its credential, throw ConfigurationException (standard IntelliJ pattern - dialog shows the error and stays open, nothing is saved). Collect all violations into one aggregated message.\n\nRules: 12 key-field providers (OpenAI, Anthropic, Mistral, Groq, DeepInfra, Gemini, DeepSeek, OpenRouter, Grok, Kimi, GLM, NVIDIA) require a non-blank key when enabled; Azure OpenAI requires key + endpoint + deployment when enabled; AWS Bedrock requires the credential for the selected auth mode (ACCESS_KEY: access key id + secret key, PROFILE: profile name, BEARER_TOKEN: token). Validation reads dialog state, so pre-existing invalid configs are flagged on next Apply. Region and local providers out of scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Apply/OK with an enabled key-field provider and blank key throws ConfigurationException naming the provider; settings are not persisted
- [x] #2 Multiple violations are aggregated into a single error message
- [x] #3 Azure OpenAI validates key, endpoint and deployment when enabled
- [x] #4 AWS Bedrock validates the credential matching the selected auth mode
- [x] #5 Valid configurations (key present, or provider disabled) apply unchanged
- [x] #6 Tests written first (TDD) in LLMProvidersConfigurableTest; full test suite stays green
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented in LLMProvidersConfigurable: apply() now calls validateEnabledProviders() before persisting anything and declares throws ConfigurationException. The validator collects all violations (12 key-field providers via a shared checkApiKeyProvider helper; Azure OpenAI key/endpoint/deployment; AWS Bedrock credential per selected auth mode with fallback to AwsBedrockAuthMode.defaultMode()) and throws a single aggregated ConfigurationException ("Incomplete LLM Provider Configuration"), so the Settings dialog shows the error and stays open with nothing saved. Whitespace-only keys count as blank. TDD: 12 new tests in LLMProvidersConfigurableTest (nested ApplyValidation class) written first and confirmed failing; six pre-existing test methods gained a throws ConfigurationException clause required by the new checked signature. Full suite green.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Blocked saving half-configured cloud LLM providers in the Settings UI. LLMProvidersConfigurable.apply() now validates the dialog state before persisting and throws ConfigurationException when any enabled provider lacks its credential, so the Settings dialog shows the aggregated error and stays open with nothing saved. Covers the 12 key-field providers (blank or whitespace-only key), Azure OpenAI (key, endpoint, deployment) and AWS Bedrock (credential per selected auth mode). Complements the task-252 submit-time guard, which remains as the safety net for configs saved by older plugin versions. TDD: 12 new tests in LLMProvidersConfigurableTest (nested ApplyValidation class) written first and confirmed failing; full test suite green.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1196
<!-- SECTION:FINAL_SUMMARY:END -->
