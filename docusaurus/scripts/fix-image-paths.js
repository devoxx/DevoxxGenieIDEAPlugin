/**
 * Script to fix absolute image paths in Markdown files
 * 
 * This script replaces absolute image paths like:
 *    ![alt text](/img/example.png)
 * with MDX import syntax:
 *    ![alt text](../../../static/img/example.png)
 */

const fs = require('fs');
const path = require('path');

// Function to read a file
function readFile(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

// Function to write to a file
function writeFile(filePath, content) {
  fs.writeFileSync(filePath, content, 'utf8');
}

// Function to convert absolute image paths in Markdown files
function fixImagePaths(filePath) {
  console.log(`Processing: ${filePath}`);
  
  const content = readFile(filePath);
  
  // Replace image links with MDX import syntax
  // This regex finds markdown image syntax with absolute paths starting with /img/
  const absolutePathRegex = /!\[(.*?)\]\(\/img\/(.*?)\)/g;
  
  // Calculate relative path from current file to /static/img directory
  const relativePath = path.relative(
    path.dirname(filePath),
    path.join(path.dirname(filePath), '..', '..', 'static', 'img')
  ).replace(/\\/g, '/');
  
  // Replace absolute paths with relative paths
  const updatedContent = content.replace(
    absolutePathRegex,
    (match, altText, imagePath) => `![${altText}](${relativePath}/${imagePath})`
  );
  
  if (content !== updatedContent) {
    writeFile(filePath, updatedContent);
    console.log(`  Updated image paths in: ${filePath}`);
  } else {
    console.log(`  No changes needed in: ${filePath}`);
  }
}

// Main function to process all files
function processFiles(fileList) {
  const filepaths = fileList.split('\n').filter(path => path.trim() !== '');
  
  filepaths.forEach(filepath => {
    try {
      fixImagePaths(filepath);
    } catch (error) {
      console.error(`Error processing ${filepath}:`, error);
    }
  });
  
  console.log(`\nProcessed ${filepaths.length} files.`);
}

// Check if file list is provided as an argument
if (process.argv.length > 2) {
  const fileListPath = process.argv[2];
  const fileList = readFile(fileListPath);
  processFiles(fileList);
} else {
  console.error('Please provide the path to a text file containing the list of Markdown files to process.');
  process.exit(1);
}
