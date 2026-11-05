---
id: TASK-272
title: Support LLM routers through one generic provider and a Routers settings tab
status: In Progress
assignee:
  - Claude
created_date: '2026-11-05T06:39:00Z'
updated_date: '2026-11-05T10:46:00Z'
labels: []
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1303
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1378
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1340
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1291
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1384
documentation:
  - docs/superpowers/specs/2026-11-05-generic-llm-routers-design.md
modified_files:
  - AGENTS.md
  - CLAUDE.md
  - README.md
  - docusaurus/docs/features/overview.md
  - docusaurus/docs/getting-started/faq.md
  - docusaurus/docs/getting-started/quick-start-cloud.md
  - docusaurus/docs/getting-started/why-devoxxgenie.md
  - docusaurus/docs/llm-providers/cloud-models.md
  - docusaurus/docs/llm-providers/overview.md
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/cloud/openrouter/OpenRouterChatModelFactory.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/cloud/openrouter/OpenRouterService.java
  - src/main/java/com/devoxx/genie/chatmodel/cloud/router/CatalogFields.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogClient.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogParser.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterChatModelFactory.java
  - src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterDefinition.java
  - src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterRegistry.java
  - src/main/java/com/devoxx/genie/controller/ProjectContextController.java
  - src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java
  - src/main/java/com/devoxx/genie/model/openrouter/Data.java
  - src/main/java/com/devoxx/genie/model/openrouter/Pricing.java
  - src/main/java/com/devoxx/genie/model/openrouter/ResponseDTO.java
  - src/main/java/com/devoxx/genie/model/openrouter/TopProvider.java
  - src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java
  - src/main/java/com/devoxx/genie/service/LLMProviderService.java
  - src/main/java/com/devoxx/genie/service/analytics/ProviderType.java
  - src/main/java/com/devoxx/genie/service/credentials/CredentialKey.java
  - src/main/java/com/devoxx/genie/service/credentials/CredentialService.java
  - >-
    src/main/java/com/devoxx/genie/service/credentials/PasswordSafeCredentialService.java
  - src/main/java/com/devoxx/genie/service/models/LLMModelRegistryService.java
  - src/main/java/com/devoxx/genie/ui/panel/LlmProviderPanel.java
  - src/main/java/com/devoxx/genie/ui/renderer/ModelInfoRenderer.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/ProviderEnabledResolver.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - >-
    src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsConfigurable.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/ModelComboLoader.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java
  - src/main/java/com/devoxx/genie/util/DefaultLLMSettingsUtil.java
  - src/main/resources/META-INF/plugin.xml
  - src/test/java/com/devoxx/genie/chatmodel/ChatModelFactoryProviderTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/cloud/azureopenai/AzureOpenAiChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/cloud/openrouter/OpenRouterChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogParserTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterChatModelFactoryTest.java
  - >-
    src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterRegistryTest.java
  - src/test/java/com/devoxx/genie/controller/ProjectContextControllerTest.java
  - >-
    src/test/java/com/devoxx/genie/model/enumarations/ModelProviderCloudflareTest.java
  - src/test/java/com/devoxx/genie/service/LLMProviderServiceRouterTest.java
  - src/test/java/com/devoxx/genie/service/LLMProviderServiceTest.java
  - >-
    src/test/java/com/devoxx/genie/service/credentials/PasswordSafeCredentialServiceSubKeyTest.java
  - >-
    src/test/java/com/devoxx/genie/service/models/LLMModelRegistryServiceRouterTest.java
  - src/test/java/com/devoxx/genie/service/openrouter/OpenRouterServiceTest.java
  - src/test/java/com/devoxx/genie/ui/panel/LlmProviderPanelTest.java
  - src/test/java/com/devoxx/genie/ui/renderer/ModelInfoRendererTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceRouterTest.java
  - src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceTest.java
  - src/test/java/com/devoxx/genie/ui/settings/ProviderEnabledResolverTest.java
  - src/test/java/com/devoxx/genie/ui/settings/agent/ModelComboLoaderTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersRouterSettingsTest.java
  - src/test/resources/router/openrouter-models.json
  - src/test/resources/router/requesty-models.json
