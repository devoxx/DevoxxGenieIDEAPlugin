# WebView Implementation with PrismJS

This package contains classes for rendering chat responses in a WebView using JCEF (JetBrains Chromium Embedded Framework) with PrismJS for syntax highlighting.

## Overview

The WebView implementation replaces the previous Swing-based approach for displaying markdown and code blocks in chat responses. It provides several advantages:

1. Better syntax highlighting for a wide range of programming languages
2. Improved code formatting and readability
3. Copy-to-clipboard functionality for code blocks
4. More consistent rendering across different JetBrains IDEs
5. Enhanced streaming capabilities for real-time responses

## Components

### WebServer

The `WebServer` class implements a lightweight HTTP server using Netty to serve HTML, CSS, and JavaScript resources to the WebView. It runs on a random available port and handles requests for static and dynamic content.

Key features:
- Embedded PrismJS resources for syntax highlighting
- Dynamic content generation for each response
- Singleton pattern for application-wide reuse

### WebViewFactory

The `WebViewFactory` class provides factory methods for creating and initializing JCef browser instances. It handles browser creation, configuration, and URL loading.

### WebViewController

The `WebViewController` class manages the interaction between Java code and the WebView. It handles:
- HTML content generation from markdown
- Code block rendering with PrismJS
- Language detection and mapping to PrismJS languages
- Event handling for browser loading and interactions

### WebViewResponsePanel

The `WebViewResponsePanel` class is a Swing component that wraps the WebView for use in the UI. It replaces the previous `ResponseDocumentPanel` implementation.

### StreamingWebViewResponsePanel

The `StreamingWebViewResponsePanel` class provides support for streaming responses, updating the WebView content in real-time as tokens are received from the LLM.

## Usage

The WebView components are integrated into the existing UI framework:

1. `ChatResponsePanel` uses `WebViewResponsePanel` to display non-streaming responses
2. `ChatStreamingResponsePanel` uses `StreamingWebViewResponsePanel` for streaming responses
3. All markdown and code rendering is handled by PrismJS in the browser

## Configuration

The WebView implementation does not require any user configuration and works out of the box. The WebServer automatically starts when needed and shuts down when the application closes.

## Dependencies

- JCEF (JetBrains Chromium Embedded Framework)
- Netty for HTTP server
- PrismJS (embedded in the plugin)

## Testing

A test HTML file is available at `src/main/resources/webview/test.html` to verify the WebView functionality.
