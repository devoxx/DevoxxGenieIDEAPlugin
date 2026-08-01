---
id: TASK-256
title: 'Fix Cloudflare Workers AI agent tool-calling 400 AiError 5006 (issue #1256)'
status: Done
assignee: []
created_date: '2026-08-01 14:30'
updated_date: '2026-08-01 14:30'
labels: []
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1256'
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1254'
ordinal: 4100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Issue #1256: in agent mode with a Cloudflare Workers AI model, the first request succeeds (the model returns a `search_files` tool call, the user approves it), but the follow-up round trip that carries the tool result fails with `400 AiError 5006 "Bad input: Error: oneOf at '/' not met"` — complaining that `messages[].content` is an array instead of a string and that a message is missing `role`/`content`.

Root cause: the plugin routes Workers AI models (`workers-ai/@cf/...`) through the gateway-level `/compat/chat/completions` endpoint (introduced for issue #1254). That endpoint forwards chat bodies to the Workers AI *native* model endpoint, whose per-model input JSON schema (`oneOf`: `prompt` | `messages` with plain-string content | `input` | `requests`) cannot represent OpenAI tool-calling shapes — an assistant message carrying `tool_calls` with `content: null`, or `role:"tool"` results. Plain chat passes (langchain4j serializes single-text messages as strings), so the failure only appears on the first tool round trip.

Cloudflare provides a correct route for exactly this: the gateway's Workers AI provider path `https://gateway.ai.cloudflare.com/v1/{account}/{gateway}/workers-ai/v1/chat/completions`, served by Workers AI's own OpenAI-compatibility layer, which per the Workers AI changelog supports tool calling (assistant `content: null` + `tool_calls`, `tool_call_id` round trips) and addresses models by their bare `@cf/...` id.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Workers AI models (`@cf/...` bare or `workers-ai/@cf/...`) are routed to `.../{account}/{gateway}/workers-ai/v1/chat/completions` with the bare `@cf/...` model id (chat + streaming)
- [x] #2 Non-Workers-AI gateway models (e.g. `openai/gpt-4o`) keep using `/compat` with `provider/model` ids
- [x] #3 An agent tool round trip (assistant `tool_calls` + `role:"tool"` result) reaches the workers-ai path — wire-level regression test
- [x] #4 Model discovery (`/compat/models`) and the model-name override normalization from issue #1254 are unchanged
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
`CloudflareGatewayUrl.workersAiBaseUrl()` assembles the workers-ai/v1 base; `CloudflareModelName.isWorkersAi()` / `stripWorkersAiPrefix()` detect Workers AI ids and derive the bare wire id; `CloudflareChatModelFactory` picks base URL + wire model per resolved model in both `createChatModel` and `createStreamingChatModel`. The dropdown/display id stays in the gateway-canonical `workers-ai/@cf/...` form. Wire tests updated: Workers AI models now asserted against `/v1/{acct}/{gw}/workers-ai/v1/chat/completions` with bare ids, plus a new `agentToolRoundTripIsSentToWorkersAiEndpoint` regression test replaying the issue's tool round trip and a `nonWorkersAiModelStaysOnCompatEndpoint` guard. NOTE: implemented in a sandbox without Maven Central access — the test suite must be verified by CI.
<!-- SECTION:NOTES:END -->
