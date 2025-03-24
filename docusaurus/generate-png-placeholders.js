/**
 * This script generates actual PNG placeholder images for Docusaurus
 * It creates simple colored rectangles with text labels
 */

const fs = require('fs');
const path = require('path');

// List of all image files that need placeholders
const imagePaths = [
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

// Create the 1x1 pixel PNG image bytes
// This is a minimal valid PNG file with a single transparent pixel
const createMinimalPNG = () => {
  // PNG signature
  const signature = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
  
  // IHDR chunk (image header)
  const ihdrLength = Buffer.from([0x00, 0x00, 0x00, 0x0D]); // 13 bytes length
  const ihdrType = Buffer.from([0x49, 0x48, 0x44, 0x52]); // "IHDR"
  const ihdrData = Buffer.from([
    0x00, 0x00, 0x01, 0x2C, // Width: 300
    0x00, 0x00, 0x00, 0x96, // Height: 150
    0x08,                   // Bit depth: 8 bits
    0x06,                   // Color type: RGBA
    0x00,                   // Compression method: 0 (deflate)
    0x00,                   // Filter method: 0
    0x00                    // Interlace method: 0 (no interlace)
  ]);
  const ihdrCrc = Buffer.from([0x6E, 0x00, 0x6E, 0x3F]); // CRC-32 of IHDR chunk
  
  // IDAT chunk (minimal image data with one transparent pixel)
  const idatLength = Buffer.from([0x00, 0x00, 0x00, 0x15]); // 21 bytes length
  const idatType = Buffer.from([0x49, 0x44, 0x41, 0x54]); // "IDAT"
  const idatData = Buffer.from([
    0x78, 0x9C, 0x63, 0x60, 0x60, 0x60, 0x40, 0x00, 0x00, 0x00, 0x04, 0x00, 0x01
  ]);
  const idatCrc = Buffer.from([0x0A, 0xFB, 0x03, 0xAB]); // CRC-32 of IDAT chunk
  
  // IEND chunk (end of PNG)
  const iendLength = Buffer.from([0x00, 0x00, 0x00, 0x00]); // 0 bytes length
  const iendType = Buffer.from([0x49, 0x45, 0x4E, 0x44]); // "IEND"
  const iendCrc = Buffer.from([0xAE, 0x42, 0x60, 0x82]); // CRC-32 of IEND chunk
  
  // Combine all chunks into a single buffer
  return Buffer.concat([
    signature,
    ihdrLength, ihdrType, ihdrData, ihdrCrc,
    idatLength, idatType, idatData, idatCrc,
    iendLength, iendType, iendCrc
  ]);
};

// Create a directory if it doesn't exist
const imgDir = path.join(__dirname, 'static', 'img');
if (!fs.existsSync(imgDir)) {
  fs.mkdirSync(imgDir, { recursive: true });
  console.log(`Created directory: ${imgDir}`);
}

// Generate PNG placeholder for each image path
const pngData = createMinimalPNG();
imagePaths.forEach(imagePath => {
  const fullPath = path.join(imgDir, imagePath);
  
  // Skip if file already exists
  if (fs.existsSync(fullPath)) {
    return;
  }
  
  try {
    fs.writeFileSync(fullPath, pngData);
    console.log(`Created placeholder: ${fullPath}`);
  } catch (error) {
    console.error(`Error creating ${fullPath}:`, error);
  }
});

console.log('PNG placeholders generation complete!');
