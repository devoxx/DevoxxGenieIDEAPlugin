---
id: TASK-209
title: 'Track feature enablement & usage analytics (RAG, Agent, MCP, Web Search, ...)'
status: In Progress
assignee: []
created_date: '2026-04-13 13:13'
updated_date: '2026-04-13 14:15'
labels:
  - analytics
  - telemetry
  - feature-tracking
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

Today `AnalyticsService` only emits `prompt_executed` and `model_selected` with `provider_id` / `model_name` (src/main/java/com/devoxx/genie/service/analytics/AnalyticsService.java:40-79). We have no visibility into **which DevoxxGenie features developers enable vs actually use**, so product decisions about RAG, Agent, MCP, Web Search, Semantic Search, etc. are flying blind.

The GenieBuilder admin panel (sibling repo `../GenieBuilder`) should be able to answer: _"What % of installs have RAG configured? How often is Agent mode actually invoked? How many MCP servers does the median install have configured?"_ — **without ever learning server names, URLs, commands, paths, project names, or custom prompt names.**

## Goal

Extend the existing consent-gated, anonymous, fire-and-forget GA4 pipeline (task-206 / task-208 guarantees preserved) with:

1. **Feature enablement snapshot** — emitted **once per IDE session** (not per project), capturing which optional features are toggled ON and coarse counts.
2. **Feature usage events** — emitted when a feature is actually exercised during a prompt.

## Enablement vs Usage — source-of-truth mapping

Enablement (settings flag) and usage (per-prompt activation) are **distinct signals** and both must be captured:

| Feature          | Enablement (configured) | Usage (activated for this prompt) |
|------------------|-------------------------|------------------------------------|
| RAG              | `DevoxxGenieStateService.ragEnabled` | `ragActivated` (chat panel toggle; see `ChatMessageContextUtil`) |
| Web Search       | `googleSearchEnabled` / `tavilySearchEnabled` | `webSearchActivated` |
| Semantic Search  | RAG index present + ChromaDB reachable | `SemanticSearchService` invocation on the prompt |
| Agent Mode       | `agentEnabled` | `AgentLoopTracker` ran ≥1 tool call |
| MCP              | `mcpEnabled` + ≥1 configured server | MCP tool actually invoked during prompt |
| Streaming        | `streamMode` | (same — not a per-prompt toggle) |
| Project context  | n/a | "full project" or "selected files" attached to the prompt |
| DEVOXXGENIE.md   | file exists at project root | auto-injected into system prompt |
| Custom Prompts   | count of user-defined prompts | a built-in OR user-defined prompt command was used (boolean only) |
| Chat memory      | `chatMemorySize` bucket | — |

**Dropped from original draft:** Git Diff context — no `GitMergeService` / git-diff prompt feature exists in this repo. (VCS diff exists only inside Event Automation via `VcsCommitListener` and is out of scope here.) Event Automation and Spec-Driven Dev are deferred until those features ship.

**Provider type:** emit `provider_type` = `local|cloud` derived from `ModelProvider` enum (src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java:10). Do **not** rely on GenieBuilder to maintain a provider map.

## Event schema (resolves GA4 25-param limit)

GA4 caps events at 25 params. The boolean+counts snapshot plus common params (`app_name`, `app_version`, `ide_version`, `session_id`, `engagement_time_msec`) would exceed this. **Decision:** emit **one `feature_enabled` event per enabled feature** with a shared shape, and a single `feature_used` event per usage.

```
feature_enabled
  params: feature_id (enum), app_name, app_version, ide_version, session_id, engagement_time_msec
feature_used
  params: feature_id (enum), provider_type (local|cloud|none), tool_call_count (int bucket),
          app_name, app_version, ide_version, session_id, engagement_time_msec
feature_counts (one-shot, per session)
  params: mcp_server_count, custom_prompt_count, chat_memory_bucket,
          app_name, app_version, ide_version, session_id, engagement_time_msec
```

**`feature_id` is a closed allowlist enum:**
`rag`, `semantic_search`, `web_search_google`, `web_search_tavily`, `agent`, `mcp`, `streaming`, `project_context_full`, `project_context_selected`, `devoxxgenie_md`, `custom_prompt`.

**Counts are bucketed** (e.g., `0`, `1`, `2-5`, `6-10`, `11+`) — never raw counts that could fingerprint an install.

## Privacy — hard rules

