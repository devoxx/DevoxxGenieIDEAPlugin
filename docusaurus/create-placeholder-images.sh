#!/bin/bash
# This script creates placeholder images for the Docusaurus site

PLACEHOLDER_SVG="./static/img/devoxxgenie-placeholder.svg"
IMG_DIR="./static/img"

# List of all image names mentioned in the documentation
image_names=(
  "logo.png"
  "installation-marketplace.png"
  "ollama-setup.png"
  "openai-setup.png"
  "provider-selection.png"
  "custom-openai-settings.png"
  "settings-llm-providers.png"
  "prompts-settings.png"
  "token-cost-settings.png"
  "token-calculator.png"
  "devoxxgenie-md-settings.png"
  "chat-interface.png"
  "rag-setup.png"
  "rag-references.png"
  "mcp-settings.png"
  "mcp-logs.png"
  "dnd-images.png"
  "git-diff-settings.png"
  "git-diff-viewer.png"
  "add-project.png"
  "project-scanner-settings.png"
  "web-search-settings.png"
  "chat-memory-settings.png"
  "devoxxgenie-toolwindow.png"
  "devoxxgenie-demo.gif"
  "rag-feature.png"
  "devoxxgenie-social-card.jpg"
  "favicon.ico"
)

# Create placeholder image for each image name if it doesn't exist
for img in "${image_names[@]}"; do
  if [[ ! -f "$IMG_DIR/$img" ]]; then
    echo "Creating placeholder for $img"
    if [[ "$img" == *.svg ]]; then
      cp "$PLACEHOLDER_SVG" "$IMG_DIR/$img"
    else
      # For non-SVG files, convert the SVG to the appropriate format
      # This requires ImageMagick to be installed
      # If ImageMagick is not available, simply copy the SVG as a placeholder
      cp "$PLACEHOLDER_SVG" "$IMG_DIR/$img"
    fi
  fi
done

echo "Placeholder images created in $IMG_DIR"
