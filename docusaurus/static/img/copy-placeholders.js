// Simple script to copy placeholder SVGs to all required image locations
const fs = require('fs');
const path = require('path');

const imgDir = path.join(__dirname);
const placeholderSvg = path.join(imgDir, 'devoxxgenie-placeholder.svg');

// List of all image names mentioned in the documentation
const imageNames = [
  'logo.png',
  'installation-marketplace.png',
  'ollama-setup.png',
  'openai-setup.png',
  'provider-selection.png',
  'custom-openai-settings.png',
  'settings-llm-providers.png',
  'prompts-settings.png',
  'token-cost-settings.png',
  'token-calculator.png',
  'devoxxgenie-md-settings.png',
  'chat-interface.png',
  'rag-setup.png',
  'rag-references.png',
  'mcp-settings.png',
  'mcp-logs.png',
  'dnd-images.png',
  'git-diff-settings.png',
  'git-diff-viewer.png',
  'add-project.png',
  'project-scanner-settings.png',
  'web-search-settings.png',
  'chat-memory-settings.png',
  'devoxxgenie-toolwindow.png',
  'devoxxgenie-demo.gif',
  'rag-feature.png',
  'devoxxgenie-social-card.jpg',
  'favicon.ico',
];

// Create placeholder image for each image name if it doesn't exist
for (const img of imageNames) {
  const imgPath = path.join(imgDir, img);
  if (!fs.existsSync(imgPath)) {
    console.log(`Creating placeholder for ${img}`);
    fs.copyFileSync(placeholderSvg, imgPath);
  }
}

console.log('Placeholder images created in', imgDir);
