/**
 * Updates the thinking indicator with formatted MCP logs
 * 
 * @param {string} messageId - The ID of the message to update
 * @param {string} content - The HTML content to show in the thinking indicator
 * @param {boolean} isDarkTheme - Whether the current theme is dark
 */
function updateMCPThinkingIndicator(messageId, content, isDarkTheme) {
  try {
    const indicator = document.getElementById('loading-' + messageId);
    if (indicator) {
      indicator.innerHTML = content;
      indicator.style.display = 'block';
      if (!document.getElementById('mcp-logs-styles')) {
        // Set CSS variables based on theme
        document.documentElement.style.setProperty('--mcp-container-bg', isDarkTheme ? "#2d2d2d" : "#f8f8f8");
        document.documentElement.style.setProperty('--mcp-pre-bg', isDarkTheme ? "#1e1e1e" : "#f0f0f0");
        
        // Load external CSS file
        const linkEl = document.createElement('link');
        linkEl.id = 'mcp-logs-styles';
        linkEl.rel = 'stylesheet';
        linkEl.href = '../css/mcpLogs.css';
        document.head.appendChild(linkEl);
      }
      window.scrollTo(0, document.body.scrollHeight);
      const mcpContainer = indicator.querySelector('.mcp-outer-container');
      if (mcpContainer) {
        mcpContainer.scrollTop = mcpContainer.scrollHeight;
      }
      if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }
    } else {
      const messagePair = document.getElementById(messageId);
      if (messagePair) {
        const loadingIndicator = messagePair.querySelector('.loading-indicator');
        if (loadingIndicator) {
          loadingIndicator.innerHTML = content;
          loadingIndicator.style.display = 'block';
          const mcpContainer = loadingIndicator.querySelector('.mcp-outer-container');
          if (mcpContainer) {
            mcpContainer.scrollTop = mcpContainer.scrollHeight;
          }
          if (typeof highlightCodeBlocks === 'function') { highlightCodeBlocks(); }
        }
      }
    }
  } catch (error) {
    console.error('Error updating MCP thinking indicator:', error);
  }
}