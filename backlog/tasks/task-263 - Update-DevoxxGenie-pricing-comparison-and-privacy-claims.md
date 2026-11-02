---
id: TASK-263
title: Update DevoxxGenie pricing comparison and privacy claims
status: Done
assignee:
  - '@codex'
created_date: '2026-11-02T14:07:00Z'
updated_date: '2026-11-02T14:09:00Z'
labels: []
dependencies: []
references:
  - https://github.com/features/copilot/plans
  - https://cursor.com/pricing
  - https://www.jetbrains.com/help/ai-assistant/licensing-and-subscriptions.html
modified_files:
  - docusaurus/docs/getting-started/why-devoxxgenie.md
priority: medium
type: docs
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The why-DevoxxGenie page compares obsolete subscription prices, hides free tiers and metered charges, and makes outdated single-provider/no-telemetry claims. The user approved updating the comparison using verified official vendor pages and current plugin behavior.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Pricing comparison states verification date, individual monthly USD scope, current free/paid plans and usage caveats with official sources.
- [x] #2 Remove unsupported competitor capability claims and correct DevoxxGenie privacy/analytics and subscription wording.
- [x] #3 Docusaurus production build and rendered links/anchors validate; preserve unrelated files and leave uncommitted.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Use the already verified current GitHub/Cursor/JetBrains pricing sources; inspect the current page and plugin privacy reference.
2. Replace the mixed subscription/capability table with a dated individual monthly USD pricing/usage comparison and source links. Correct no-telemetry/single-provider/local-only absolutes and stale provider lists without weakening DevoxxGenie's real BYOK/open-source benefits.
3. Build Docusaurus, inspect rendered comparison, validate article internal links and stale-claim checks, finalize uncommitted.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Updated the comparison using official vendor pages checked 2026-11-02: individual monthly USD plans, free tiers, GitHub AI credits, Cursor third-party model charges and Grok-only Plus, JetBrains eligibility/credits. Removed unsupported generic capability rows rather than perpetuating Limited/single-provider claims.

Corrected DevoxxGenie no-telemetry and absolute local-only/network promises; linked privacy/analytics guide and clarified provider/API/runner charges. Refreshed provider list and made feature wording factual rather than implying competing tools only chat/autocomplete.

Production Docusaurus build passed after final edit. Rendered 4-row comparison and 21 internal documentation links/anchors validated; pricing/source/caveat assertions and git diff --check passed. No plugin code edits, commit, push or deployment; unrelated experimental plan untouched.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Refreshed why-DevoxxGenie pricing comparison with verification date, monthly individual USD scope, official pricing links, current free/paid plans and usage caveats. Removed misleading competitor capability rows and corrected privacy, analytics, provider-choice and API/subscription claims. Production build, rendered comparison, 21 internal links/anchors, pricing assertions and whitespace checks passed. Changes remain uncommitted.
<!-- SECTION:FINAL_SUMMARY:END -->
