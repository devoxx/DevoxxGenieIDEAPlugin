---
id: TASK-262
title: Refresh token cost and context-window documentation
status: Done
assignee:
  - '@codex'
created_date: '2026-11-02T13:58:00Z'
updated_date: '2026-11-02T14:02:00Z'
labels: []
dependencies: []
references:
  - https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1367
modified_files:
  - docusaurus/docs/configuration/token-cost.md
priority: medium
type: docs
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The live token-cost guide still uses old model examples and describes editable per-thousand pricing despite the plugin exposing a read-only per-million catalogue. Users need dated, authoritative API examples and accurate guidance for estimates and model limits. Follow-up to the documentation audit in PR #1367.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Replace obsolete model-window examples with dated examples linked to official provider specifications and distinguish context from output limits.
- [x] #2 Correct pricing units, read-only catalogue behavior, provider overrides and unsupported controls to match the plugin.
- [x] #3 Docusaurus builds and rendered page, anchors and source-backed documentation checks pass; leave changes uncommitted.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Verify official GPT-5.5/GPT-5.4, Claude Sonnet 5.5 and Gemini 3.8 Flash API limits and inspect current cost/context/token calculation implementation.
2. Refresh token-cost.md with dated sourced examples, explicit context-versus-input/output labels, read-only per-million catalogue behavior, provider override guidance, and realistic estimate limitations. Remove nonexistent scan and calculation controls.
3. Build Docusaurus and verify rendered table, links/anchors, source-backed units/settings assertions and whitespace. Do not commit or deploy.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Verified live page had legacy GPT-4o/GPT-3.5 Turbo/Claude3.5/Gemini1.5 examples. Official model specs opened and checked on 2026-11-02: GPT-5.5/5.4 context 1050000/output128000; Sonnet5.5 context1000000/output128000; Gemini3.8Flash and3.1ProPreview input1048576/output65536. Kept provider/input distinctions and dated availability/catalogue caveats.

Source inspection confirmed read-only cost table, USD/1M calculation, background refresh with cached/hardcoded fallback, custom provider overrides and no general tokenizer/scanner-limit controls. Corrected those surrounding claims, removed blanket disabled-model assertions and added hypothetical cost formula example.

Docusaurus production build passed. Rendered table contains six example rows including local-server row; existing window-size-by-provider and configuring-provider-costs anchors preserved; 23 internal documentation links/anchors resolve. Source-backed checks match 4 shipped catalogue limits, table editability, cost units and $0.03 hypothetical arithmetic. git diff --check passed.

Created docs/token-cost-refresh from fetched origin/master after worktree moved to stale master; no plugin source/catalogue edits, commit, push or deployment. Unrelated experimental plan preserved.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Refreshed token-cost reference page with dated official API limits for GPT-5.5/5.4, Claude Sonnet5.5 and Gemini3.8Flash/3.1ProPreview, explicit context/input/output distinctions and provider links. Corrected the read-only USD-per-million catalogue guidance, refresh/fallback behavior, override instructions, scanner claims and estimation caveats. Production build, source-backed assertions, rendered table/anchor checks and 23 internal links passed. Uncommitted on docs/token-cost-refresh.
<!-- SECTION:FINAL_SUMMARY:END -->
