---
id: TASK-210
title: 'GenieBuilder admin UI: Feature Usage panel for DevoxxGenie analytics'
status: To Do
assignee: []
created_date: '2026-04-13 14:14'
labels:
  - analytics
  - geniebuilder
  - admin-ui
dependencies:
  - TASK-209
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Context

Task-209 extended the DevoxxGenie IntelliJ plugin's GA4 analytics pipeline with three new event types:

- `feature_enabled` — one event per enabled feature, per IDE session
- `feature_used` — one event per activated feature, per prompt
- `feature_counts` — one event per session with bucketed counts

See the shared schema doc: `../DevoxxGenieIDEAPlugin/docs/analytics-schema.md` (source of truth for both repos).

The plugin already emits these events (consent-gated, anonymous, PII-free). The GenieBuilder admin panel needs a matching "Feature Usage" dashboard so product decisions about RAG / Agent / MCP / Web Search investment can be data-driven instead of guessed.

## Goal

Surface DevoxxGenie feature analytics in a new "Feature Usage" admin panel filtered by `app_name=devoxxgenie-intellij`. The panel should answer:

1. What % of installs have each optional feature enabled?
2. Which features are actually used during prompts (per-feature trend)?
3. How many MCP servers / custom prompts does the median install have configured?
4. How does usage break down by `provider_type` (local vs cloud)?
5. How does usage trend across plugin and IDE versions?

## Proposed UI

- **Top row**: donut + stacked-bar of `feature_enabled` events grouped by `feature_id` — % of installs with each feature on, filtered to the most recent N days.
- **Middle row**: daily/weekly `feature_used` trend lines, one per `feature_id`. Toggle between count-of-events and unique-install count.
- **Bottom row**: histogram of `mcp_server_count`, `custom_prompt_count`, and `chat_memory_bucket` from `feature_counts` events.
- **Filters**: `app_version`, `ide_version`, `provider_type`, date range.
- **Drill-down table**: top recent `feature_used` events with bucketed `tool_call_count`.

Implementation is flexible — match the existing GenieBuilder admin conventions.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Feature Usage admin panel exists with enablement donut/bar, per-feature usage trend, and counts histogram sections
- [ ] #2 Panel respects `app_name=devoxxgenie-intellij` filter and supports `app_version`/`ide_version`/`provider_type`/date-range filters
- [ ] #3 All rendered values come from the closed enum allowlists documented in `docs/analytics-schema.md` — no free-form strings surfaced
- [ ] #4 Panel handles zero-data edge cases (brand-new install, unseen feature_id) gracefully
- [ ] #5 Documentation updated to reference the shared analytics schema doc in the DevoxxGenie repo

## References

- `../DevoxxGenieIDEAPlugin/docs/analytics-schema.md` — event shapes, feature_id allowlist, bucket tables, provider_type mapping
- `../DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/service/analytics/AnalyticsEventBuilder.java` — plugin-side allowlist enforcement
- DevoxxGenie task-206 — original analytics opt-in pipeline
- DevoxxGenie task-208 — offline hardening
- DevoxxGenie task-209 — feature enablement + usage events (this task is the admin-UI follow-up)

## Out of scope

- Changing the GA4 schema — schema changes must go through DevoxxGenie first and update the shared doc.
- Adding new per-install dimensions — the plugin only sends what's in the schema.
- Displaying MCP server names, URLs, commands, tool names, or user-defined prompt names — these are deliberately never emitted by the plugin.
<!-- SECTION:DESCRIPTION:END -->
<!-- AC:END -->
