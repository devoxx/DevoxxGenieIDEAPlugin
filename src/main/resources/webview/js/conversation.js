function copyMessageResponse(button) {
    const assistantMessage = button.closest('.assistant-message');
    // Get all content except the Copy button and metadata
    const contentToCopy = Array.from(assistantMessage.childNodes)
        .filter(node => !node.classList || (!node.classList.contains('copy-response-button') && !node.classList.contains('metadata-info')))
        .map(node => node.textContent || node.innerText)
        .join('\n')
        .trim();

    navigator.clipboard.writeText(contentToCopy).then(function() {
        // Add animation class
        button.classList.add('copy-button-flash');
        button.textContent = 'Copied!';

        setTimeout(function() {
            button.textContent = 'Copy';
            button.classList.remove('copy-button-flash');
        }, 2000);
    }).catch(function(err) {
        console.error('Failed to copy: ', err);
        button.textContent = 'Error!';
        setTimeout(function() {
            button.textContent = 'Copy';
        }, 2000);
    });
}

function toggleFileReferences(header) {
    const content = header.nextElementSibling;
    const toggle = header.querySelector('.file-references-toggle');
    if (content.style.display === 'none') {
        content.style.display = 'block';
        toggle.textContent = '▼';
    } else {
        content.style.display = 'none';
        toggle.textContent = '▶';
    }
}

function highlightCodeBlocks() {
    if (typeof Prism !== 'undefined') {
        Prism.highlightAll();
        // Add copy buttons to code blocks
        document.querySelectorAll('pre:not(.processed)').forEach(function(block) {
            // Mark the block as processed to avoid adding buttons multiple times
            block.classList.add('processed');
            var button = document.createElement('button');
            button.className = 'copy-button';
            button.textContent = 'Copy';
            var container = document.createElement('div');
            container.className = 'toolbar-container';
            container.appendChild(button);
            block.appendChild(container);
            button.addEventListener('click', function() {
                var code = block.querySelector('code');
                var text = code.textContent;
                navigator.clipboard.writeText(text).then(function() {
                    button.textContent = 'Copied!';
                    setTimeout(function() {
                        button.textContent = 'Copy';
                    }, 2000);
                }).catch(function(err) {
                    console.error('Failed to copy: ', err);
                    button.textContent = 'Error!';
                });
            });
        });
    }
}
// Initialize when the page loads
document.addEventListener('DOMContentLoaded', function() {
    highlightCodeBlocks();
    // Ensure the whole page is visible
    document.body.style.display = 'block';
});