The new events MUST NEVER include:
- MCP server names, commands, URLs, env vars, or tool names (src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java:834 contains all of these locally — none of it leaves the IDE).
- User-defined custom prompt names or bodies (src/main/java/com/devoxx/genie/service/prompt/command/CustomPromptCommand.java:78). Only `custom_prompt_used=true` / bucketed count.
- File paths, project names, file contents, prompt text, response text, API keys, host names, user identity.

Enforcement:
- A **closed param allowlist** per event in `AnalyticsService`. Unknown keys are dropped with a debug log and a unit-tested rejection path.
- All new string params must be enum-typed (e.g., `feature_id`, `provider_type`). No free-form strings.
- Unit test asserts that passing a path-shaped or URL-shaped value for any param causes the event to be dropped.

## Emission points

- **`feature_enabled` + `feature_counts`**: emitted from a new `AnalyticsSessionSnapshotService` (APP-level `@Service`) guarded by an `AtomicBoolean snapshotSent` keyed on `sessionId`. `PostStartupActivity` (src/main/java/com/devoxx/genie/service/PostStartupActivity.java:62) calls `snapshotIfNeeded()` — which is a no-op on the 2nd+ project open in the same IDE session. Re-armed on settings change via `GeneralSettingsConfigurable#apply`.
- **`feature_used`**:
  - Agent: `AgentLoopTracker` end-of-run hook, with bucketed `tool_call_count`.
  - MCP: a new instrumenting **tool-provider wrapper** sitting above `FilteredMcpToolProvider` (src/main/java/com/devoxx/genie/service/mcp/FilteredMcpToolProvider.java:51) and composing with `ApprovalRequiredToolProvider` (src/main/java/com/devoxx/genie/service/mcp/ApprovalRequiredToolProvider.java:49). This covers both standalone MCP and MCP-inside-agent. Do **not** instrument `MCPExecutionService` — it is not the execution boundary.
  - Web Search: `WebSearchPromptExecutionService`.
  - Semantic Search: `SemanticSearchService`.
  - RAG activation / project context / DEVOXXGENIE.md / custom prompts: `PromptExecutionService` / `MessageCreationService` at message assembly time, reading `ChatMessageContext`.

## Consent & disclosure surfaces

Every user-visible analytics disclosure must stay in sync before rollout:
- `src/main/java/com/devoxx/genie/service/analytics/AnalyticsConsentNotifier.java:27` (first-run notice)
- `src/main/java/com/devoxx/genie/ui/settings/general/GeneralSettingsComponent.java:28` (settings disclosure block)
- `src/main/resources/META-INF/plugin.xml:47` (marketplace description)

## GenieBuilder admin UI (follow-up, out of scope here)

File a sibling-repo task in `../GenieBuilder` to add a "Feature Usage" panel:
- % of installs with each `feature_id` seen in `feature_enabled`
- Daily/weekly `feature_used` trend per `feature_id`
- `mcp_server_count` / `custom_prompt_count` bucket histograms
- Filter by `app_version` / `ide_version` / `provider_type`

## References