priority: medium
type: feature
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Vendors keep asking to be added as LLM providers (Requesty, DemonRoute, OrcaRouter), and each one means copying classes and editing about 14 existing files, even though these services differ mainly in a base URL, a key and a model-list shape. Routers also carry a different privacy risk, because they forward prompts and project code to other providers, so users should see them grouped and warned about in one place. Maintainer's rule: any LLM provider that redirects its inference to another provider is a router. Decisions: the list is fixed in code with a checkbox per router and no user-defined routers; Cloud Direct holds providers that serve inference themselves, including NVIDIA, Azure OpenAI and Bedrock; the plugin must not appear to endorse any router or join revenue-share or attribution programmes. OrcaRouter (#1291) is out of scope because its issue gives no API URL.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Settings tabs read Local, Cloud Direct, Routers and Custom OpenAI
- [x] #2 The Routers tab shows the agreed privacy and security warning and lists OpenRouter, Cloudflare, Requesty and DemonRoute
- [x] #3 Each router's checkbox controls whether it appears in the LLM chat selection, and new routers are off by default
- [x] #4 Adding a typical OpenAI-compatible router requires only one provider enum line and one definition entry, enforced by a test
- [ ] #5 Existing OpenRouter and Cloudflare users keep their keys, enabled state and selected model after upgrading
- [x] #6 Listing generic routers in the chat selection reads no credentials and sends no network requests
- [x] #7 A disabled router or a router without a key sends no requests
- [x] #8 A catalog request started with an old key cannot repopulate the cache after a credential change
- [ ] #9 Both agent settings model loaders run off the EDT for generic routers and keep the saved model through asynchronous loading and Apply
- [x] #10 Requests send no affiliate, referral or attribution data
- [x] #11 Docs and release notes cover the Routers tab, and state that DemonRoute was not tested live
- [x] #12 PRs #1303 and #1378 are closed with credit to their authors and a link to the replacement PR
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Follow docs/superpowers/plans/2026-11-05-generic-llm-routers.md on branch feat/generic-llm-routers, using subagent-driven development: (1) router credentials and enabled state, (2) definitions and catalog parser, (3) RouterChatModelFactory, (4) move OpenRouter onto the generic path, (5) Routers settings tab, (6) asynchronous agent settings loaders, (7) add Requesty and DemonRoute plus docs, (8) verify, open the PR and close #1303 and #1378. Design: docs/superpowers/specs/2026-11-05-generic-llm-routers-design.md. Plan and design were approved by the maintainer on 2026-11-05.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verified on 71a3f577: full suite 4134 tests, 0 failures, 0 errors, 22 skipped; verifyPlugin compatible with IU-253.31033.145 and IU-261.22158.277.

Criteria 5 and 9 stay open until the manual IDE check: the pieces are tested (key reuse, legacy-flag seed, ModelComboLoader), but the upgrade and the settings dialog have not been exercised in a running IDE, and AgentSettingsComponent's wiring to effectiveModelName() was verified by reading only. Requesty and DemonRoute have not been tested against live services.

Known residuals: a routerEnabled XML entry with a null key or value makes state deserialization throw (hand-edited or corrupted XML only). Sibling providers can still delete a stored key on an empty keychain read (pre-existing). Tracked META-INF/plugin.xml.bak still names the deleted OpenRouterService.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added one generic router provider (RouterDefinition, RouterRegistry, RouterChatModelFactory), so a typical OpenAI-compatible router takes one enum line and one definition entry, enforced by a guard test. OpenRouter moved onto it, Requesty and DemonRoute were added, the settings tabs are now Local, Cloud Direct, Routers and Custom OpenAI with a privacy warning and per-router checkboxes, and both agent settings model loaders run off the EDT for generic routers. PR #1384 replaces #1303 and #1378, which are closed with credit.

Verified on 71a3f577: 4134 tests, 0 failures, 22 skipped; verifyPlugin compatible with IU-253.31033.145 and IU-261.22158.277.

Not yet verified: no run in an IDE, and no live test of any router. Criteria 5 and 9 stay open until the maintainer's IDE check, and the task stays In Progress until then.
<!-- SECTION:FINAL_SUMMARY:END -->
