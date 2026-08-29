---
id: TASK-207
title: >-
  GenieBuilder: surface DevoxxGenie prompt_executed events in analytics
  dashboard
status: Done
assignee: []
created_date: '2026-04-13 09:34'
updated_date: '2026-04-13 11:00'
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
- [x] #8 CSV export honors the active app filter and includes the new breakdown columns
- [x] #9 Unit tests cover the new query branches in functions/src/analytics.ts (allowlist, app filter, prompt_executed breakdowns)
- [ ] #10 End-to-end validation: a synthetic prompt_executed event with app_name=devoxxgenie-intellij sent through the Cloudflare worker appears in the DevoxxGenie view of the dashboard within GA4's normal latency window
- [x] #11 Cross-repo work is implemented in /Users/stephan/IdeaProjects/GenieBuilder on a feature branch following GenieBuilder's branching conventions, not in DevoxxGenieIDEAPlugin
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented in cross-repo PR https://github.com/devoxx/GenieBuilder/pull/336 on branch `feature/task-207-devoxxgenie-prompt-executed-analytics` in /Users/stephan/IdeaProjects/GenieBuilder.

**Cloud Functions (`functions/src/`)**
- `analytics.ts` — added `prompt_executed` to `TRACKED_EVENTS`, exported `AppName` type, extended `fetchAnalyticsReport(start, end, appName?)` with a `withAppFilter` helper that wraps each query in an `andGroup` filtering on `customEvent:app_name` when `appName` is set. Omitting `appName` is byte-identical to before (backwards compatible). Added two new parallel GA4 queries for prompt-executed provider/model breakdowns. `AnalyticsReport` now carries `appName`, `promptProviderBreakdown`, `promptModelBreakdown`.
- `index.ts` — `analyticsReport` handler reads `req.query.appName`, validates against the allowlist (`devoxxgenie-intellij`, `geniebuilder-electron`), 400s on bad input, and forwards.
- `analytics.test.ts` — bumped mocked parallel call count 9 → 11 across all tests; added 5 new tests (allowlist, prompt provider/model parsing, app filter present/absent).

**Web admin (`web-admin/src/app/`)**
- `analytics.model.ts` — added `AppFilter` union, `APP_FILTER_LABELS`, `promptProviderBreakdown`/`promptModelBreakdown` fields, `prompt_executed` event label, and `prompt_executed` in the `LLM Usage` category.
- `analytics.service.ts` — new `selectedApp` signal + `setApp()`, appName forwarded in `loadReport()` URL, `topPromptProviders`/`topPromptModels` computed signals, CSV export scoped per app with intent and actual breakdown sections (with proper escaping).
- `analytics-overview.ts` — `mat-button-toggle-group` app selector at the top of the toolbar, provider and model breakdowns each render two side-by-side cards labeled "Models selected (intent)" vs "Prompts dispatched (actual)" with tooltips and a distinct fill colour.
- Spec files updated and 4 new component tests added (selector renders all 3 options, prompt-executed breakdowns render).

**Verification**: functions `npm test` 51 pass / 3 pre-existing failures unchanged from main; functions `npm run build` clean; web-admin `ng test` 101 pass / 3 pre-existing failures unchanged; web-admin `ng build` succeeds with same budget warnings as main; `tsc --noEmit` clean.

**AC #10 (E2E synthetic event validation)** is post-deploy and will be confirmed once Cloud Functions are deployed and the existing `prompt_executed` event (already in GA4 from TASK-206) surfaces in the DevoxxGenie tab's "Prompts dispatched (actual)" card after the GA4 latency window.
<!-- SECTION:FINAL_SUMMARY:END -->