- src/main/java/com/devoxx/genie/service/analytics/AnalyticsService.java — existing pipeline
- src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java:71 — `ragEnabled` source of truth
- src/main/java/com/devoxx/genie/util/ChatMessageContextUtil.java:44 — `ragActivated` per-prompt signal
- src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java:10 — local/cloud/optional classification
- src/main/java/com/devoxx/genie/service/mcp/FilteredMcpToolProvider.java:51 — MCP tool-provider boundary
- src/main/java/com/devoxx/genie/service/mcp/ApprovalRequiredToolProvider.java:49 — MCP approval wrapper
- src/main/java/com/devoxx/genie/service/PostStartupActivity.java:62 — per-project startup (not per-session)
- task-206 — analytics disclosure list
- task-208 — offline hardening precedent
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `feature_enabled` is emitted at most once per IDE session (not per opened project), guarded by an app-level `AtomicBoolean` keyed on the existing `sessionId`; unit test opens two projects and asserts a single emission
- [x] #2 `feature_enabled` / `feature_used` / `feature_counts` schemas match the closed allowlist in the description; unknown params are dropped and the drop is unit-tested
- [x] #3 `feature_id` is a closed enum (`rag`, `semantic_search`, `web_search_google`, `web_search_tavily`, `agent`, `mcp`, `streaming`, `project_context_full`, `project_context_selected`, `devoxxgenie_md`, `custom_prompt`); no free-form strings accepted
- [x] #4 Enablement and usage are captured as separate events — snapshot reflects `ragEnabled`-style settings flags, `feature_used` reflects per-prompt activation (`ragActivated`, `webSearchActivated`, actual MCP tool invocation, etc.)
- [x] #5 MCP tool invocations are counted via an instrumenting tool-provider wrapper composed with `FilteredMcpToolProvider` and `ApprovalRequiredToolProvider`, working for both standalone MCP and MCP-inside-agent; `MCPExecutionService` is NOT used as the counting point
- [x] #6 `provider_type` = `local|cloud` is derived in-plugin from `ModelProvider` enum, not delegated to GenieBuilder
- [x] #7 All counts (`mcp_server_count`, `custom_prompt_count`, `tool_call_count`, `chat_memory_bucket`) are emitted as coarse buckets, never raw integers
- [x] #8 No new event ever carries MCP server names/URLs/commands/tool names, custom prompt names/bodies, file paths, project names, file contents, prompt text, API keys, host names, or user identity — enforced by allowlist + a unit test that passes path/URL-shaped values and asserts they are rejected
- [x] #9 Git Diff context criterion is explicitly out of scope (no such feature exists in the repo); Event Automation and Spec-Driven Dev are deferred
- [x] #10 Existing consent gates (`analyticsNoticeAcknowledged`, `analyticsEnabled`) suppress all new events when off — unit tested
- [x] #11 All three disclosure surfaces are updated in lockstep: `AnalyticsConsentNotifier`, `GeneralSettingsComponent`, and `plugin.xml` marketplace description
- [ ] #12 Unit tests cover: snapshot one-shot guard, per-event allowlist rejection, consent-off suppression, offline fire-and-forget (task-208 regression), bucketing boundaries
- [x] #13 GA4 schema is documented in a shared location (e.g., `docs/analytics-schema.md`) that both DevoxxGenie and GenieBuilder reference
- [x] #14 Follow-up task filed in `../GenieBuilder` for the Feature Usage admin panel
- [x] #15 `AnalyticsService.buildPayload` is refactored into a generic `AnalyticsEventBuilder` that takes `(eventName, Map<String,String>)` and enforces a closed per-event param allowlist; existing `prompt_executed` / `model_selected` events route through it and `AnalyticsServiceTest` still passes
- [x] #16 ModelProvider.Type mapping is implemented as LOCAL→local, CLOUD→cloud, OPTIONAL→cloud; `provider_type` allowed values are strictly `local|cloud|none`; unit test covers each enum value
- [x] #17 `Buckets` utility maps raw counts to the exact bucket strings in the task plan (`0`,`1`,`2-5`,`6-10`,`11+` for most; `0`,`1-5`,`6-10`,`11-20`,`21+` for chat_memory); boundary test covers each transition
- [x] #18 `tool_call_count` is emitted as `"0"` for all `feature_used` events except `agent` and `mcp`; unit-tested
- [x] #19 `streaming` emits `feature_enabled` when `streamMode=true` in the snapshot AND `feature_used` on every prompt when `streamMode=true`
- [x] #20 Semantic Search enablement is derived from `ragEnabled` only; no ChromaDB network call is made during startup; `semantic_search` is emitted only as `feature_used` from inside `SemanticSearchService.search()`
- [x] #21 `project_context_full`, `project_context_selected`, `semantic_search`, and `devoxxgenie_md` are usage-only feature_ids (rejected if passed to `trackFeatureEnabled`)
- [x] #22 Snapshot re-arming is implemented via a central MessageBus topic `DevoxxGenieSettingsChangedTopic` subscribed by `AnalyticsSessionSnapshotService`, not per-panel `apply()` hooks; `AnalyticsConsentNotifier`'s Keep-Enabled action also triggers `snapshotIfNeeded()`
- [x] #23 Agent `feature_used` is emitted from `StreamingPromptStrategy`, `NonStreamingPromptExecutionService`, AND `SubAgentRunner` after the chat finishes (success, error, or cancellation), each reading its own `AgentLoopTracker.getCallCount()`; sub-agent events are separate and do not double-count parent runs
- [x] #24 `InstrumentedMcpToolProvider` sits in the wrapper stack as `ApprovalRequiredToolProvider → InstrumentedMcpToolProvider → FilteredMcpToolProvider → raw`; counts are incremented inside wrapped `ToolExecutor.execute()`, not inside `provideTools()`; works for both standalone-MCP and MCP-inside-agent paths
- [x] #25 `ChatMessageContext` gains three new booleans (`projectContextFullUsed`, `projectContextSelectedUsed`, `devoxxGenieMdUsed`) set at the assembly sites named in the plan; `PromptExecutionService` reads them at prompt completion to emit the corresponding `feature_used` events
- [x] #26 One `feature_used` event is emitted per activated `feature_id` per prompt (a prompt activating RAG + Web Search + Agent emits three events)
- [x] #27 Disclosure copy in `AnalyticsConsentNotifier`, `GeneralSettingsComponent`, and `plugin.xml` is updated with the exact draft text in the task plan (feature enablement + feature usage bullets)
- [x] #28 One-shot session guard is unit-tested without IntelliJ platform fixtures: instantiate `AnalyticsSessionSnapshotService`, call `snapshotIfNeeded()` twice, assert single HTTP request via recording HttpClient; then trigger re-arm and assert a second emission
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Pre-implementation decisions (resolved from review)

