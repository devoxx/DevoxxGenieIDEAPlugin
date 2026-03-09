---
id: TASK-200
title: Support AWS Bearer Token authentication for Bedrock
status: In Progress
assignee:
  - codex
created_date: '2026-03-08 17:54'
updated_date: '2026-03-08 18:17'
labels:
  - enhancement
  - bedrock
  - authentication
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/966'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add support for `AWS_BEARER_TOKEN_BEDROCK` as an alternative authentication method for Amazon Bedrock, allowing users to access Bedrock models without needing to configure `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` or an AWS profile.

**GitHub Issue:** #966

**Current state:** Bedrock authentication requires AWS access key ID + secret access key, or an AWS profile. Some users have bearer tokens (e.g., from SSO/identity center) and cannot easily provide static credentials.

**What needs to change:**
- Add a bearer token field to the Bedrock provider settings UI
- Update the Bedrock `ChatModelFactory` to support bearer token credential resolution
- Ensure the Langchain4J Bedrock integration supports this auth method (may require custom credential provider)
- Add appropriate validation and error handling for bearer token auth
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Users can authenticate to Bedrock using an AWS bearer token instead of access key/secret key
- [ ] #2 Bearer token option is available in Bedrock provider settings UI
- [ ] #3 Existing access key/secret key and profile authentication methods continue to work unchanged
- [ ] #4 Clear error message when bearer token is invalid or expired
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Proposed implementation plan (pending user approval):
1. Add a persisted Bedrock authentication mode setting with three explicit options: access key/secret key, AWS profile, and bearer token. Keep AWS region and regional inference settings shared across all modes.
2. Update the Bedrock settings UI to present a single authentication mode selector and show only the fields for the active mode. Add a secure bearer token input field and remove ambiguity from the current profile-vs-direct-credentials toggle flow.
3. Refactor Bedrock client creation into a shared auth resolver/factory used by both runtime inference and model listing, so `BedrockModelFactory` and `BedrockService` follow the same auth mode.
4. Implement bearer-token auth using the AWS SDK Bedrock / Bedrock Runtime token auth path, while preserving existing access-key and profile flows unchanged.
5. Update settings validation/provider availability checks so Bedrock is considered configured when the selected auth mode has the required fields populated, instead of assuming secret/access key presence.
6. Add or update tests for settings persistence, UI/configurable behavior, Bedrock auth resolution, and regression coverage for all three auth modes.
7. Validate with targeted tests and manual code-path review that model listing and chat/streaming inference use the same selected auth mode and produce clear errors for invalid or expired bearer tokens.

Open design decision captured for implementation:
- Preferred UI is an explicit authentication mode selector rather than an 'or' label beside multiple simultaneously visible fields.
- AWS region remains required for all auth modes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Research 2026-03-08: latest LangChain4j Bedrock docs explicitly mention `AWS_BEARER_TOKEN_BEDROCK` for API key authentication (`/Users/stephan/IdeaProjects/langchain4j/docs/docs/integrations/language-models/amazon-bedrock.md`).

Research 2026-03-08: current/latest LangChain4j Bedrock chat builders do not expose a dedicated bearer-token field; they either create AWS SDK clients with `DefaultCredentialsProvider.create()` or accept a caller-supplied `BedrockRuntimeClient` / `BedrockRuntimeAsyncClient` (`/Users/stephan/IdeaProjects/langchain4j/langchain4j-bedrock/src/main/java/dev/langchain4j/model/bedrock/BedrockChatModel.java`, `/Users/stephan/IdeaProjects/langchain4j/langchain4j-bedrock/src/main/java/dev/langchain4j/model/bedrock/BedrockStreamingChatModel.java`).

Research 2026-03-08: AWS SDK v2.42.1 Bedrock and Bedrock Runtime client builders support bearer token auth via `tokenProvider(...)`, and their generated base builders also auto-read `AWS_BEARER_TOKEN_BEDROCK` / `aws.bearerTokenBedrock` (`software.amazon.awssdk.services.bedrock.DefaultBedrockBaseClientBuilder`, `software.amazon.awssdk.services.bedrockruntime.DefaultBedrockRuntimeBaseClientBuilder`, `EnvironmentTokenSystemSettings` from local Gradle sources).

Research 2026-03-08: plugin gap is DevoxxGenie-specific. Current settings/UI/state only model AWS access key, secret key, profile, and region; `BedrockModelFactory` and `BedrockService` always build clients from access-key/profile code paths; Bedrock settings visibility and provider-key checks also assume secret/access key presence (`src/main/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockModelFactory.java`, `src/main/java/com/devoxx/genie/chatmodel/cloud/bedrock/BedrockService.java`, `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java`, `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java`, `src/main/java/com/devoxx/genie/service/LLMProviderService.java`).

Implementation implication: task-200 does not require waiting on a new LangChain4j feature. It can be implemented in DevoxxGenie by adding bearer-token state/UI and then either (a) constructing Bedrock/BedrockRuntime clients with AWS SDK `tokenProvider(...)`, or (b) setting the Bedrock bearer-token system setting/env path explicitly before client construction. Control-plane model listing (`BedrockClient`) and runtime inference (`BedrockRuntimeClient` / async) both need the same auth path.

Plan approved by user on 2026-03-08. Proceed with implementation and update existing related tests as part of the change.

Implemented 2026-03-08: added persisted `AwsBedrockAuthMode` with `ACCESS_KEY`, `PROFILE`, and `BEARER_TOKEN`, plus `awsBearerToken` setting. Legacy `shouldPowerFromAWSProfile` is kept synchronized for backward compatibility in state handling.

Implemented 2026-03-08: Bedrock settings UI now uses an explicit authentication method selector and conditionally shows access-key, profile, or bearer-token fields while keeping region/regional inference shared.

Implemented 2026-03-08: introduced shared `BedrockAuthResolver` and switched both `BedrockModelFactory` and `BedrockService` to use it so runtime inference and model listing share the same auth mode resolution, including AWS SDK bearer-token configuration.

Implemented 2026-03-08: updated provider/settings gating so Bedrock enablement is based on the selected auth mode plus region, and updated related tests accordingly.

Verification 2026-03-08: `./gradlew -q test --tests com.devoxx.genie.ui.settings.DevoxxGenieStateServiceTest --tests com.devoxx.genie.ui.settings.llm.LLMProvidersConfigurableTest --tests com.devoxx.genie.chatmodel.cloud.bedrock.BedrockModelFactoryTest --tests com.devoxx.genie.chatmodel.cloud.bedrock.BedrockAuthResolverTest --tests com.devoxx.genie.ui.panel.LlmProviderPanelTest` passed.

Follow-up consideration: bearer-token auth now works through AWS SDK client configuration, but the task remains In Progress until we decide whether additional explicit user-facing rewording is needed for invalid/expired bearer-token runtime errors beyond current AWS/LangChain4j exception messages.
<!-- SECTION:NOTES:END -->
