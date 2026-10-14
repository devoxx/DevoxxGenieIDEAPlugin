---
type: C4 Component
title: Newsletter sign-up
status: stable
groma:
  id: docs-newsletter
  parent: docs-website
  code:
    - scanner: javascript
      file: docusaurus/src/components/NewsletterSignup/index.js
      symbol: NewsletterSignup
    - scanner: javascript
      file: docusaurus/src/pages/newsletter-welcome.js
      symbol: NewsletterWelcome
    - scanner: javascript
      file: docusaurus/src/pages/newsletter.js
      symbol: Newsletter
  technology: React, Mailjet embed
description: Newsletter pages and embedded sign-up form
---

Newsletter landing and welcome pages plus a reusable component that embeds the Mailjet sign-up widget.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [docusaurus/src/components/NewsletterSignup/index.js](../../../../../../docusaurus/src/components/NewsletterSignup/index.js) | [mailjet](../../../../../externals/mailjet.md) | Embeds sign-up form | Mailjet embed |
