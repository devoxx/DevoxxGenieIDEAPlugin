# Generic LLM Routers — Design

Date: 2026-11-05
Status: Draft, awaiting maintainer approval
Related: PR #1303 (Requesty), PR #1378 / issue #1340 (DemonRoute), issue #1291 (OrcaRouter)

## Why

Vendors keep asking to be added as LLM providers, and each new provider means copying
classes and editing about 14 existing files (OpenRouter appears in 14 files outside its
own package). Most of these services are OpenAI-compatible, so they differ only in data:
a base URL, a key, and the shape of their model list.

Routers also carry a different privacy risk from other cloud providers. A router forwards
prompts and project code to another provider, so users should see them grouped and warned
about in one place.

## Definition of a router (maintainer's rule)

**Any LLM provider that redirects its inference to another provider is a router.**

## Goals

1. Adding a typical OpenAI-compatible router takes one `ModelProvider` enum line and one
   `RouterDefinition` entry: no new factory, service, DTO, credential enum, state field,
   settings row or `switch` case.
2. A new **Routers** settings tab lists every router, each with a checkbox that controls
   whether it appears in the LLM chat selection.
3. The tab shows a privacy and security warning.
4. Requesty and DemonRoute ship as definitions, replacing PRs #1303 and #1378.
5. Existing OpenRouter and Cloudflare users keep their keys, checkbox state and selected
   model through the upgrade.

## Non-goals

