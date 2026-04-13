# DevoxxGenie Analytics Schema

This document is the shared GA4 event schema for DevoxxGenie (IntelliJ plugin) and the
GenieBuilder admin UI. Both projects MUST agree on this schema before changes ship.

## Guarantees

- **Consent-gated.** Nothing is sent unless `analyticsNoticeAcknowledged` AND
  `analyticsEnabled` are both true in `DevoxxGenieStateService`.
- **Anonymous.** Client ID is a locally-generated UUID; session ID is a random 10-digit
  integer re-rolled on every IDE launch.
- **Fire-and-forget.** All POSTs are async and never block the EDT; failures are silent.
- **Closed allowlists.** Every event has a closed per-event param allowlist enforced in
  `AnalyticsEventBuilder`. Unknown params are dropped and the event is not sent.
- **Enum-typed.** `feature_id`, `provider_type`, and every bucketed count are closed enums.
  Free-form values (`provider_id`, `model_name`) are accepted but pass through a shape
  filter that rejects absolute paths, URLs (`://`), newlines, and values over 128 chars.
- **No PII.** The schema NEVER carries prompt text, response text, file content, file
  paths, project names, MCP server names/URLs/commands, tool names, user-defined custom
  prompt names, API keys, user identity, git remotes, token counts, or cost data.

## Transport

All events are sent as a single GA4 Measurement Protocol POST per event:

```
POST {analyticsEndpoint}
Content-Type: application/json

{
  "client_id": "<uuid>",
  "events": [{
    "name": "<event_name>",
    "params": { ... }
  }]
}
```

The endpoint is configured via `DevoxxGenieStateService.analyticsEndpoint` and routes through
the shared GenieBuilder Cloudflare worker. DevoxxGenie traffic is segmented from GenieBuilder
Electron traffic by `params.app_name = "devoxxgenie-intellij"`.

## Common params (attached to every event)

| Param | Type | Description |
|-------|------|-------------|
| `app_name` | string constant | Always `devoxxgenie-intellij` |
| `app_version` | string | Plugin version from `plugin.xml` |
| `ide_version` | string | IntelliJ full version from `ApplicationInfo` |
| `session_id` | 10-digit int as string | Re-rolled per IDE launch |
| `engagement_time_msec` | int literal `1` | GA4 requires non-zero engagement |

## Events

### `prompt_executed` (task-206)

Fired once per LLM prompt dispatch (after local slash-command handling, before network).

| Param | Allowed values | Example |
|-------|----------------|---------|
| `provider_id` | free-form, shape-filtered | `anthropic` |
| `model_name` | free-form, shape-filtered | `claude-3-5-sonnet` |

### `model_selected` (task-206)

Fired when the user changes the selected model in the LLM picker.

| Param | Allowed values | Example |
|-------|----------------|---------|
| `provider_id` | free-form, shape-filtered | `ollama` |
| `model_name` | free-form, shape-filtered | `llama3` |

### `feature_enabled` (task-209)

One event per enabled feature, emitted in the session-enablement snapshot. Emitted at most
once per IDE session (`AtomicBoolean`-guarded) and re-armed on settings change via
`DevoxxGenieSettingsChangedTopic`.

| Param | Allowed values |
|-------|----------------|
| `feature_id` | `rag`, `web_search_google`, `web_search_tavily`, `agent`, `mcp`, `streaming`, `custom_prompt` |

**Usage-only feature IDs** (`semantic_search`, `project_context_full`,
`project_context_selected`, `devoxxgenie_md`) are explicitly **rejected** if passed to
`trackFeatureEnabled` — they only appear as `feature_used`.

### `feature_used` (task-209)

One event per activated feature, per prompt. A prompt that activates RAG + Web Search +
Agent emits three events.

| Param | Allowed values |
|-------|----------------|
| `feature_id` | `rag`, `semantic_search`, `web_search_google`, `web_search_tavily`, `agent`, `mcp`, `streaming`, `project_context_full`, `project_context_selected`, `devoxxgenie_md`, `custom_prompt` |
| `provider_type` | `local`, `cloud`, `none` |
| `tool_call_count` | `0`, `1`, `2-5`, `6-10`, `11+` |

`tool_call_count` is only semantically meaningful for `feature_id = agent` and
`feature_id = mcp`. All other usage events emit `"0"` — a deliberate constant to keep the
schema flat and dashboard logic predictable.

