/**
 * External Link Handler JavaScript
 * 
 * This script handles external links in the webview by adding visual indicators.
 * The main external link handling is done by the Java CefRequestHandler.
 */

(function() {
    'use strict';
    
    console.log('Initializing external link handler JavaScript...');
    
    // Function to check if a URL is external (not from our internal server)
    function isExternalUrl(url) {
        if (!url) return false;
        
        try {
            const linkUrl = new URL(url);
            const protocol = linkUrl.protocol.toLowerCase();
            
            // Only handle HTTP and HTTPS URLs
            if (protocol !== 'http:' && protocol !== 'https:') {
                return false;
            }
            
            // Check if it's not from our internal server
            return !url.startsWith(window.location.origin);
        } catch (e) {
            return false;
        }
    }
    
    // Function to add visual indicators to external links
    function addExternalLinkIndicators() {
        const links = document.querySelectorAll('a[href]');
        links.forEach(function(link) {
            const href = link.getAttribute('href');
            if (isExternalUrl(href) && !link.querySelector('.external-link-indicator')) {
                // Add visual indicator for external links
                const indicator = document.createElement('span');
                indicator.className = 'external-link-indicator';
                indicator.innerHTML = '↗';
                indicator.title = 'Opens in external browser';
                link.appendChild(indicator);
                
                // Also add a class to the link for CSS styling
                link.classList.add('external-link');
                
                console.log('Added external link indicator for:', href);
            }
        });
    }
    
    // Function to setup everything
    function setupExternalLinkHandler() {
        // Process any existing links
        addExternalLinkIndicators();
        
        // Set up mutation observer for dynamically added links
        const observer = new MutationObserver(function(mutations) {
            let shouldUpdate = false;
            mutations.forEach(function(mutation) {
                mutation.addedNodes.forEach(function(node) {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        // Check if the added node is a link or contains links
                        if (node.tagName === 'A' || (node.querySelector && node.querySelector('a'))) {
                            shouldUpdate = true;
                        }
                    }
                });
            });
            
            if (shouldUpdate) {
                // Use setTimeout to batch updates and avoid performance issues
                setTimeout(addExternalLinkIndicators, 10);
            }
        });
        
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
        
        console.log('External link handler initialized successfully');
    }
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupExternalLinkHandler);
    } else {
        setupExternalLinkHandler();
    }
    
    // Expose function globally for reinitializing if needed
    window.initializeExternalLinkHandler = setupExternalLinkHandler;
    
})();