- User-defined routers. The list is fixed in code. There is no "add router" dropdown or
  form (maintainer's decision).
- Moving Cloud Direct providers onto the generic class.
- Changing the Custom OpenAI tab, which remains the place for self-hosted or company
  endpoints.
- Fixing every provider's hardcoded `maxTokens` or tokenizer choice.

## Anti-goals

- The plugin must not appear to vouch for any router. No marketing text, and no vendor
  privacy claims repeated as fact.
- No affiliate, referral or attribution headers or parameters, and no revenue-share
  participation.
- Upgrading must not lose a user's OpenRouter key, enabled state or model selection.
- Listing generic routers in the chat selection must not read the keychain. The settings page
  may read router keys the same way it already reads every other provider's key, with no new
  kind of read. Cloudflare's existing key reads are out of scope and stay as they are.
- A router key must never be deleted because its field wasn't loaded. Fields are always
  populated before `apply`, as they are today.

## Maintainer decisions

- Tabs become `Local | Cloud Direct | Routers | Custom OpenAI`. Only the first-listed
  "Cloud" label is renamed; "Custom OpenAI" keeps its name.
- **Routers:** OpenRouter, Cloudflare, Requesty, DemonRoute.
- **Cloud Direct:** OpenAI, Anthropic, Mistral, Google, DeepSeek, Grok, Kimi, GLM, Groq,
  DeepInfra, NVIDIA, Azure OpenAI, Bedrock.
- Each router has a checkbox that enables or disables it in the LLM chat selection.
- One generic Java class is used instead of per-router classes.
- PRs #1303 and #1378 are closed with credit; both routers become definitions.

## Design

Package: `com.devoxx.genie.chatmodel.cloud.router`.

### RouterDefinition

An immutable record, with all entries held in one registry class (`RouterRegistry`):

| Field | Purpose |
|---|---|
| `provider` | The `ModelProvider` constant |
| `label` | Name shown on the settings row |
| `baseUrl` | OpenAI-compatible base URL |
| `modelEndpoints` | One or more paths to fetch model lists from, merged in order, de-duplicated by id |
| `credentialSubKey` | PasswordSafe sub-key. OpenRouter reuses `openRouterKey` |
| `keyUrl` | Link for obtaining a key |
| `hint` | Optional neutral factual text |
| `catalogFields` | Optional JSON field mapping (below) |
| `maxTokens` | Optional fixed value; null means use the value from `CustomChatModel` |

### RouterChatModelFactory

One `ChatModelFactory` class constructed from a `RouterDefinition`. It:

- Builds `OpenAiChatModel` and `OpenAiStreamingChatModel` with the same options the
  existing factories use (retries, temperature, top-p, timeout, thinking, listeners).
- Lists models from `modelEndpoints` through one generic catalog reader.
- Separates `getModels()`, which may fetch and must run off the EDT, from a new non-fetching
  `getCachedModels()`, which returns whatever is already loaded.
- Caches successful results, including a partial result where one endpoint failed, until
  `resetModels()`. A fully failed or empty fetch is remembered for 60 seconds so an unreachable
  router isn't probed repeatedly. Refresh clears this. Settings Apply clears it for a router
  whose key or checkbox changed.
- Is shared across panels and projects, so its cache is thread-safe: readers get an immutable
  snapshot, published atomically.
- Rejects stale results using a generation counter. Each fetch records the generation when it
  starts, `resetModels()` increments it, and a fetch that finishes under an older generation
  neither updates the cache nor records a failure. Otherwise a request started with key A could
  finish after Apply changed the key to B and restore A's account-specific catalog indefinitely.
  A caller still waiting on a stale fetch gets the current cached snapshot.
- When disabled, `getModels()` checks this first and returns no models, with no key read, no
  request and no retry delay. A background load queued before the router was disabled therefore
  sends nothing when it runs.
- With no key, skips fetching and returns no models. When a key is present it is sent to every
  endpoint. This also skips Requesty's public list, which keeps one rule for all routers.
- Is registered for each definition in `ChatModelFactoryProvider` by looping over the
  registry.

### Catalog reader (no per-router DTOs)

It reads the standard `{"data":[{"id":...}]}` shape as a JSON tree. `catalogFields` names
optional dotted paths:

- Context window, with ordered fallbacks.
- Input and output price, which may be numbers or strings, plus a scale to per-million.
- Display name.
- A filter on a field value.

Without a mapping, or where a value is missing, a model gets a default context window of
128,000 and zero cost. The window is best-effort: it feeds the usage bar and "add project
to context" budget and does not truncate conversations.

| Router | Endpoints | Mapping |
|---|---|---|
| OpenRouter | `/models` | `name`; `context_length`, then `top_provider.context_length`; `pricing.prompt`, `pricing.completion` (× 1,000,000) |
| Requesty | `/models/managed`, `/models` | `context_window`; `input_price`, `output_price` (× 1,000,000); keep entries where `api` is absent or `chat` |
| DemonRoute | `/models` | None: IDs only until a sample response shows otherwise |

Base URLs: OpenRouter `https://openrouter.ai/api/v1/`, Requesty
`https://router.requesty.ai/v1`, DemonRoute `https://api.demonroute.com/v1`.

Requesty's `/models/managed` is public; its `/models` requires a key. A failure on one
endpoint keeps the results of the other (behaviour from PR #1303).

The catalog request and key use the fast-fail probe pattern from `NvidiaChatModelFactory`
(short timeouts, no retries) so a slow router cannot hold up the model list.

### Preserving OpenRouter behaviour

- `maxTokens` stays 4,000 for OpenRouter via the definition. Requesty also uses 4,000, as
  in its PR. DemonRoute uses the `CustomChatModel` value.
- The R50K tokenizer in `ProjectContentService` stays for OpenRouter only. Other generic
  routers use the existing default.
- After moving, `OpenRouterChatModelFactory`, `OpenRouterService`, `model/openrouter`
  DTOs, and the `OpenRouterService` entry in `plugin.xml` are deleted. Their tests are
  ported so the same price, context-window and failure cases still pass.

### ModelProvider

Add `Type.ROUTER`, and move `OpenRouter` and `Cloudflare` to it. Add `Requesty` and
`DemonRoute`. Enum constant names do not change, so stored selections still resolve.

`ProviderType.fromModelProvider` maps `ROUTER` to `CLOUD`, keeping analytics wire values
unchanged.

### Replacing the per-provider sites

| Site | Change |
|---|---|
| `LLMProviderService.providerKeyMap` | Registry lookup for generic routers |
| Enabled `switch`es in `LLMProviderService`, `LlmProviderPanel`, `AgentSettingsComponent` (two) | One `isEnabled(provider)` for generic routers |
| `LlmProviderPanel.isLocalProvider` (refresh), `DefaultLLMSettingsUtil.isApiKeyBasedProvider`, `ProjectContextController.isSupportedProvider` | `RouterRegistry.isGeneric(provider)` |
| `LLMModelRegistryService.getOpenRouterModels` | Loop over enabled definitions |
| `LLMProvidersConfigurable` (isModified, apply, reset, validation, `hasEnabledAuxCloudKey`) | Loop over definitions |
| `DevoxxGenieSettingsService` and `DevoxxGenieStateService` key accessors | Generic accessors by definition; OpenRouter's named accessors removed if nothing else needs them |

**Use `isGeneric`, not `Type.ROUTER`, at these sites.** Cloudflare is `Type.ROUTER` but is
absent from several of these lists today, so a type check would silently change its
behaviour. Cloudflare keeps its existing `case` branches and class.

### Enabled state

`DevoxxGenieStateService` gains `Map<String, Boolean> routerEnabled`, keyed by enum name,
defaulting to off. On load, if the OpenRouter entry is absent, it is seeded from the legacy
`isOpenRouterEnabled`. The legacy field remains readable so the value is not lost.

### Credentials

`CredentialService` gains overloads taking a sub-key string. `CredentialKey` is not extended
for new routers, because its reflective migration expects a matching legacy plaintext field
and new routers have none (PR #1303 had to add one). OpenRouter's key is stored under the
same PasswordSafe entry, so it carries over unchanged.

### Chat selection

An enabled router must appear even if its catalog has never loaded.
`getEnabledCloudModelProviders` currently derives providers from the model registry, so a
router with no registry entries would never be listed. Enabled generic routers are added
directly from the `routerEnabled` map.

This check reads only flags: no keychain and no network. Keys are read when a model is
listed or a prompt runs. Catalog fetches stay off the EDT, as in the existing background
load at `LlmProviderPanel` (`factory.getModels()`).

A router that is enabled but has no key shows in the selection with no models. Settings
validation flags it through the existing `checkApiKeyProvider` path.

### Registry fetch cost

`LLMModelRegistryService.getModels()` currently creates a new `OpenRouterChatModelFactory`
per call, so nothing is cached between calls and each call reads the key and can hit the
network. It has synchronous callers, including provider listing during panel setup,
`LanguageModelCostSettingsComponent`, `TokenCalculationService`, and `AddDirectoryAction`.

**The registry never fetches router catalogs.** It only merges `getCachedModels()` from the
shared factory instances in `ChatModelFactoryProvider`, for enabled routers. This removes the
keychain read and network call from those paths, and a disabled router sends no request.

Catalogs load when a router is selected in the chat panel, through the existing background
load, and on Refresh. Until then, callers looking up a router's model find nothing and use
their existing not-found behaviour: unknown cost and the default window. The planner must check
each caller listed above handles this.

`AgentSettingsComponent` has two loaders that call `factory.getModels()` from Swing code:
`updateSubAgentModelComboBox` and `updateRowModelComboBox`. For generic routers, both move off
the EDT. Inside the modal settings dialog, use `executeOnPooledThread` with
`ModalityState.any()`, not `Task.Backgroundable`. Both loaders must also:

- Restore the saved model after loading completes, because selecting it before the items exist
  has no effect.
- Discard a late result if that combo's provider has changed since the load started.
- Not lose the saved model if the user presses Apply while loading is still in progress. The
  saved value is kept until the load finishes, rather than reading an empty combo.

Cloudflare's registry path (`getCloudflareModels`) is unchanged, so it still reads its key on
registry calls. That's a known limitation for a follow-up.

### Settings UI (`LLMProvidersComponent`)

- `"Cloud"` becomes `"Cloud Direct"`. OpenRouter and Cloudflare rows move out.
- `createRoutersPanel()`, in this order:
  1. Warning (below).
  2. A row per generic router from the registry: checkbox, password field, key link, hint.
  3. Cloudflare's existing rows and fields, unchanged.
- Fields are held in a map keyed by provider, not one named field per router.

Warning text:

> **Routers forward your prompts and code to other providers.** Anything you send, including
> project files added to context, may pass through the router and whichever provider it
> selects. Each has its own logging, retention and training policies, and a provider may be
> in another country. DevoxxGenie doesn't verify these services or their privacy claims, and
> listing one here isn't an endorsement. Check the router's terms before enabling it, and
> avoid sending confidential code or secrets unless your organisation allows it.

### Docs

- Update `docusaurus/docs/llm-providers/cloud-models.md` with a Routers section, the provider
  lists Requesty's PR touched, and `getting-started/faq.md` for the renamed tab.
- Update "Adding a New LLM Provider" in `CLAUDE.md` with the two-line router path.
- Add a `plugin.xml` change note.

## Testing

Test-first, per project convention.

- **Definition-driven:** every definition produces chat and streaming models with the correct
  base URL; a table test of catalog mapping for each router using recorded JSON samples.
- **Regression:** ported OpenRouter cases; upgrade tests for legacy enabled flag and existing
  key; persisted provider and model restored after upgrade.
- **Behaviour:** a disabled router sends no request; listing providers and
  `LLMModelRegistryService.getModels()` make no generic-router network or credential calls; a
  failed fetch is retried only after 60 seconds or a Refresh; one failed Requesty endpoint keeps
  the other's results; a router with no key sends no request; a load queued before disabling
  reads no key and sends no request when it runs.
- **Concurrency:** a request started with key A that finishes after a credential change and
  reset doesn't repopulate the cache, and the next load uses key B; a late failure from an old
  request doesn't trigger the 60-second backoff; concurrent readers see complete snapshots.
- **Agent settings:** for both loaders, the saved model is restored after an asynchronous load;
  a late result for a previous provider is discarded; Apply during a load keeps the saved model.
- **Guard:** a test that a new definition needs no other registration, by iterating the
  registry through `ChatModelFactoryProvider`, `LLMProviderService` and the settings rows.
- **Cloudflare:** behaviour at `isGeneric` sites unchanged.
- No live network calls in tests; the existing live `OpenRouterServiceTest` is replaced with
  a mocked response.

Manual: run in the IDE with OpenRouter and Requesty keys. **DemonRoute cannot be tested live**
because the vendor declined to provide a test key, so its mapping is checked only against the
OpenAI shape, and the release notes should say so.

## Rollout

1. Move OpenRouter onto the generic class with no user-visible change.
2. Add the tab, rename and warning.
3. Add Requesty and DemonRoute definitions.
4. Open the PR, then close #1303 and #1378 with comments crediting the authors and linking
   it. Co-author credit for Requesty's catalog logic. Also update issue #1340.

## Left to the builder

- Exact class names beyond those above, and the shape of `catalogFields`.
- Layout details of the warning, using existing IntelliJ UI components.
- The order of generic routers on the tab.

## Calls made by the design author (not decided by the maintainer)

- Fixed-list registry in code with a JSON field mapping, instead of per-router parser classes.
- OpenRouter moves onto the generic class; Cloudflare keeps its own class and form.
- All new routers off by default.
- Warning wording.
- `Type.ROUTER` maps to `CLOUD` in analytics.
- An enabled router with no key is listed in the selection with no models, and sends no requests.
- The registry only merges already-loaded catalogs; failed fetches are remembered for 60 seconds.
  OpenRouter's costs and context windows are therefore unavailable until it is first selected in
  a session, a small behaviour change from today.
- Cloudflare's existing keychain reads stay, as a known limitation for a follow-up.
- The settings page keeps its existing eager key loading instead of a lazy-loading redesign.
- Base branch is `master`: `CLAUDE.md` says `develop`, but no `develop` branch exists locally
  or on the remote.

## Open

- **OrcaRouter (#1291):** not in this change. The maintainer's approval covered "both" PR-backed
  routers, the issue gives no API base URL, and it has a revenue-share offer the plugin won't
  join. Adding it later needs one enum line and one definition.
