---
type: C4 Component
title: Site search
status: stable
groma:
  id: docs-search
  parent: docs-website
  code:
    - scanner: javascript
      file: docusaurus/src/clientModules/algoliaResultLinks.js
    - scanner: javascript
      file: docusaurus/src/plugins/algolia-experiences/index.js
    - scanner: javascript
      file: docusaurus/src/theme/SearchBar/index.js
      symbol: SearchBar
  technology: Algolia Experiences
description: Algolia-backed search box and result links
---

A swizzled navbar SearchBar that renders the mount point, a plugin that loads Algolia Experiences, and a client module that rewrites result links to the current site.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [docusaurus/src/plugins/algolia-experiences/index.js](../../../../../../docusaurus/src/plugins/algolia-experiences/index.js) | [algolia](../../../../../externals/algolia.md) | Loads search UI | Algolia Experiences script |
