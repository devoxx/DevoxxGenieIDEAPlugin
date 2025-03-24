// This Node.js script generates PNG placeholder images using Canvas
// To run, first install the dependencies:
// npm install canvas

const fs = require('fs');
const path = require('path');
const { createCanvas, registerFont } = require('canvas');

// Define the dimensions for the placeholder images
const WIDTH = 800;
const HEIGHT = 450;

// Configure basic colors
const COLORS = {
  background: '#f5f5f5',
  border: '#dddddd',
  title: '#1565c0',
  subtitle: '#666666',
  devoxxBlue: '#1565c0'
};

// Create a canvas
function createPlaceholderImage(filename) {
  const canvas = createCanvas(WIDTH, HEIGHT);
  const ctx = canvas.getContext('2d');
  const name = path.basename(filename, path.extname(filename));
  const extension = path.extname(filename).slice(1);

  // Fill background
  ctx.fillStyle = COLORS.background;
  ctx.fillRect(0, 0, WIDTH, HEIGHT);

  // Draw border
  ctx.strokeStyle = COLORS.border;
  ctx.lineWidth = 10;
  ctx.strokeRect(5, 5, WIDTH - 10, HEIGHT - 10);

  // Draw diagonal lines pattern in top left corner
  ctx.strokeStyle = '#e0e0e0';
  ctx.lineWidth = 2;
  for (let i = 0; i < 5; i++) {
    ctx.beginPath();
    ctx.moveTo(0, 50 + i * 20);
    ctx.lineTo(50 + i * 20, 0);
    ctx.stroke();
  }

  // Draw diagonal lines pattern in bottom right corner
  for (let i = 0; i < 5; i++) {
    ctx.beginPath();
    ctx.moveTo(WIDTH - 50 - i * 20, HEIGHT);
    ctx.lineTo(WIDTH, HEIGHT - 50 - i * 20);
    ctx.stroke();
  }

  // Add DevoxxGenie logo placeholder
  ctx.fillStyle = COLORS.devoxxBlue;
  ctx.beginPath();
  ctx.arc(WIDTH / 2, 120, 60, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = '#ffffff';
  ctx.beginPath();
  ctx.arc(WIDTH / 2, 120, 30, 0, Math.PI * 2);
  ctx.fill();

  // Draw title
  ctx.fillStyle = COLORS.title;
  ctx.font = 'bold 36px Arial';
  ctx.textAlign = 'center';
  ctx.fillText(formatTitle(name), WIDTH / 2, 220);

  // Draw subtitle
  ctx.fillStyle = COLORS.subtitle;
  ctx.font = '24px Arial';
  ctx.fillText('DevoxxGenie Documentation', WIDTH / 2, 270);

  // Add note about placeholder
  ctx.fillStyle = '#999999';
  ctx.font = '18px Arial';
  ctx.fillText('(Placeholder image - replace with actual screenshot)', WIDTH / 2, 310);

  // Add file extension info
  ctx.font = '16px Arial';
  ctx.fillText(extension.toUpperCase() + ' format', WIDTH / 2, 340);

  // Add bottom message
  ctx.fillStyle = COLORS.devoxxBlue;
  ctx.font = '14px Arial';
  ctx.fillText('Generated for Docusaurus documentation', WIDTH / 2, HEIGHT - 20);

  // Save the image
  const buffer = canvas.toBuffer('image/png');
  fs.writeFileSync(filename, buffer);
  console.log(`Created placeholder: ${filename}`);

  // For GIF files, we still need a static image as we can't easily generate animated GIFs
  if (extension === 'gif') {
    console.log(`Note: For ${filename}, a static PNG placeholder was created. Replace with an actual GIF.`);
  }
}

// Format the title to be more readable
function formatTitle(name) {
  // Replace hyphens and underscores with spaces
  let title = name.replace(/[-_]/g, ' ');
  
  // Capitalize first letter of each word
  title = title.split(' ').map(word => {
    return word.charAt(0).toUpperCase() + word.slice(1);
  }).join(' ');
  
  return title;
}

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

// Create placeholder image for each image name if it doesn't exist
for (const img of imageNames) {
  const imgPath = path.join(__dirname, img);
  
  // Don't overwrite existing images
  if (!fs.existsSync(imgPath)) {
    // SVG and ICO files need special handling
    if (img.endsWith('.svg')) {
      // Create a simple SVG file
      const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 450">
        <rect width="800" height="450" fill="#f5f5f5"/>
        <rect x="5" y="5" width="790" height="440" fill="none" stroke="#dddddd" stroke-width="10"/>
        <circle cx="400" cy="120" r="60" fill="#1565c0"/>
        <circle cx="400" cy="120" r="30" fill="#ffffff"/>
        <text x="400" y="220" font-family="Arial" font-size="36" font-weight="bold" fill="#1565c0" text-anchor="middle">${formatTitle(path.basename(img, '.svg'))}</text>
        <text x="400" y="270" font-family="Arial" font-size="24" fill="#666666" text-anchor="middle">DevoxxGenie Documentation</text>
        <text x="400" y="310" font-family="Arial" font-size="18" fill="#999999" text-anchor="middle">(Placeholder image - replace with actual SVG)</text>
        <text x="400" y="430" font-family="Arial" font-size="14" fill="#1565c0" text-anchor="middle">Generated for Docusaurus documentation</text>
      </svg>`;
      fs.writeFileSync(imgPath, svgContent);
      console.log(`Created SVG placeholder: ${imgPath}`);
    }
    else if (img.endsWith('.ico')) {
      // For ICO files, we'll use a simple transparent pixel
      const icoContent = Buffer.from([0, 0, 1, 0, 1, 0, 16, 16, 0, 0, 1, 0, 32, 0, 68, 4, 0, 0, 22, 0, 0, 0, 40, 0, 0, 0, 16, 0, 0, 0, 32, 0, 0, 0, 1, 0, 32, 0, 0, 0, 0, 0, 64, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]);
      fs.writeFileSync(imgPath, icoContent);
      console.log(`Created ICO placeholder: ${imgPath}`);
    }
    else {
      // For PNG, JPG, GIF create a PNG placeholder
      createPlaceholderImage(imgPath);
    }
  }
}

console.log('All placeholder images created in', __dirname);
