---
id: TASK-264
title: Retire unused success stories page and promotion links
status: Done
assignee:
  - '@codex'
created_date: '2026-11-02T14:11:00Z'
updated_date: '2026-11-02T14:13:00Z'
labels: []
dependencies: []
modified_files:
  - docusaurus/src/pages/success-stories.js
  - docusaurus/docusaurus.config.js
  - README.md
  - docusaurus/blog/2026-08-10-share-your-success-story.md
priority: low
type: docs
ordinal: 9000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The user reports the success-stories page has never been used and requests its removal. Remove the unused form route and entry points without leaving broken links; preserve the historical announcement with a retirement notice and a supported feedback destination.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The success-stories page route and footer promotion are removed.
- [x] #2 README and historical announcement contain no links to the retired route or active calls to use its form.
- [x] #3 Docusaurus builds and generated routes, sitemap and links no longer advertise the retired page; preserve unrelated work.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Inspect page, footer, README and announcement references. Confirm the existing route is generated before removal.
2. Delete the unused page, remove footer/README promotion, and retain the historical blog with a retirement notice and links to GitHub Discussions/issues instead of the retired form.
3. Build Docusaurus fresh and verify no generated route/sitemap/navigation links or form embed remain. Preserve unrelated pricing changes and experimental plan; leave uncommitted.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Confirmed baseline generated success-stories.html existed and the removal assertion failed before changes. Deleted source route and removed footer/README promotions. Kept historical blog slug with dated retirement notice, no active form call-to-action, and Discussions/issues links.

Production Docusaurus build passed. Removal checks now pass: no generated success-stories page or sitemap URL; no retired-route links or retired Google Form embed across all 196 generated HTML pages; historical announcement still renders. git diff --check passed. Hash checks confirm preexisting pricing edits and unrelated plan unchanged.

No commit, push or deployment. The external Google Form was not modified or deleted; only website route/embed and promotion links were removed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Retired the unused success-stories route, embedded form and footer/README promotion. Updated the historical announcement to mark retirement and direct feedback to GitHub. Production build and generated route/sitemap/link/embed checks passed across 196 HTML pages. Pricing edits and unrelated plan preserved; changes remain uncommitted.
<!-- SECTION:FINAL_SUMMARY:END -->
