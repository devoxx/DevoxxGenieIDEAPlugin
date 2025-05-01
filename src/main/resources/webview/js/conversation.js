function copyToClipboard(contentToCopy, button) {
    // Store the original content of the button
    const originalContent = button.innerHTML;
    
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
    // Get all content except the Copy button and metadata
    const contentToCopy = Array.from(assistantMessage.childNodes)
        .filter(node => !node.classList || (!node.classList.contains('copy-response-button') && !node.classList.contains('metadata-info')))
        .map(node => node.textContent || node.innerText)
        .join('\n')
        .trim();

    copyToClipboard(contentToCopy, button);
}

function copyUserMessage(button) {
    const userMessage = button.closest('.user-message');
    // Get all the text from the user message paragraph(s)
    let contentToCopy = '';
    
    // Try to get text from paragraphs or directly from the div
    const paragraphs = userMessage.querySelectorAll('p');
    if (paragraphs && paragraphs.length > 0) {
        contentToCopy = Array.from(paragraphs)
            .map(p => p.textContent)
            .join('\n')
            .trim();
    } else {
        // Fallback to get all text content except the button
        contentToCopy = Array.from(userMessage.childNodes)
            .filter(node => node.nodeType === Node.TEXT_NODE || 
                    (node.nodeType === Node.ELEMENT_NODE && 
                     !node.classList.contains('copy-user-message-button')))
            .map(node => node.textContent)
            .join('\n')
            .trim();
    }

    copyToClipboard(contentToCopy, button);
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
                navigator.clipboard.writeText(text).then(function() {
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