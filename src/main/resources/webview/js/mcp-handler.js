/**
 * MCP message handler functions
 */

// Toggle MCP tool content visibility
function toggleToolContent(element) {
    const toolMessage = element.closest('.tool-message');
    if (toolMessage) {
        toolMessage.classList.toggle('collapsed');
    }
}

// Copy tool output to clipboard
function copyToolOutput(button) {
    const toolMessage = button.closest('.tool-message');
    if (!toolMessage) return;
    
    const toolContent = toolMessage.querySelector('.tool-content');
    if (!toolContent) return;
    
    // Get the text content
    const textToCopy = toolContent.textContent;
    
    // Copy to clipboard
    navigator.clipboard.writeText(textToCopy).then(
        function() {
            // Add flash animation class
            button.classList.add('copy-button-flash');
            
            // Remove the class after animation completes
            setTimeout(function() {
                button.classList.remove('copy-button-flash');
            }, 500);
        }
    ).catch(function(err) {
        console.error('Could not copy text: ', err);
    });
}

// Expand all tool messages
function expandAllTools() {
    document.querySelectorAll('.tool-message.collapsed').forEach(function(element) {
        element.classList.remove('collapsed');
    });
}

// Collapse all tool messages
function collapseAllTools() {
    document.querySelectorAll('.tool-message:not(.collapsed)').forEach(function(element) {
        element.classList.add('collapsed');
    });
}

// Format JSON in tool results for better readability
function formatJsonOutput() {
    document.querySelectorAll('.tool-result').forEach(function(element) {
        try {
            const content = element.textContent.trim();
            // Only attempt to format if it looks like JSON
            if ((content.startsWith('{') && content.endsWith('}')) || 
                (content.startsWith('[') && content.endsWith(']'))) {
                
                const json = JSON.parse(content);
                const formatted = JSON.stringify(json, null, 2);
                element.textContent = formatted;
            }
        } catch (e) {
            // Not valid JSON, leave as is
            console.log('Not valid JSON, skipping formatting');
        }
    });
}

// Initialize all MCP features when the document is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Format any JSON outputs for readability
    formatJsonOutput();
    
    // Add event listeners for any dynamically added MCP content
    document.addEventListener('click', function(event) {
        if (event.target.closest('.toggle-icon')) {
            toggleToolContent(event.target);
        }
        
        if (event.target.closest('.copy-tool-button')) {
            copyToolOutput(event.target.closest('.copy-tool-button'));
        }
    });
});

// Function to be called when new MCP content is added to the DOM
function initNewMcpContent() {
    formatJsonOutput();
}
