function copyToClipboard(contentToCopy, button, isHtml = false) {
    // Store the original content of the button
    const originalContent = button.innerHTML;
    
    // If the content is HTML and we need to convert it to Markdown
    if (isHtml) {
        // Use Turndown library to convert HTML to Markdown
        const turndownService = new TurndownService({
            headingStyle: 'atx',
            codeBlockStyle: 'fenced',
            emDelimiter: '*',
            strongDelimiter: '**'
        });
        
        // Configure Turndown to handle code blocks better
        turndownService.addRule('fencedCodeBlock', {
            filter: function (node, options) {
                return (
                    node.nodeName === 'PRE' &&
                    node.firstChild &&
                    node.firstChild.nodeName === 'CODE'
                )
            },
            replacement: function (content, node, options) {
                const className = node.firstChild.getAttribute('class') || '';
                const language = (className.match(/language-(\w+)/) || [null, ''])[1];
                const code = node.firstChild.textContent;
                return '\n\n```' + language + '\n' + code + '\n```\n\n';
            }
        });
        
        // Skip certain divs like metadata and copy buttons
        turndownService.remove(function(node) {
            return (
                node.nodeName === 'DIV' && 
                (node.classList.contains('metadata-info') || 
                 node.classList.contains('copy-response-button') ||
                 node.classList.contains('loading-indicator'))
            );
        });
        
        contentToCopy = turndownService.turndown(contentToCopy);
    }
    
    navigator.clipboard.writeText(contentToCopy).then(function () {
        // Add animation class
        button.classList.add('copy-button-flash');
        button.innerHTML = 'Copied!';

        setTimeout(function () {
            button.innerHTML = originalContent;
            button.classList.remove('copy-button-flash');
        }, 2000);
    }).catch(function (err) {
        console.error('Failed to copy: ', err);
        button.innerHTML = 'Error!';
        setTimeout(function () {
            button.innerHTML = originalContent;
        }, 2000);
    });
}

function copyMessageResponse(button) {
    const assistantMessage = button.closest('.assistant-message');
    
    // Create a clone of the assistant message to work with
    const clonedMessage = assistantMessage.cloneNode(true);
    
    // Remove the copy button and metadata from the clone
    const copyButton = clonedMessage.querySelector('.copy-response-button');
    if (copyButton) copyButton.remove();
    
    const metadata = clonedMessage.querySelector('.metadata-info');
    if (metadata) metadata.remove();
    
    // Get the HTML content of the message (without the removed elements)
    const htmlContent = clonedMessage.innerHTML;
    
    // Copy the content as Markdown
    copyToClipboard(htmlContent, button, true);
}

function copyUserMessage(button) {
    const userMessage = button.closest('.user-message');
    
    // Create a clone of the user message to work with
    const clonedMessage = userMessage.cloneNode(true);
    
    // Remove the copy button from the clone
    const copyButton = clonedMessage.querySelector('.copy-user-message-button');
    if (copyButton) copyButton.remove();
    
    // Remove any margin wrapper divs
    const marginWrapper = clonedMessage.querySelector('div[style*="margin-right"]');
    if (marginWrapper) {
        // Move the content from the wrapper to the parent
        while (marginWrapper.firstChild) {
            clonedMessage.appendChild(marginWrapper.firstChild);
        }
        marginWrapper.remove();
    }
    
    // Get the HTML content of the message (without the removed elements)
    const htmlContent = clonedMessage.innerHTML;
    
    // Copy the content as Markdown
    copyToClipboard(htmlContent, button, true);
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
            const copyIcon = document.createElement('img');
            copyIcon.src = '../icons/copy.svg';
            copyIcon.alt = 'Copy';
            copyIcon.className = 'copy-icon';
            button.appendChild(copyIcon);
            var container = document.createElement('div');
            container.className = 'toolbar-container';
            container.appendChild(button);
            block.appendChild(container);
            button.addEventListener('click', function() {
                var code = block.querySelector('code');
                var text = code.textContent;
                
                // Get language class if available
                var language = '';
                if (code.className) {
                    var match = code.className.match(/language-(\w+)/);
                    if (match) {
                        language = match[1];
                    }
                }
                
                // Format as markdown code block
                var markdownText = '```' + language + '\n' + text + '\n```';
                
                navigator.clipboard.writeText(markdownText).then(function() {
                    // Store the original icon
                    const originalIcon = button.innerHTML;
                    button.innerHTML = 'Copied!';
                    setTimeout(function() {
                        // Restore the icon
                        button.innerHTML = originalIcon;
                    }, 2000);
                }).catch(function(err) {
                    console.error('Failed to copy: ', err);
                    button.innerHTML = 'Error!';
                    setTimeout(function() {
                        // Restore the icon
                        const copyIcon = document.createElement('img');
                        copyIcon.src = '../icons/copy.svg';
                        copyIcon.alt = 'Copy';
                        copyIcon.className = 'copy-icon';
                        button.innerHTML = '';
                        button.appendChild(copyIcon);
                    }, 2000);
                });
            });
        });
    }
}
// Add copy buttons to user messages that don't have them
function addCopyButtonsToUserMessages() {
    document.querySelectorAll('.user-message:not(.processed-copy-button)').forEach(function(userMessage) {
        // Mark the message as processed
        userMessage.classList.add('processed-copy-button');
        
        // Check if it already has a copy button
        if (!userMessage.querySelector('.copy-user-message-button')) {
            // Create the button
            const button = document.createElement('button');
            button.className = 'copy-user-message-button';
            const copyIcon = document.createElement('img');
            copyIcon.src = '../icons/copy.svg';
            copyIcon.alt = 'Copy';
            copyIcon.className = 'copy-icon';
            button.appendChild(copyIcon);
            button.onclick = function() { copyUserMessage(this); };
            
            // Add the button to the user message
            userMessage.insertBefore(button, userMessage.firstChild);
            
            // Ensure the text doesn't overlap with the button by adding margin wrapper if needed
            if (!userMessage.querySelector('div[style*="margin-right"]')) {
                // Get all content except the button
                const contentNodes = Array.from(userMessage.childNodes)
                    .filter(node => node !== button);
                
                // Create wrapper div with margin
                const wrapper = document.createElement('div');
                wrapper.style.marginRight = '50px';
                
                // Move content to wrapper
                contentNodes.forEach(node => {
                    wrapper.appendChild(node.cloneNode(true));
                    if (node.parentNode === userMessage) {
                        userMessage.removeChild(node);
                    }
                });
                
                // Add wrapper to message
                userMessage.appendChild(wrapper);
            }
        }
    });
}

// Initialize when the page loads
document.addEventListener('DOMContentLoaded', function() {
    highlightCodeBlocks();
    addCopyButtonsToUserMessages();
    // Ensure the whole page is visible
    document.body.style.display = 'block';
    
    // Use a MutationObserver to detect when new messages are added
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.addedNodes && mutation.addedNodes.length > 0) {
                // Check for new user messages and add copy buttons
                addCopyButtonsToUserMessages();
            }
        });
    });
    
    // Start observing the conversation container for changes
    const container = document.getElementById('conversation-container');
    if (container) {
        observer.observe(container, { childList: true, subtree: true });
    }
});