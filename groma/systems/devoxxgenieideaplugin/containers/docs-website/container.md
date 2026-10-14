---
type: C4 Container
title: Documentation website
status: stable
groma:
  id: docs-website
  parent: devoxxgenieideaplugin
  technology: Docusaurus, React, Node.js
description: Docusaurus site published at genie.devoxx.com
---

Static documentation, landing page and blog for DevoxxGenie, built with Docusaurus from the `docusaurus/` directory and deployed by GitHub Actions on a daily schedule. Besides human readers, the plugin reads two of its published files: `/api/models.json` (model catalogue with context windows and prices) and the blog RSS feed shown on the welcome screen.