### Provider type mapping (ModelProvider.Type → provider_type)
- `Type.LOCAL` → `"local"` (Ollama, LMStudio, GPT4All, Jan, LLaMA.cpp, Exo, CustomOpenAI, CLIRunners, ACPRunners)
- `Type.CLOUD` → `"cloud"` (OpenAI, Anthropic, Mistral, Groq, DeepInfra, Google, OpenRouter, DeepSeek, Grok, Kimi, GLM)
- `Type.OPTIONAL` → `"cloud"` (Azure OpenAI, Bedrock — cloud-hosted enterprise endpoints)
- Absent / unknown → `"none"`

Allowed values in schema: `local | cloud | none`. No `optional` bucket — folded into `cloud`.

### Bucket definitions (all counts, coarse, never raw)

| Metric | Buckets |
|--------|---------|
| `mcp_server_count` | `0`, `1`, `2-5`, `6-10`, `11+` |
| `custom_prompt_count` | `0`, `1`, `2-5`, `6-10`, `11+` |
| `tool_call_count` | `0`, `1`, `2-5`, `6-10`, `11+` |
| `chat_memory_bucket` | `0`, `1-5`, `6-10`, `11-20`, `21+` |

A single `Buckets` utility class owns these mappings; unit test covers each boundary.

### `tool_call_count` semantics
- Only meaningful for `feature_id = agent` and `feature_id = mcp`.
- For all other `feature_used` events, emit `"0"` (keeps GA4 schema flat; dashboard logic stays predictable).

### Streaming usage rule
- `feature_enabled` for `streaming` emitted from the session snapshot when `streamMode = true`.
- `feature_used` for `streaming` emitted on **every prompt** when `streamMode = true`, alongside `prompt_executed`. Lets the admin panel compute "% of prompts streamed."

### Semantic Search enablement — simplified
- Enablement signal = `ragEnabled` only. No ChromaDB network probe on startup.
- `semantic_search` appears **only as `feature_used`**, fired from inside `SemanticSearchService.search()` when it actually runs.

### Usage-only `feature_id`s (never in `feature_enabled`)
`project_context_full`, `project_context_selected`, `semantic_search`, `devoxxgenie_md` are **usage-only**. `devoxxgenie_md` enablement is derivable from `feature_used` frequency; we don't emit a separate enablement event for it to avoid per-project noise in a per-session snapshot.

### Snapshot re-arming strategy
- **Central listener, not per-panel hooks.** `AnalyticsSessionSnapshotService` subscribes to a MessageBus topic `DevoxxGenieSettingsChangedTopic` (new, published from `GeneralSettingsConfigurable#apply` and any other settings panel that toggles tracked features). On any settings change the service clears its `AtomicBoolean sent` flag and re-sends.
- Also re-armed when the user clicks "OK, Keep Enabled" in `AnalyticsConsentNotifier` after setting `analyticsNoticeAcknowledged=true`.

### Agent orchestrator — where to sample `AgentLoopTracker.getCallCount()`
`AgentLoopTracker` is a `ToolProvider` wrapper with no end-of-run callback. The orchestrators that hold the tracker and invoke the LLM are:

- `src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java` — streaming path
- `src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java` — non-streaming path
- `src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java` — parallel sub-agents spawned by `parallel_explore`

Each orchestrator must, after the chat finishes (success, error, or cancellation), read `tracker.getCallCount()` and call `FeatureUsageTracker.agentCompleted(tracker.getCallCount(), providerType)`. Sub-agent completions emit their own `feature_used` events so the parent run's count isn't double-counted.

