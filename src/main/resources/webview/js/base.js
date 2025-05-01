// Will be initialized when content is loaded
function highlightCode() {
    if (typeof Prism !== 'undefined') {
        Prism.highlightAll();
        
        // Add copy buttons to code blocks
        document.querySelectorAll('pre').forEach(function(pre) {
            const container = document.createElement('div');
            container.className = 'toolbar-container';
            
            const copyButton = document.createElement('button');
            copyButton.className = 'copy-button';
            const copyIcon = document.createElement('img');
            copyIcon.src = '/icons/copy.svg';
            copyIcon.alt = 'Copy';
            copyIcon.className = 'copy-icon';
            copyButton.appendChild(copyIcon);
            
            copyButton.addEventListener('click', function() {
                const code = pre.querySelector('code');
                const text = code.textContent;
                
                navigator.clipboard.writeText(text).then(function() {
                    // Store the original icon
                    const originalIcon = copyButton.innerHTML;
                    copyButton.innerHTML = 'Copied!';
                    setTimeout(function() {
                        // Restore the icon
                        copyButton.innerHTML = originalIcon;
                    }, 2000);
                }).catch(function(err) {
                    console.error('Failed to copy: ', err);
                    copyButton.innerHTML = 'Error!';
                    setTimeout(function() {
                        // Restore the icon
                        const copyIcon = document.createElement('img');
                        copyIcon.src = '../icons/copy.svg';
                        copyIcon.alt = 'Copy';
                        copyIcon.className = 'copy-icon';
                        copyButton.innerHTML = '';
                        copyButton.appendChild(copyIcon);
                    }, 2000);
                });
            });
            
            container.appendChild(copyButton);
            pre.appendChild(container);
        });
    }
}

// Call highlight when the page loads
document.addEventListener('DOMContentLoaded', highlightCode);