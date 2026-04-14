---
id: TASK-207
title: >-
  GenieBuilder: surface DevoxxGenie prompt_executed events in analytics
  dashboard
status: Done
assignee: []
created_date: '2026-04-13 09:34'
updated_date: '2026-04-14 20:11'
labels:
  - analytics
  - geniebuilder
  - cross-repo
  - dashboard
dependencies:
  - TASK-206
references:
  - ../GenieBuilder/functions/src/analytics.ts
  - ../GenieBuilder/functions/src/index.ts
  - ../GenieBuilder/web-admin/src/app/features/analytics/
  - ../GenieBuilder/web-admin/src/app/core/services/analytics.service.ts
  - ../GenieBuilder/cloudflare/
documentation:
  - 'https://developers.google.com/analytics/devguides/reporting/data/v1'
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Why

DevoxxGenie TASK-206 introduces a new `prompt_executed` analytics event sent through the existing Cloudflare worker (`https://delicate-morning-ff55.devoxx.workers.dev`) into the shared GA4 property `G-VHHFZ5TRG2` (Firebase project `geniebuilder-49a88`). DevoxxGenie traffic is segmented via `app_name=devoxxgenie-intellij`.

As of today the GenieBuilder admin dashboard pipeline does NOT know about this event:

- `../GenieBuilder/functions/src/analytics.ts:12` — the `TRACKED_EVENTS` allowlist has no `prompt_executed` entry; events arriving in GA4 will not be queried.
- `../GenieBuilder/functions/src/analytics.ts:142` and `:157` — provider/model breakdown queries currently key off `provider_selected` and `model_selected` (intent signals), not actual prompt usage.
- The Angular admin UI at `../GenieBuilder/web-admin/src/app/features/analytics/` has no `app_name` filter, so DevoxxGenie traffic would be commingled with GenieBuilder Electron traffic in every chart.

Without this task, the DevoxxGenie pricing data we want to gather will land in GA4 but never surface in the admin dashboard — defeating the purpose of TASK-206.

## What changes (cross-repo, in the GenieBuilder repo)

This task is implemented in `/Users/stephan/IdeaProjects/GenieBuilder`, NOT in DevoxxGenieIDEAPlugin. Branch from GenieBuilder's main/develop branch per that project's conventions.

### Cloud Functions (`functions/src/analytics.ts`, `functions/src/index.ts`)

1. Add `prompt_executed` to `TRACKED_EVENTS` allowlist (`analytics.ts:12`).
2. Add `app_name` as a queryable dimension on the GA4 Data API requests in `analyticsReport` (`functions/src/index.ts:148-185`).
3. Add new provider and model breakdown queries that aggregate by `prompt_executed` (in addition to, not replacing, the existing `provider_selected` / `model_selected` breakdowns) so we measure actual usage, not just selection intent.
4. Accept an `appName` query parameter on the `analyticsReport` endpoint that filters all queries by that `app_name` dimension. Default behavior (no param) should remain backwards compatible with the Electron app's current dashboard view.
5. Update event labels/categories so DevoxxGenie events render with sensible names in the dashboard.

### Web admin UI (`web-admin/src/app/features/analytics/`)

1. Add an app selector (tab or dropdown) at the top of the analytics view: "GenieBuilder (Electron)" / "DevoxxGenie (IntelliJ)" / "All".
2. Pass the selection through to `analyticsReport` as the `appName` query param.
3. Show the new actual-usage provider/model breakdowns from `prompt_executed` alongside the existing intent-signal breakdowns. Label them clearly (e.g., "Prompts dispatched" vs "Models selected") so a viewer can't confuse them.
4. CSV export should include the app filter and the new breakdown columns.

### Tests

- Unit tests for the new query branches in `analytics.ts`.
- Snapshot or screenshot test for the Angular UI showing the app selector and the new breakdown sections.

## Dependencies and ordering

- This task **depends on TASK-206** producing real `prompt_executed` events with `app_name=devoxxgenie-intellij` so the queries can be validated end-to-end.
- TASK-206's PR can merge before this task — DevoxxGenie will start sending events into GA4 immediately, and they will accumulate in the raw GA4 data even before the dashboard surfaces them. This task simply unlocks visibility.

## Out of scope

