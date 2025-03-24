/**
 * Script to update metadata in Markdown files
 * 
 * This script automatically adds or updates front matter metadata in Markdown files
 * to ensure consistency across the documentation.
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

// Function to extract the file name without extension
function getPageName(filePath) {
  return path.basename(filePath, '.md')
    .split('-')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

// Function to generate keywords based on file path and content
function generateKeywords(filePath, content) {
  const baseKeywords = ['devoxxgenie', 'intellij plugin'];
  const pathKeywords = filePath
    .replace('/Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/docusaurus/docs/', '')
    .split('/')
    .filter(part => part !== '' && !part.endsWith('.md'))
    .map(part => part.replace(/-/g, ' '));
  
  // Extract h1 and h2 headings for additional keywords
  const headings = [];
  const headingMatches = content.matchAll(/^##?\s+(.+)$/gm);
  for (const match of headingMatches) {
    const heading = match[1].toLowerCase();
    if (heading && !heading.includes('overview')) {
      headings.push(heading);
    }
  }
  
  // Combine and filter keywords
  const allKeywords = [...baseKeywords, ...pathKeywords, ...headings]
    .map(kw => kw.trim().toLowerCase())
    .filter(kw => kw.length > 0 && kw.length < 30) // Avoid overly long phrases
    .slice(0, 8); // Limit to 8 keywords
  
  return allKeywords;
}

// Function to generate a description based on the first paragraph
function generateDescription(content) {
  // Remove front matter if present
  const contentWithoutFrontMatter = content.replace(/^---\n[\s\S]*?\n---\n/, '');
  
  // Find the first paragraph after headings
  const firstParagraphMatch = contentWithoutFrontMatter.match(/^(?!#)(.+?\.)(?:\s|$)/m);
  
  if (firstParagraphMatch && firstParagraphMatch[1]) {
    let description = firstParagraphMatch[1].trim();
    
    // Clean up the description
    description = description.replace(/\*\*/g, ''); // Remove markdown bold
    
    // Make sure it's not too long
    if (description.length > 160) {
      description = description.substring(0, 157) + '...';
    }
    
    return description;
  }
  
  // Fallback description
  return `Documentation for ${getPageName(filePath)} feature in DevoxxGenie, a Java-based LLM Code Assistant for IntelliJ IDEA.`;
}

// Function to get sidebar position from existing front matter or generate a new one
function getSidebarPosition(existingFrontMatter, filePath) {
  // Try to extract from existing front matter
  const positionMatch = existingFrontMatter.match(/sidebar_position:\s*(\d+)/);
  if (positionMatch && positionMatch[1]) {
    return positionMatch[1];
  }
  
  // Generate based on file name if it looks like a number
  const fileName = path.basename(filePath, '.md');
  if (/^\d+/.test(fileName)) {
    return fileName.match(/^(\d+)/)[1];
  }
  
  // Default value
  return '1';
}

// Function to update metadata in a file
function updateMetadata(filePath) {
  console.log(`Processing: ${filePath}`);
  
  const content = readFile(filePath);
  const pageName = getPageName(filePath);
  
  // Check if front matter exists
  const frontMatterMatch = content.match(/^---\n([\s\S]*?)\n---\n/);
  const existingFrontMatter = frontMatterMatch ? frontMatterMatch[1] : '';
  
  // Generate metadata
  const sidebarPosition = getSidebarPosition(existingFrontMatter, filePath);
  const description = generateDescription(content);
  const keywords = generateKeywords(filePath, content);
  
  // Create new front matter
  const newFrontMatter = `---
sidebar_position: ${sidebarPosition}
title: ${pageName} - DevoxxGenie Documentation
description: ${description}
keywords: [${keywords.join(', ')}]
image: /img/devoxxgenie-social-card.jpg
---`;

  // Replace existing front matter or add new front matter
  let updatedContent;
  if (frontMatterMatch) {
    updatedContent = content.replace(/^---\n[\s\S]*?\n---\n/, `${newFrontMatter}\n\n`);
  } else {
    updatedContent = `${newFrontMatter}\n\n${content}`;
  }
  
  // Write updated content
  writeFile(filePath, updatedContent);
  console.log(`Updated metadata for: ${filePath}`);
}

// Main function to process all files
function processFiles(fileList) {
  const filepaths = fileList.split('\n').filter(path => path.trim() !== '');
  
  filepaths.forEach(filepath => {
    try {
      updateMetadata(filepath);
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
