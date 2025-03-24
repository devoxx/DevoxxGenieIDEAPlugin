// Create placeholder images for the documentation
const fs = require('fs');
const path = require('path');

const imgDir = path.join(__dirname, 'static', 'img');

// Create a basic SVG placeholder
const createPlaceholderSvg = (filename) => {
  const name = path.basename(filename, path.extname(filename));
  const extension = path.extname(filename).slice(1);
  
  const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 200">
    <rect width="400" height="200" fill="#e0e0e0"/>
    <text x="200" y="100" font-family="Arial" font-size="18" text-anchor="middle" dominant-baseline="middle" fill="#333">${name}</text>
    <text x="200" y="130" font-family="Arial" font-size="14" text-anchor="middle" dominant-baseline="middle" fill="#666">(${extension} placeholder)</text>
  </svg>`;
  
  fs.writeFileSync(filename, svgContent);
  console.log(`Created placeholder: ${filename}`);
}

// List of all image names mentioned in the documentation
const imageNames = [
  'logo.svg',
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
];

// Ensure img directory exists
if (!fs.existsSync(imgDir)) {
  fs.mkdirSync(imgDir, { recursive: true });
  console.log(`Created directory: ${imgDir}`);
}

// Create placeholder image for each image name if it doesn't exist
for (const img of imageNames) {
  const imgPath = path.join(imgDir, img);
  if (!fs.existsSync(imgPath)) {
    createPlaceholderSvg(imgPath);
  }
}

console.log('All placeholder images created in', imgDir);