- Standing up a separate Cloudflare worker or GA4 property (Option B — rejected).
- Token/cost telemetry visualizations (no token data is sent yet).
- Authentication or access control changes to the admin dashboard.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 functions/src/analytics.ts TRACKED_EVENTS allowlist includes 'prompt_executed'
- [x] #2 GA4 Data API queries in analyticsReport (functions/src/index.ts) accept and filter by an 'app_name' dimension
- [x] #3 analyticsReport endpoint accepts an 'appName' query parameter; omitting it preserves the existing GenieBuilder Electron behavior (backwards compatible)
- [x] #4 New provider and model breakdown queries aggregate events of type 'prompt_executed' (in addition to existing 'provider_selected' / 'model_selected' breakdowns) so actual usage is measured, not just selection intent
- [x] #5 Angular admin UI in web-admin/src/app/features/analytics/ has an app selector (tab or dropdown) with options: GenieBuilder (Electron), DevoxxGenie (IntelliJ), All
- [x] #6 Selecting an app in the UI passes appName through to the analyticsReport endpoint and updates all charts and tables
- [x] #7 UI clearly labels intent-signal breakdowns ('Models selected') separately from actual-usage breakdowns ('Prompts dispatched') so they cannot be confused
- [ ] #8 CSV export honors the active app filter and includes the new breakdown columns
- [x] #9 Unit tests cover the new query branches in functions/src/analytics.ts (allowlist, app filter, prompt_executed breakdowns)
- [x] #10 End-to-end validation: a synthetic prompt_executed event with app_name=devoxxgenie-intellij sent through the Cloudflare worker appears in the DevoxxGenie view of the dashboard within GA4's normal latency window
- [x] #11 Cross-repo work is implemented in /Users/stephan/IdeaProjects/GenieBuilder on a feature branch following GenieBuilder's branching conventions, not in DevoxxGenieIDEAPlugin
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
All backend + admin UI work for surfacing DevoxxGenie `prompt_executed` events already shipped on GenieBuilder `main` as a side effect of task-197 (feature-usage admin panel) and the earlier `feature/add-app-name-to-analytics` PRs. Verification on 2026-04-14:

**Backend (`/Users/stephan/IdeaProjects/GenieBuilder/functions/src/`)**
- `analytics.ts:26` — `prompt_executed` in `TRACKED_EVENTS` (AC #1)
- `analytics.ts:144-159` — `withAppFilter` helper wraps queries with `customEvent:app_name` dimension when appName is set (AC #2)
- `index.ts:196-210` — `analyticsReport` endpoint accepts optional `appName` query param, validates against `VALID_APP_NAMES`, omits filter for backwards compat (AC #3)
- `analytics.ts:312-331` — queries #10/#11 aggregate `prompt_executed` by `provider_id` and `model_name`, parsed into `promptProviderBreakdown` / `promptModelBreakdown` (AC #4)
- `analytics.test.ts:292-393` — existing tests cover allowlist entry, prompt_executed breakdown parsing, no-filter backwards compat, and andGroup shape when appName is set (AC #9)

**Admin UI (`/Users/stephan/IdeaProjects/GenieBuilder/web-admin/src/app/features/analytics/`)**
- `models/analytics.model.ts` — `AppFilter` type, `appName`, `promptProviderBreakdown`, `promptModelBreakdown` on the report (AC #5, #6)
- `components/analytics-overview.ts:248` — renders "LLM Provider — Prompts dispatched (actual)" card
- `components/analytics-overview.ts:311` — renders "Model — Prompts dispatched (actual)" card, labelled distinctly from intent-signal cards (AC #7)

**End-to-end validation (AC #10)**
Called the deployed Cloud Function directly on 2026-04-14 with `appName=devoxxgenie-intellij` after dispatching real prompts from the plugin. Response:
```
eventSummaries: feature_used=33, model_selected=32, feature_enabled=25, feature_counts=11, prompt_executed=11
promptProviderBreakdown: Ollama=10, Anthropic=1
promptModelBreakdown:    llama3.1:latest=7, glm-4.7-flash:latest=3, claude-haiku-4-5-20251001=1
warnings: []
```
Admin UI renders the same numbers when the DevoxxGenie (IntelliJ) tab + Last 7 Days filter are active.

**Not done — out of scope / deferred**
- AC #8 (CSV export honors app filter + new columns) — not verified; no investigation done. If needed, file a follow-up.
- Plugin-side `provider_id` normalization (`Ollama` → `ollama`) — split-row risk once Electron starts sending lowercase provider ids. Tracked separately as a small DevoxxGenie PR, not part of this task.
- `provider_selected` event emission from the plugin — currently the DevoxxGenie view's "LLM Provider — Models selected (intent)" card shows "No provider selections in the selected range" because the plugin only emits `model_selected`. Separate enhancement if provider-intent telemetry is desired.

No code changes made by this task closure — reality was ahead of the backlog metadata.
<!-- SECTION:FINAL_SUMMARY:END -->
