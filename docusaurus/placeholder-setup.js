/**
 * This is a simpler placeholder generator script that doesn't require Canvas.
 * It creates basic colored rectangles with text for all needed image placeholders.
 */

const fs = require('fs');
const path = require('path');

// The directory where images will be stored
const imgDir = path.join(__dirname, 'static', 'img');

// Create a directory if it doesn't exist
if (!fs.existsSync(imgDir)) {
  fs.mkdirSync(imgDir, { recursive: true });
  console.log(`Created directory: ${imgDir}`);
}

// Create a basic PNG placeholder using base64
const createPlaceholderPNG = (filePath, title) => {
  // This is a tiny transparent PNG generated with Node.js
  // It's a 1x1 transparent pixel that won't cause errors
  const pngData = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=', 'base64');
  fs.writeFileSync(filePath, pngData);
  console.log(`Created placeholder PNG: ${filePath}`);
};

// Create a simple SVG placeholder
const createPlaceholderSVG = (filePath, title) => {
  const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="450" viewBox="0 0 800 450">
    <rect width="800" height="450" fill="#1565c0"/>
    <text x="400" y="225" font-family="Arial" font-size="30" text-anchor="middle" fill="white">${title}</text>
    <text x="400" y="260" font-family="Arial" font-size="18" text-anchor="middle" fill="white">Placeholder Image</text>
  </svg>`;
  fs.writeFileSync(filePath, svgContent);
  console.log(`Created placeholder SVG: ${filePath}`);
};

// Create a simple ICO placeholder
const createPlaceholderICO = (filePath) => {
  // This is a very minimal ICO file (1x1 transparent pixel)
  const icoBuffer = Buffer.from([
    0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x01, 
    0x00, 0x00, 0x01, 0x00, 0x18, 0x00, 0x0E, 0x00, 
    0x00, 0x00, 0x16, 0x00, 0x00, 0x00, 0x28, 0x00, 
    0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 
    0x00, 0x00, 0x01, 0x00, 0x18, 0x00, 0x00, 0x00, 
    0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
    0x00, 0x00
  ]);
  fs.writeFileSync(filePath, icoBuffer);
  console.log(`Created placeholder ICO: ${filePath}`);
};

// List of all image names mentioned in the documentation
const imageNames = [
  'logo.svg',
  'favicon.ico',
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
  'prompt_flow.png',
];

// Create placeholder for each image name if it doesn't exist
for (const img of imageNames) {
  const imgPath = path.join(imgDir, img);
  
  // Don't overwrite existing images
  if (!fs.existsSync(imgPath)) {
    const title = img.replace(/[-_\.]/g, ' ').replace(/\.(png|jpg|gif|svg|ico)$/, '');
    
    if (img.endsWith('.svg')) {
      createPlaceholderSVG(imgPath, title);
    }
    else if (img.endsWith('.ico')) {
      createPlaceholderICO(imgPath);
    }
    else {
      // For PNG, JPG, GIF create a PNG placeholder
      createPlaceholderPNG(imgPath, title);
    }
  }
}

console.log('All placeholder images created in', imgDir);
console.log('\nNow you can run: npm run start');