### `feature_counts` (task-209)

One event per IDE session, emitted alongside the `feature_enabled` snapshot.

| Param | Allowed values |
|-------|----------------|
| `mcp_server_count` | `0`, `1`, `2-5`, `6-10`, `11+` |
| `custom_prompt_count` | `0`, `1`, `2-5`, `6-10`, `11+` |
| `chat_memory_bucket` | `0`, `1-5`, `6-10`, `11-20`, `21+` |

## Enum reference

### `feature_id` (closed set)

| Wire value | Source of truth (plugin) | Usage-only |
|------------|--------------------------|:---:|
| `rag` | `DevoxxGenieStateService.ragEnabled` / `ChatMessageContext.ragActivated` | |
| `semantic_search` | `SemanticSearchService.search()` invocation | ✓ |
| `web_search_google` | `isGoogleSearchEnabled` / `webSearchActivated` | |
| `web_search_tavily` | `isTavilySearchEnabled` / `webSearchActivated` | |
| `agent` | `agentModeEnabled` / `AgentLoopTracker.getCallCount()` | |
| `mcp` | `mcpEnabled` + configured server / `InstrumentedMcpToolProvider` counter | |
| `streaming` | `streamMode` | |
| `project_context_full` | `ChatMessageContext.projectContextFullUsed` | ✓ |
| `project_context_selected` | `ChatMessageContext.projectContextSelectedUsed` | ✓ |
| `devoxxgenie_md` | `ChatMessageContext.devoxxGenieMdUsed` | ✓ |
| `custom_prompt` | `ChatMessageContext.commandName != null` / `DevoxxGenieStateService.customPrompts` | |

### `provider_type` mapping from `ModelProvider.Type`

| `ModelProvider.Type` | Wire value | Providers |
|----------------------|------------|-----------|
| `LOCAL` | `local` | Ollama, LMStudio, GPT4All, Jan, LLaMA.cpp, Exo, CustomOpenAI, CLIRunners, ACPRunners |
| `CLOUD` | `cloud` | OpenAI, Anthropic, Mistral, Groq, DeepInfra, Google, OpenRouter, DeepSeek, Grok, Kimi, GLM |
| `OPTIONAL` | `cloud` | Azure OpenAI, Bedrock (enterprise cloud endpoints) |
| _no model_ | `none` | (fallback) |

`OPTIONAL` is folded into `cloud` deliberately — both are cloud-hosted, they just require
extra setup. The wire schema has no `optional` value.

### Bucket ladders

**Standard** — for `mcp_server_count`, `custom_prompt_count`, `tool_call_count`:

| Raw | Bucket |
|-----|--------|
| ≤0  | `0`    |
| 1   | `1`    |
| 2–5 | `2-5`  |
| 6–10| `6-10` |
| ≥11 | `11+`  |

**Chat memory** — for `chat_memory_bucket`:

| Raw | Bucket |
|-----|--------|
| ≤0   | `0`     |
| 1–5  | `1-5`   |
| 6–10 | `6-10`  |
| 11–20| `11-20` |
| ≥21  | `21+`   |

## Validation layers

Every event goes through `AnalyticsEventBuilder.build()` which enforces, in order:

1. Event name must be in `EVENT_ALLOWLIST`.
2. Every event-specific param key must be in the allowlist for that event.
3. Every enum-typed param value must be in `ENUM_VALUE_ALLOWLIST[param]`.
4. Every string value must pass `rejectShape()`: no leading `/` or `\`, no `://`, no
   newlines, ≤ 128 characters.
5. Every common param must be in `COMMON_PARAM_KEYS`.

A failure at any layer drops the event and logs at debug level. The caller sees `null`
and silently does not send.

## Changing the schema

Any change to the schema (new event, new param, new enum value) MUST:

1. Update this document.
2. Update the disclosure copy in all three lockstep surfaces:
   `AnalyticsConsentNotifier`, `GeneralSettingsComponent`, `plugin.xml` marketplace description.
3. Update `AnalyticsEventBuilder.EVENT_ALLOWLIST` / `ENUM_VALUE_ALLOWLIST`.
4. Add unit tests to `AnalyticsEventBuilderTest` asserting both positive acceptance and
   rejection of the boundary.
5. File a matching task in the GenieBuilder repo so the admin UI stays in sync.
