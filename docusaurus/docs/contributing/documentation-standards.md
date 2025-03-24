---
sidebar_position: 4
title: Documentation Standards - DevoxxGenie Documentation
description: Guidelines for creating and maintaining consistent, high-quality documentation for the DevoxxGenie project, including metadata, formatting, and best practices.
keywords: [devoxxgenie, intellij plugin, documentation, standards, guidelines, metadata, seo]
image: /img/devoxxgenie-social-card.jpg
---

# Documentation Standards

This guide outlines the standards and best practices for DevoxxGenie documentation to ensure consistency, clarity, and discoverability of our content.

## Metadata Requirements

Every documentation page must include proper front matter metadata at the top of the Markdown file:

```yaml
---
sidebar_position: 2
title: Page Title - DevoxxGenie Documentation
description: A concise 1-2 sentence description of what this page covers (120-160 characters ideal).
keywords: [devoxxgenie, intellij plugin, key1, key2, key3]
image: /img/devoxxgenie-social-card.jpg
---
```

### Metadata Fields Explained

1. **sidebar_position**: Controls the order of pages in the sidebar navigation
2. **title**: Page title that appears in browser tabs and search results (include "DevoxxGenie" for SEO)
3. **description**: A concise summary that appears in search results (120-160 characters is ideal)
4. **keywords**: Relevant terms for search engines, in order of importance
5. **image**: Social media sharing image (consistent across the site)

## Document Structure

### Headings

- Use a single `# Heading` at the top of each page (matches the title)
- Structure content with `## Second-level` and `### Third-level` headings
- Maintain a logical hierarchy (don't skip levels)
- Keep headings concise and descriptive

### Content Guidelines

- Start with a brief introduction explaining the purpose of the page
- Use short paragraphs (3-5 sentences max)
- Include code examples where appropriate
- Use bulleted lists for features, steps, or options
- Use numbered lists for sequential procedures
- Include relevant screenshots with descriptive alt text

## Writing Style

- Use clear, concise language
- Address the reader directly ("you" instead of "the user")
- Use present tense ("Click the button" not "You should click the button")
- Be consistent with terminology (e.g., "LLM" vs "language model")
- Spell out acronyms on first use (e.g., "Large Language Model (LLM)")
- Use sentence case for headings (capitalize first word and proper nouns only)

## Code Examples

- Use syntax highlighting for code blocks:
  ```java
  public class Example {
      // Code here
  }
  ```
- Provide explanations before or after code blocks
- Keep examples simple and focused
- Include comments for complex sections

## Images

- Use descriptive filenames (e.g., `git-diff-viewer.png` not `image1.png`)
- Provide descriptive alt text for accessibility
- Optimize images for web (compression, appropriate dimensions)
- Use consistent styling for screenshots (size, annotations)
- Place in the `/static/img/` directory

## Links

- Use descriptive link text ("See [Installation Guide](../getting-started/installation.md)" not "Click [here](../getting-started/installation.md)")
- Check for broken links before committing changes
- Use relative paths for internal links
- Specify whether external links open in a new tab

## Maintaining Documentation

- Review existing documentation regularly
- Update content to reflect the latest features
- Fix broken links and outdated information
- Run the metadata update script periodically:
  ```bash
  npm run update-metadata
  ```

## Automation Tools

We provide a script to help maintain consistent metadata across documentation:

1. The `scripts/update-metadata.js` can automatically update front matter
2. Run using `npm run update-metadata` 
3. See `scripts/README.md` for more details

## SEO Best Practices

- Include the term "DevoxxGenie" in titles and headings
- Use descriptive URLs (slugs) that include key terms
- Keep descriptions focused on user value
- Include relevant keywords naturally in content
- Use headings to structure content for readability

By following these standards, we ensure the DevoxxGenie documentation remains high-quality, consistent, and useful for our users.
