# DevoxxGenie Documentation Site

This is the official documentation site for DevoxxGenie, a fully Java-based LLM Code Assistant plugin for IntelliJ IDEA.

## Development

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Install SEO plugins
bash install-seo-deps.sh
```

### Local Development

```bash
npm start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

### Build

```bash
npm run build
```

This command generates static content into the `build` directory and can be served using any static content hosting service.

## SEO Enhancements

This documentation site has been optimized for search engines with:

- Comprehensive metadata for all main pages
- Sitemap generation
- Structured data using Schema.org
- Optimized image handling
- Proper heading hierarchies

When adding new content, please follow these SEO guidelines:

1. Add proper front matter with title, description, and keywords
2. Use descriptive image alt tags
3. Follow a logical heading structure (H1 → H2 → H3)
4. Include internal links to related content
5. Optimize content for relevant search terms

## Contributing

Feel free to contribute to this documentation by submitting pull requests or opening issues for any content improvements or corrections.

## License

This documentation is part of the DevoxxGenie project and is available under the same license.
