# Documentation Utility Scripts

This directory contains utility scripts for maintaining the DevoxxGenie documentation website.

## Available Scripts

### Metadata Update Script

The `update-metadata.js` script automatically adds or updates front matter metadata in Markdown files to ensure consistency across the documentation. This improves SEO and makes the documentation more discoverable.

#### Usage

```bash
node update-metadata.js markdown-files.txt
```

Where `markdown-files.txt` is a file containing a list of paths to Markdown files, one per line.

#### What the Script Does

1. Reads each Markdown file in the list
2. Extracts or generates:
   - Sidebar position (preserves existing if available)
   - Page title based on filename
   - Description from the first paragraph of content
   - Keywords based on page path and headings
3. Creates or updates the front matter metadata
4. Writes the updated content back to the file

#### Generated Metadata

The script generates the following front matter:

```yaml
---
sidebar_position: [position]
title: [Page Name] - DevoxxGenie Documentation
description: [Auto-generated description]
keywords: [devoxxgenie, intellij plugin, keyword1, keyword2, ...]
image: /img/devoxxgenie-social-card.jpg
---
```

## Maintaining Consistent Metadata

When creating new documentation pages, please include all metadata fields:

- `sidebar_position` - Controls order in navigation
- `title` - Page title (include "DevoxxGenie" for SEO)
- `description` - 1-2 sentence summary (120-160 chars)
- `keywords` - Relevant terms for search engines
- `image` - Social card image path

See the existing pages for examples of proper metadata formatting.
