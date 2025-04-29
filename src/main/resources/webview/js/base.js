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
            copyButton.textContent = 'Copy';
            
            copyButton.addEventListener('click', function() {
                const code = pre.querySelector('code');
                const text = code.textContent;
                
                navigator.clipboard.writeText(text).then(function() {
                    copyButton.textContent = 'Copied!';
                    setTimeout(function() {
                        copyButton.textContent = 'Copy';
                    }, 2000);
                }).catch(function(err) {
                    console.error('Failed to copy: ', err);
                });
            });
            
            container.appendChild(copyButton);
            pre.appendChild(container);
        });
    }
}

// Call highlight when the page loads
document.addEventListener('DOMContentLoaded', highlightCode);