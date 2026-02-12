/**
 * Opens a file in the IDE based on its ID
 * @param {string} fileId - The ID of the file element to open
 */
function openFile(fileId) {
    const fileElement = document.getElementById(fileId);
    if (fileElement && fileElement.dataset.filePath) {
        console.log('Opening file: ' + fileElement.dataset.filePath);
        openFileFromJava(fileElement.dataset.filePath);
    }
}


/**
 * Toggles the visibility of file references section
 * @param {HTMLElement} header - The header element that was clicked
 */
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

/**
 * Adds file references to the conversation
 * @param {string} messageId - The ID of the message to attach file references to
 * @param {string} fileReferencesHtml - The HTML content for the file references
 */
function addFileReferencesToConversation(messageId, fileReferencesHtml) {
    try {
        const messagePair = document.getElementById(messageId);
        if (messagePair) {
            const fileRefsContainer = document.createElement('div');
            fileRefsContainer.innerHTML = fileReferencesHtml;
            
            messagePair.parentNode.insertBefore(fileRefsContainer, messagePair.nextSibling);
            
            // Scroll to the bottom (deferred to allow layout recalculation)
            setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0);
        } else {
            console.error('Message pair not found: ' + messageId);
        }
    } catch (error) {
        console.error('Error adding file references:', error);
    }
}

/**
 * Adds the necessary styles for file references to the document head
 * @param {boolean} isDarkTheme - Whether the IDE is using dark theme
 */
function addFileReferencesStyles(isDarkTheme) {
    if (!document.getElementById('file-references-styles')) {
        const styleEl = document.createElement('style');
        styleEl.id = 'file-references-styles';
        
        // Now using CSS variables consistent with our theme system
        styleEl.textContent = `
            .file-references-container { 
                margin: 10px 0; 
                background-color: var(--file-refs-bg); 
                border-radius: 4px; 
                border-left: 4px solid var(--file-refs-border); 
            }
            .file-references-header { 
                padding: 10px 8px; 
                cursor: pointer; 
                display: flex; 
                align-items: center; 
            }
            .file-references-header:hover { 
                background-color: var(--file-refs-header-hover-bg); 
            }
            .file-references-icon { 
                margin-right: 8px; 
            }
            .file-references-title { 
                flex-grow: 1; 
                font-weight: bold; 
            }
            .file-references-toggle { 
                margin-left: 8px; 
            }
            .file-references-content { 
                padding: 10px 8px; 
                border-top: 1px solid var(--file-refs-divider-color); 
            }
            .file-list { 
                list-style-type: none; 
                padding: 0; 
                margin: 0; 
            }
            .file-item { 
                padding: 5px 0; 
            }
            .file-name { 
                font-weight: bold; 
                margin-right: 8px; 
            }
            .file-path { 
                color: var(--file-path-color); 
                font-style: italic; 
                font-size: 0.9em; 
            }
            .file-references-info {
                display: flex;
                align-items: flex-start;
                padding: 8px;
                margin-bottom: 10px;
                background-color: var(--info-bg-color, rgba(33, 150, 243, 0.1));
                border-radius: 4px;
                border: 1px solid var(--info-border-color, rgba(33, 150, 243, 0.3));
            }
            .info-icon {
                margin-right: 8px;
                flex-shrink: 0;
            }
            .info-text {
                font-size: 0.9em;
                line-height: 1.4;
            }
        `;
        
        document.head.appendChild(styleEl);
    }
}