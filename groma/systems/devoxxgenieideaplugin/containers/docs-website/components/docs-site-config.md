---
type: C4 Component
title: Site configuration and navigation
status: stable
groma:
  id: docs-site-config
  parent: docs-website
  code:
    - scanner: javascript
      file: docusaurus/docusaurus.config.js
    - scanner: javascript
      file: docusaurus/sidebars.js
  technology: Docusaurus config
description: Docusaurus configuration, plugins, blog and sidebars
---

Defines the site URL, theme, navbar, docs sidebars, blog preset (which produces the RSS feed the plugin reads), client modules and the custom plugins (search, stats, schema.org). Static assets such as `static/api/models.json` are published alongside.
