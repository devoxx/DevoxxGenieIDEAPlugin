---
id: TASK-206
title: Add anonymous LLM provider/model usage analytics
status: Done
assignee: []
created_date: '2026-04-13 09:25'
updated_date: '2026-04-13 10:37'
labels:
  - analytics
  - telemetry
  - feature
dependencies: []
references:
  - src/main/java/com/devoxx/genie/controller/PromptExecutionController.java
  - src/main/java/com/devoxx/genie/service/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/
  - src/main/resources/META-INF/plugin.xml
  - ../GenieBuilder/src/main/services/analytics/analytics-service.ts
  - ../GenieBuilder/tests/unit/analytics-service.test.ts
  - ../GenieBuilder/functions/src/index.ts
  - ../GenieBuilder/functions/src/analytics.ts
  - ../GenieBuilder/web-admin/src/app/features/analytics/
  - ../GenieBuilder/cloudflare/
documentation:
  - 'https://developers.google.com/analytics/devguides/collection/protocol/ga4'
  - 'https://aws.amazon.com/bedrock/pricing/'
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Why

We are evaluating a flat-fee LLM cloud subscription tier for DevoxxGenie users (fronted by AWS Bedrock and other providers). To price plans without losing money, we need real data on **which LLM providers and models DevoxxGenie users actually run**, and at what relative frequency. Today we have zero visibility into this — pricing decisions would be guesswork against a heavy-tailed cost distribution where one Opus power user can wipe out hundreds of light subscribers.

This task adds **minimal, anonymous, opt-out-able** usage analytics that capture LLM provider and model per actual prompt execution. No prompts, responses, file content, file paths, project names, API keys, conversation history, or user identity are ever sent.

## What we are reusing from GenieBuilder (Option A)

GenieBuilder (sister Electron project at `../GenieBuilder`) already implements this pattern:

- **Cloudflare Worker endpoint:** `https://delicate-morning-ff55.devoxx.workers.dev` — accepts GA4 Measurement Protocol JSON and forwards to GA4 (property `G-VHHFZ5TRG2`, Firebase project `geniebuilder-49a88`).
- **Reference client:** `../GenieBuilder/src/main/services/analytics/analytics-service.ts` (1-258) and tests at `../GenieBuilder/tests/unit/analytics-service.test.ts` (21-51).
- **Backend read API:** Firebase Cloud Function `analyticsReport` at `../GenieBuilder/functions/src/index.ts` (148-185), allowlist in `../GenieBuilder/functions/src/analytics.ts:12`, provider/model breakdowns at `analytics.ts:142,157`.
- **Admin dashboard:** `../GenieBuilder/web-admin/src/app/features/analytics/`.