### MCP wrapper stack order (outer → inner)
`ApprovalRequiredToolProvider` → **`InstrumentedMcpToolProvider` (new)** → `FilteredMcpToolProvider` → raw `McpToolProvider`

Rationale:
- Sitting **below** `FilteredMcpToolProvider` means disabled-in-settings tools are never even exposed to the instrumenter, so we don't count filtered tools.
- Sitting **below** `ApprovalRequiredToolProvider` means denied-by-user tools are never executed through us, so we count actual approved executions only.
- The instrumenter counts inside the wrapped `ToolExecutor.execute()` call, **not** inside `provideTools()` (the LLM framework may call `provideTools` speculatively). One event per actual execution.

Works identically for standalone MCP mode and MCP-inside-agent mode (agent wraps the same MCP provider chain).

### New ChatMessageContext fields
Add three booleans set at message assembly time:

- `projectContextFullUsed` — set in `ChatMessageContextUtil.setWindowContext()` when full-project context is attached.
- `projectContextSelectedUsed` — set when `pendingAttachedFiles` / selected files are processed.
- `devoxxGenieMdUsed` — set in `ChatMemoryManager.buildSystemPrompt()` (or wherever `DEVOXXGENIE.md` is read into the system prompt).

`PromptExecutionService` reads these at prompt completion and emits the corresponding `feature_used` events.

### AnalyticsService payload refactor
Before adding any new event, refactor `AnalyticsService.buildPayload()` (src/main/java/com/devoxx/genie/service/analytics/AnalyticsService.java:173-193) which today hardcodes `provider_id` / `model_name`:

1. Extract payload assembly into a new `AnalyticsEventBuilder` that takes `(String eventName, Map<String,String> params)` and enforces the **closed per-event allowlist** (unknown keys dropped with debug log + counter).
2. Route existing `trackPromptExecuted` / `trackModelSelected` through the new builder (behavior-preserving, covered by existing `AnalyticsServiceTest`).
3. Add `trackFeatureEnabled(featureId)`, `trackFeatureUsed(featureId, providerType, toolCallCountBucket)`, `trackFeatureCounts(...)` as thin wrappers over the generic path.
4. All allowlist enforcement lives in one place; call sites can't accidentally leak.

### Draft disclosure copy (lockstep update across the three surfaces)

Add to the "What is sent" bullet list in `AnalyticsConsentNotifier`, `GeneralSettingsComponent`, and the `plugin.xml` marketplace description:

> - Which optional features are enabled (e.g., RAG, Agent mode, MCP, Web Search) and coarse counts such as the number of configured MCP servers or custom prompts — never server names, URLs, commands, or user-defined prompt names.
> - Which features are actually used during a prompt (e.g., RAG, Agent, MCP, Web Search, project context, custom prompts) — feature identifiers only, never prompt text or file content.

### `feature_used` event multiplicity
**One `feature_used` event per activated `feature_id` per prompt.** A prompt that activates RAG + Web Search + Agent emits three events. This is consistent with the GA4 25-param workaround (one feature per event) and keeps the dashboard aggregation trivial.

### New classes to introduce

1. `AnalyticsEventBuilder` — generic `Map → JSON` payload builder + per-event allowlist.
2. `AnalyticsSessionSnapshotService` (APP-level `@Service`) — reads `DevoxxGenieStateService`, emits `feature_enabled` per enabled feature + `feature_counts`, guarded by `AtomicBoolean sent` on the existing `sessionId`. Package-private accessor for test inspection (no full platform fixture needed).
3. `InstrumentedMcpToolProvider` — MCP tool-provider wrapper counting actual `ToolExecutor.execute()` invocations.
4. `FeatureUsageTracker` — thin static helper called from `PromptExecutionService`, `WebSearchPromptExecutionService`, `SemanticSearchService`, the agent orchestrators, and `InstrumentedMcpToolProvider`.
5. `Buckets` — count → bucket-string mapping utility with unit-tested boundaries.

### Testability of the one-shot guard
Unit test does **not** launch two IntelliJ projects. Instead it:
1. Instantiates `AnalyticsSessionSnapshotService` directly.
2. Calls `snapshotIfNeeded()` twice.
3. Asserts exactly one HTTP request captured by the recording `HttpClient` (same pattern as existing `AnalyticsServiceTest`).
4. Calls the MessageBus re-arm path and asserts a second emission fires.
<!-- SECTION:PLAN:END -->
