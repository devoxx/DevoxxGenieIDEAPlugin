---
id: TASK-191
title: Add DevoxxGenie Workshop promotion to Docusaurus welcome page
status: Done
assignee: []
created_date: '2026-03-07 15:11'
labels:
  - docusaurus
  - website
  - marketing
dependencies: []
references:
  - 'docusaurus/src/pages/index.js:74-79'
  - 'https://stephanjanssen.be'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a "DevoxxGenie Workshop" link on the Docusaurus welcome page banner (line 74-79 of `docusaurus/src/pages/index.js`), next to the existing "Agentic Engineering Workshop" link.

Currently the banner reads:
> Learn hands-on Agentic Engineering with the founder of DevoxxGenie — **Agentic Engineering Workshop**

It should be updated to also promote the DevoxxGenie Workshop, linking to https://stephanjanssen.be (with appropriate UTM parameters).

Both workshops should be visually consistent (same styling, same banner area).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 DevoxxGenie Workshop link added to the banner area on the welcome page
- [x] #2 Link points to https://stephanjanssen.be with utm_source=DevoxxGenie parameter
- [x] #3 DevoxxGenie Workshop link is visually consistent with the existing Agentic Engineering Workshop link
- [x] #4 Both workshop links are clearly visible and distinguishable from each other
- [x] #5 Page renders correctly on desktop and mobile
<!-- AC:END -->
