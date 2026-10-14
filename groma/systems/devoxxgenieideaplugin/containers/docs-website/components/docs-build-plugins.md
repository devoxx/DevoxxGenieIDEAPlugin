---
type: C4 Component
title: Build-time data plugins
status: stable
groma:
  id: docs-build-plugins
  parent: docs-website
  code:
    - scanner: javascript
      file: docusaurus/src/plugins/genie-stats/index.js
    - scanner: javascript
      file: docusaurus/src/plugins/schema-plugin/index.js
  technology: Docusaurus plugins (Node)
description: Bakes live project stats and schema.org metadata into the static HTML
---

`genie-stats` fetches Marketplace downloads, active users and GitHub stars at build time and exposes them as global data; `schema-plugin` injects JSON-LD schema tags.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [docusaurus/src/plugins/genie-stats/index.js](../../../../../../docusaurus/src/plugins/genie-stats/index.js) | [project-stats-apis](../../../../../externals/project-stats-apis.md) | Fetches project stats | HTTPS at build time |
