---
id: TASK-272
title: Support LLM routers through one generic provider and a Routers settings tab
status: To Do
assignee: []
created_date: '2026-11-05T06:39:00Z'
labels: []
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1303
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1378
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1340
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1291
documentation:
  - docs/superpowers/specs/2026-11-05-generic-llm-routers-design.md
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
- [ ] #1 Settings tabs read Local, Cloud Direct, Routers and Custom OpenAI
- [ ] #2 The Routers tab shows the agreed privacy and security warning and lists OpenRouter, Cloudflare, Requesty and DemonRoute
- [ ] #3 Each router's checkbox controls whether it appears in the LLM chat selection, and new routers are off by default
- [ ] #4 Adding a typical OpenAI-compatible router requires only one provider enum line and one definition entry, enforced by a test
- [ ] #5 Existing OpenRouter and Cloudflare users keep their keys, enabled state and selected model after upgrading
- [ ] #6 Listing generic routers in the chat selection reads no credentials and sends no network requests
- [ ] #7 A disabled router or a router without a key sends no requests
- [ ] #8 A catalog request started with an old key cannot repopulate the cache after a credential change
- [ ] #9 Both agent settings model loaders run off the EDT for generic routers and keep the saved model through asynchronous loading and Apply
- [ ] #10 Requests send no affiliate, referral or attribution data
- [ ] #11 Docs and release notes cover the Routers tab, and state that DemonRoute was not tested live
- [ ] #12 PRs #1303 and #1378 are closed with credit to their authors and a link to the replacement PR
<!-- AC:END -->
