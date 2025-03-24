# DevoxxGenie Documentation

This directory contains the documentation website for the DevoxxGenie IntelliJ IDEA plugin, built with [Docusaurus](https://docusaurus.io/).

## Local Development

### Prerequisites

- [Node.js](https://nodejs.org/en/download/) version 18 or above
- [npm](https://www.npmjs.com/) (comes with Node.js)

### Installation

```bash
npm install
```

### About Image Placeholders

The documentation references many screenshots that need to be created. For development and testing, we use automatically generated placeholder images.

The `npm start` and `npm run build` commands will automatically generate these placeholders for all missing images. You don't need to run any separate commands for this.

### Local Development Server

```bash
npm run start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

### Build

```bash
npm run build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

## Adding Screenshots

Replace the placeholder images in the `static/img` directory with actual screenshots of the plugin:

1. Take screenshots of the relevant features in the plugin
2. Save them with the same filenames as the placeholders
3. Place them in the `static/img` directory

## Structure

- `docs/`: Documentation content in Markdown
- `src/`: React components for custom pages
- `static/`: Static assets like images
- `docusaurus.config.js`: Docusaurus configuration
- `sidebars.js`: Sidebar structure for documentation

## Adding Content

### Adding Documentation Pages

1. Add Markdown files to the `docs` directory
2. Update `sidebars.js` if you want to add the page to the sidebar

### Adding Images

1. Place image files in `static/img/`
2. Reference them in Markdown using relative paths: `![Alt text](/img/example.png)`

### Adding Components

If you need custom React components, add them to the `src/components/` directory.

## Deployment

The documentation site can be deployed to GitHub Pages:

```bash
GIT_USER=<Your GitHub username> npm run deploy
```

This command is a convenient way to build the website and push to the `gh-pages` branch.