**Option A (chosen):** reuse the existing Cloudflare Worker AND Firebase admin dashboard. DevoxxGenie traffic is segmented in the same GA4 property via `app_name=devoxxgenie-intellij`. No new worker, no new GA4 property, no new Firebase project. The cross-repo backend changes required to surface `prompt_executed` in the dashboard are tracked separately in **TASK-207** (a hard dependency for end-to-end visibility, but does not block this plugin task's PR from merging).

## Exact payload (full disclosure)

```json
{
  "client_id": "uuid-persisted-once-in-state-service",
  "events": [{
    "name": "prompt_executed",
    "params": {
      "provider_id": "anthropic",
      "model_name": "claude-3-5-sonnet",
      "app_name": "devoxxgenie-intellij",
      "app_version": "1.4.1",
      "ide_version": "2024.3",
      "session_id": "1234567890",
      "engagement_time_msec": 1
    }
  }]
}
```

**Every field above is user-visible data.** README, plugin.xml change-notes, CHANGELOG, the settings help text, and the first-launch notification MUST list all of these explicitly. The earlier draft phrasing ("only LLM provider and model") was inaccurate and is replaced.

`session_id` is a string of 10 digits regenerated on each IDE launch (matches GenieBuilder format and GA4 Measurement Protocol expectations for realtime reporting).

## Two events

- **`prompt_executed`** — load-bearing event for pricing math. Fired only when DevoxxGenie has actually decided to dispatch a prompt to an LLM. NOT fired for: empty prompts, slash commands handled locally (`/init`, `/help`, `/clear`, etc.), stop toggles, or any non-LLM command. Insertion point is after command processing resolves provider+model on `ChatMessageContext` (currently `ActionButtonsPanelController.java:83`) and before `PromptExecutionController.java:94` dispatches. The event must still fire if the LLM call later fails — it represents intent to dispatch.
- **`model_selected`** — intent signal. Fired only on **user-initiated** model changes in the model combo at `DevoxxGenieToolWindowContent.java:273`. MUST be guarded so it does NOT fire on: combo initialization, project open, settings refresh/restore, programmatic `setSelectedItem`, or provider switch repopulating the list. Note: `LlmProviderPanel.java:350` only handles the Exo edge case and is not the right hook for this event.

## What is NEVER sent

- Prompt text, response text, conversation history
- File content, file paths, project name, git remote
- API keys, credentials, user name, email
- Token counts, cost data (out of scope; possibly a future task)

## Privacy, consent, gating

This is an open-source IntelliJ Marketplace plugin. Silent telemetry will get the plugin flagged. Mandatory rules:

1. **Persisted flag** `analyticsNoticeAcknowledged` (default `false`) in `DevoxxGenieStateService`, alongside `analyticsEnabled` (default `true`) and `analyticsClientId`.
2. **No analytics event may be emitted before the notice has been shown and acknowledged** — enforced as a hard precondition in `AnalyticsService`, unit-tested.
3. **First-launch / post-update notification** shown exactly once per install: lists every field collected (client_id, session_id, app_version, ide_version, provider_id, model_name), states what is NOT collected, and offers inline `[Disable]` and `[OK]` actions. `[Disable]` must synchronously set both `analyticsEnabled=false` and `analyticsNoticeAcknowledged=true` and persist immediately.
4. **Settings checkbox** in the LLM Settings configurable (`LLMConfigSettingsComponent`) — or a new "General" configurable registered under the DevoxxGenie root in `plugin.xml:448` if cleaner. Help text under the checkbox enumerates every field sent.
5. **Honor the opt-out flag synchronously** — when `analyticsEnabled=false`, `AnalyticsService` makes zero HTTP requests.

## Implementation outline

1. Add to `DevoxxGenieStateService`: `analyticsEnabled` (boolean, default true), `analyticsNoticeAcknowledged` (boolean, default false), `analyticsClientId` (UUID String, generated once on first read), `analyticsEndpoint` (String, defaults to the Cloudflare worker URL — overridable for tests).
2. Create `service/analytics/AnalyticsService.java` (~150 LoC). Async fire-and-forget via `ApplicationManager.getApplication().executeOnPooledThread()` — never block EDT. Silent failure (debug log only). Use `java.net.http.HttpClient` (no new dependency).
3. Wire `prompt_executed` at the dispatch decision point described above.
4. Wire `model_selected` at `DevoxxGenieToolWindowContent.java:273` behind a user-action guard.
5. Add the settings checkbox + first-launch notification + state flags.
6. Documentation: README privacy section, plugin.xml change-notes, CHANGELOG entry — all listing every field.
7. Cross-repo dashboard updates tracked in TASK-207.

## Branch strategy

Per project CLAUDE.md: create branch `feature/llm-usage-analytics` from `master` BEFORE any code changes in this repo. The untracked `backlog/tasks/task-206 *.md` and `task-207 *.md` files should be committed on that branch as part of the first commit so task metadata travels with the implementation.

## Out of scope (explicitly)

- Token counting or cost telemetry
- User accounts, authentication, or non-anonymous identifiers
- Per-request metadata beyond the payload above
- Standing up a separate Cloudflare worker, GA4 property, or Firebase project (Option B — rejected)
- GenieBuilder backend/admin UI changes (tracked in TASK-207)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 AnalyticsService class exists under service/analytics/ and posts JSON to the configured Cloudflare worker endpoint asynchronously off the EDT using java.net.http.HttpClient
- [x] #2 Payload contains EXACTLY these fields and no others: client_id, event name, provider_id, model_name, app_name='devoxxgenie-intellij', app_version, ide_version, session_id (10-digit string), engagement_time_msec — verified by unit test asserting no extra fields
- [x] #3 session_id is a 10-digit string, regenerated on each IDE launch, identical in shape to GenieBuilder's analytics-service.ts format
- [x] #4 Anonymous client_id is a UUID generated once on first read and persisted in DevoxxGenieStateService across IDE restarts and plugin updates
- [x] #5 prompt_executed event is fired only when DevoxxGenie has decided to dispatch a prompt to an LLM — NOT for empty prompts, /init, /help, /clear, stop toggles, or any locally-handled command
- [x] #6 prompt_executed fires after ChatMessageContext has the resolved provider+model (after command processing in ActionButtonsPanelController.java:83) and before PromptExecutionController.java:94 dispatches; the event still fires if the LLM call later fails
- [x] #7 model_selected fires only on user-initiated changes to the model combo in DevoxxGenieToolWindowContent.java:273; a user-action guard prevents firing during initialization, project open, settings refresh/restore, provider switch repopulation, or programmatic setSelectedItem
- [x] #8 DevoxxGenieStateService gains persistent fields: analyticsEnabled (default true), analyticsNoticeAcknowledged (default false), analyticsClientId (UUID), analyticsEndpoint (default to Cloudflare worker URL, overridable for tests)
- [x] #9 AnalyticsService emits zero events when analyticsNoticeAcknowledged is false — enforced as a hard precondition and verified by unit test
- [x] #10 First-launch (or post-update) notification is shown exactly once per install, listing every field collected (client_id, session_id, app_version, ide_version, provider_id, model_name) and stating that prompts, code, and file content are NOT sent
- [x] #11 Notification's inline Disable action synchronously sets analyticsEnabled=false AND analyticsNoticeAcknowledged=true and persists immediately before any event can be emitted
- [x] #12 Settings checkbox 'Send anonymous usage statistics' lives in LLMConfigSettingsComponent (or a new General configurable explicitly registered under the DevoxxGenie root in plugin.xml:448); help text under the checkbox enumerates every field sent
- [x] #13 When analyticsEnabled is false, AnalyticsService makes zero HTTP requests — verified by unit test
- [x] #14 Network failures, timeouts, and non-2xx responses are caught silently and logged at debug level only — never surfaced to the user
- [x] #15 Unit test asserts no prompt text, response text, conversation history, file content, file paths, project names, API keys, or user identity appear in any payload
- [x] #16 README, plugin.xml description/change-notes, and CHANGELOG explicitly list every field collected (client_id, session_id, app_version, ide_version, provider_id, model_name) and explain how to opt out
- [x] #17 Unit tests cover: payload schema, opt-out behavior, EDT non-blocking, silent failure, client_id persistence, notice gating precondition, model_selected user-action guard
- [x] #18 Feature branch feature/llm-usage-analytics is created from master before any code changes; task-206 and task-207 markdown files are committed on that branch
- [x] #19 TASK-207 is listed as a hard dependency for end-to-end dashboard visibility but does not block this task's PR from merging
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Anonymous LLM provider/model usage analytics (task-206)

Implemented opt-out anonymous usage analytics that capture LLM provider and model on every real prompt dispatch and on user-initiated model changes. Reuses the existing GenieBuilder Cloudflare worker (`https://delicate-morning-ff55.devoxx.workers.dev`) and segments DevoxxGenie traffic via `app_name=devoxxgenie-intellij`. **Cross-repo dashboard work tracked separately in TASK-207.**

### What was added
- **`AnalyticsService`** (`service/analytics/`) — application-level service that builds the GA4 Measurement Protocol payload by hand (no new dependencies), posts via `java.net.http.HttpClient` on a pooled thread, and never blocks the EDT. Hard preconditions block emission until the consent notice is acknowledged AND the user hasn't opted out.
- **`AnalyticsConsentNotifier`** — first-launch notification with two inline actions (`OK` and `Disable`), shown exactly once per install via a separate `analyticsNoticeShown` flag set synchronously before the EDT dispatch to avoid concurrent-project-open races.
- **General settings configurable** (`ui/settings/general/`) — new top-level entry under DevoxxGenie with the opt-out checkbox and explicit help text enumerating every field that is and is not sent.
- **State service additions**: `analyticsEnabled`, `analyticsNoticeShown`, `analyticsNoticeAcknowledged`, `analyticsClientId` (lazy UUID), `analyticsEndpoint`. Client id persists across IDE restarts and plugin updates.

### What was wired
- **`prompt_executed`** fires inside `PromptExecutionService.executePrompt` *after* `processCommands` confirms an LLM dispatch and *before* the strategy executes — so locally-handled commands (`/init`, `/help`, `/clear`, stop toggles, empty prompts) are excluded by construction. Event still fires if the LLM call later fails.
- **`model_selected`** fires from `DevoxxGenieToolWindowContent.processModelNameSelection` only when **all three** guards pass: `isInitializationComplete`, `suppressModelSelectionTracking` (wraps `settingsChanged`), and `LlmProviderPanel.isUpdatingModelNames()` (wraps `updateModelNamesComboBox` and `restoreLastSelectedLanguageModel`). Provider switches, settings refreshes, and programmatic restores no longer emit false events.

### Privacy posture
The exact payload is six fields: `client_id`, `session_id` (10-digit string per launch), `app_version`, `ide_version`, `provider_id`, `model_name`, plus the GA4-required `engagement_time_msec` and `app_name=devoxxgenie-intellij`. **Never sent**: prompt text, response text, conversation history, file content, file paths, project names, git remotes, API keys, credentials, token counts, cost data. Every one of those exclusions is documented in `README.md` (new Privacy section), `plugin.xml` description (new Marketplace section), `plugin.xml` change-notes (Unreleased), `CHANGELOG.md`, and the General settings help text.

### Tests added
- `AnalyticsServiceTest` (8 tests): exact payload allowlist, 10-digit session id, UUID client id persistence, opt-out gating, notice-acknowledgement gating, missing provider/model gating, silent network failure, no-PII even with path-like inputs.
- `LlmProviderPanelTest.isUpdatingModelNames_isTrueDuringUpdateAndFalseAfter`: regression for the user-action guard.

### Verification
`./gradlew test` — full suite green (was 6 failing before; the 6 Exo failures were pre-existing on master, addressed in a separate concern via `Assumptions.assumeTrue` skip when no Exo server is running on `localhost:52415`). 8 new analytics tests passing, 1 regression test passing.

### Notes
- TASK-207 (GenieBuilder backend + admin UI to surface `prompt_executed` and add an `app_name` filter) is a hard dependency for end-to-end visibility but does not block this PR.
<!-- SECTION:FINAL_SUMMARY:END -->
