#!/bin/bash

# Update metadata for all Markdown files in the docs directory
echo "Updating metadata for all documentation files..."

# Find all Markdown files in the docs directory
find ../docs -name "*.md" > markdown-files.txt

# Run the metadata update script
node update-metadata.js markdown-files.txt

echo "Metadata update complete!"
