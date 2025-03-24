#!/bin/bash
# Script to set up and run the DevoxxGenie documentation site

echo "Setting up DevoxxGenie Docusaurus documentation site..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "Node.js is required but not installed. Please install Node.js version 18 or higher."
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d. -f1 | sed 's/v//')
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "Node.js version 18 or higher is required. Current version: $(node -v)"
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
    
    if [ $? -ne 0 ]; then
        echo "Dependency installation failed. Please check the errors above."
        exit 1
    fi
fi

# Note: npm start will automatically generate placeholder images
# No need to run placeholder-setup.js separately

# Start the development server
echo "Starting Docusaurus development server..."
echo "The documentation site will be available at http://localhost:3000/"
npm start
