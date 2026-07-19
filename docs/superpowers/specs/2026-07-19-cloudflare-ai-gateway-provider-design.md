# Cloudflare AI Gateway Provider — Design

**Status:** Approved
**Date:** 2026-07-19
**Related:** GitHub issue #1210 (Custom OpenAI + Cloudflare AI Gateway 401), PR #1211

## Summary

Add a first-class **Cloudflare** LLM provider that talks to the
[Cloudflare AI Gateway](https://developers.cloudflare.com/ai-gateway/get-started/)
OpenAI-compatible (`/compat`) endpoint. Today users can only reach Cloudflare
by hand-configuring the generic *Custom OpenAI* provider, which requires them
to assemble the full gateway URL themselves — the source of the 401 confusion
in issue #1210. This provider assembles the URL from a Cloudflare **account ID**
and **gateway name**, authenticates with a single Cloudflare **API token**, and
auto-discovers models — matching the ergonomics of the other API-key providers.

## Goals

- One-field auth: a Cloudflare API token, sent as `Authorization: Bearer <token>`.
- Assemble the gateway base URL from account ID + gateway name so users never
  type raw URLs.
- Auto-discover available models from `/compat/models`, with a manual
  model-name override as a fallback.
- Follow the existing cloud-provider patterns (OpenRouter for dynamic model
  discovery; Custom OpenAI for the `/models` probe and override field).

## Non-Goals

- The dual-header auth mode (`cf-aig-authorization` + provider `Authorization`)
  is **out of scope**. Only single-token BYOK is supported. (Downstream provider
  keys are managed in the Cloudflare dashboard.)
- Per-model cost/context-window configuration UI beyond what `/compat/models`
  reports and the shared defaults provide.
- Workers AI-specific endpoints (`/ai/run`), the REST/unified API, or the
  `/responses` agentic endpoint.

## Background: Cloudflare AI Gateway

- **OpenAI-compatible base URL:**
  `https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}/compat`
- The OpenAI client appends `/chat/completions`; the model probe appends
  `/models`. Both resolve correctly against the `/compat` base.
- **Model naming:** `provider/model`, e.g. `openai/gpt-4o-mini`,
  `anthropic/claude-4-5-sonnet`, `workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast`.
- **Auth (BYOK / stored keys):** a single Cloudflare API token in the
  `Authorization: Bearer` header; downstream provider keys are stored in the
  Cloudflare dashboard.
- The `default` gateway is auto-created by Cloudflare on first authenticated
  request, so `default` is a sensible field default.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Auth model | Single Cloudflare token (BYOK) | Matches "api key + account id"; one `Authorization` header maps cleanly onto langchain4j's OpenAI client. |
| Gateway name | Configurable field, default `default` | Supports named gateways without forcing raw-URL entry. |
| Model list | Fetch `/compat/models` + manual override | Auto-discovery convenience with a fallback for empty/failing discovery. |
| Provider `Type` | `CLOUD` | It is fundamentally an API-key provider; account-ID/gateway fields are lightweight config, not the "inconvenient setup" of `OPTIONAL` providers (Azure/Bedrock). |

## Architecture

The provider reuses two established patterns:

- **OpenRouter** — a gateway provider whose models are fetched dynamically and
  injected into `LLMModelRegistryService.getModels()` at call time when a key is
  present. This is what makes the provider appear in the model combo box.
- **Custom OpenAI** — the best-effort, fast-fail `/models` probe, the
  model-name override field, and the "cache ids not built models" discipline.

### URL construction

```
baseUrl = "https://gateway.ai.cloudflare.com/v1/"
        + accountId + "/"
        + gatewayName + "/compat"
```

Assembled once per model build from current settings. Trimmed of surrounding
whitespace; trailing slashes on user input are normalised.

### Component changes

1. **`model/enumarations/ModelProvider`** — add
   `Cloudflare("Cloudflare", Type.CLOUD)`.

2. **`chatmodel/cloud/cloudflare/CloudflareChatModelFactory`** (new):
   - `createChatModel` / `createStreamingChatModel`: `OpenAiChatModel` /
     `OpenAiStreamingChatModel` with the assembled base URL,
     `apiKey = getCloudflareKey()`, `modelName = resolveModelName(...)`.
   - `getModels()`:
     - If the model-name override is enabled and non-blank, return that single
       model and **skip** the `/models` probe (mirrors the #1210 fix).
     - Otherwise probe `{baseUrl}/models` via
       `LocalLLMProviderUtil.getModelsFromUrl(url, ResponseDTO.class, client, cloudflareKey)`
       — reusing the bearer-token overload added in PR #1211 — and map the ids
       to `LanguageModel`s built from current settings.
     - Cache model **ids** only (never built `LanguageModel`s), cleared by
       `resetModels()`.
     - Degrade gracefully (empty list) on any failure; log at `DEBUG`.
   - `resolveModelName(...)`: override field → dropdown selection → `"default"`.

3. **`ui/settings/DevoxxGenieStateService`** (+ `DevoxxGenieSettingsService`
   interface):
   - `cloudflareKey` — secret, keychain-backed via new
     `CredentialKey.CLOUDFLARE_KEY("cloudflareKey")`; hand-written
     `@Transient` accessors like the other keys.
   - `cloudflareAccountId` — plain `@OptionTag`, non-secret, default `""`.
   - `cloudflareGatewayName` — plain `@OptionTag`, default `"default"`.
   - `cloudflareModelName` — plain `@OptionTag`, default `""`.
   - `isCloudflareModelNameEnabled` — boolean, default `false`.
   - `isCloudflareEnabled` — boolean, default `false`.

4. **`service/LLMProviderService`:**
   - `providerKeyMap.put(Cloudflare, () -> getCloudflareKey())`.
   - Add `case Cloudflare -> stateService.isCloudflareEnabled();` to the
     enabled-cloud-provider switch.

5. **`service/models/LLMModelRegistryService.getModels()`:** add
   `getCloudflareModels(modelsCopy)` — when the Cloudflare key and account ID are
   present, call `new CloudflareChatModelFactory().getModels()` and inject each
   under key `Cloudflare.getName() + ":" + modelName`. Exact mirror of
   `getOpenRouterModels(...)`. Injected models are built with `apiKeyUsed(true)`
   so `LLMProviderService.getEnabledCloudModelProviders()` surfaces the provider
   in the combo box.

6. **`chatmodel/ChatModelFactoryProvider`:** register
   `FACTORY_SUPPLIERS.put(Cloudflare, CloudflareChatModelFactory::new)`. The
   `ChatModelFactoryProviderTest` completeness check enforces this.

7. **Settings UI:**
   - `ui/settings/llm/LLMProvidersComponent` — add rows: enable checkbox +
     Cloudflare API Key, Account ID, Gateway name, Model override (with an
     enable checkbox), each with a hint. The URL preview hint shows the
     assembled `.../v1/<account>/<gateway>/compat` form.
   - `ui/settings/llm/LLMProvidersConfigurable` — `isModified`, `apply`, and
     `reset` wiring for every new field.

## Data Flow

```
User selects Cloudflare in provider combo
  → LLMProviderService.getEnabledCloudModelProviders() includes Cloudflare
     (isCloudflareEnabled && models present in registry)
  → model combo populated from LLMModelRegistryService.getModels()
     → getCloudflareModels() → CloudflareChatModelFactory.getModels()
        → (override set?) yes: [override model]; skip probe
        → no: GET {base}/compat/models  (Authorization: Bearer <token>)
  → user submits prompt
  → CloudflareChatModelFactory.createChatModel()/createStreamingChatModel()
     → POST {base}/compat/chat/completions  (Authorization: Bearer <token>)
```

## Error Handling

- Missing/blank account ID or API key → discovery returns an empty model list
  (no crash); the provider simply shows no models until configured. Chat
  requests fail with the provider's own error surfaced as usual.
- `/compat/models` probe failure (401, unreachable, unparseable) → empty list,
  logged at `DEBUG`; the manual override remains usable.
- Fast-fail probe client (short connect/read timeout, no retries) so a slow or
  misconfigured gateway never blocks the UI.

## Testing

- **`CloudflareChatModelFactoryTest`** (new), mirroring the Custom OpenAI suite:
  - Base URL is assembled correctly from account ID + gateway name
    (`.../v1/<acct>/<gw>/compat/models` is the probed URL).
  - The `/models` probe carries the API token as a bearer token.
  - The model-name override skips the probe and returns the override model.
  - Blank override still probes.
  - Blank account ID / URL degrades to an empty list.
  - `createChatModel` / `createStreamingChatModel` return non-null.
- **`ChatModelFactoryProviderTest`** — existing completeness check passes once
  the provider is registered.
- Test suite run per repo convention (JDK 21):
  `./gradlew test --tests "...cloudflare.*" --tests "...ChatModelFactoryProviderTest"`.

## Rollout / Branch Notes

- Built on the `feature/cloudflare-ai-gateway-provider` branch, which is cut
  from the `#1210` fix branch because it depends on the
  `getModelsFromUrl(..., bearerToken)` overload introduced in PR #1211.
- Once PR #1211 merges to `master`, this branch rebases cleanly onto `master`.
