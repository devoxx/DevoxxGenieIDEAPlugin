# DevoxxGenie Documentation Website

This folder contains a [Docusaurus](https://docusaurus.io/) website that provides comprehensive documentation for the DevoxxGenie plugin.

## Running the Documentation Site

### Prerequisites

- Node.js version 18 or higher
- npm (comes with Node.js)

### On Unix/Linux/macOS

1. Make the install script executable:
   ```bash
   chmod +x install.sh
   ```

2. Run the install script:
   ```bash
   ./install.sh
   ```

### On Windows

1. Run the install script:
   ```cmd
   install.bat
   ```

Alternatively, you can manually install and run the site:

```bash
# Install dependencies
npm install

# Start development server
npm start
```

## Building for Production

To build the website for production deployment:

```bash
npm run build
```

This will generate static content in the `build` directory that can be served by any static hosting service.

## Deploying to GitHub Pages

The documentation can be deployed to GitHub Pages with:

```bash
GIT_USER=<Your GitHub username> npm run deploy
```

## Documentation Structure

- `docs/`: Contains all documentation markdown files
- `blog/`: Contains blog posts
- `static/`: Contains static assets like images
- `src/`: Contains React components and custom pages

## Contributing to Documentation

1. Add or modify content in the appropriate folders
2. Run the development server to preview changes
3. Submit a pull request with your changes

## Best Practices

- Keep documentation up to date with the latest plugin features
- Include screenshots and diagrams where helpful
- Provide clear step-by-step instructions
- Follow the existing organization and style
