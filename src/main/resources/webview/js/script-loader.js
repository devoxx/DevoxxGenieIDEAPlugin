function loadScriptContent(id, scriptContent) {
    // Check if the script with this id already exists
    if (!document.getElementById(id)) {
        // Create a new script element
        const scriptEl = document.createElement('script');
        scriptEl.id = id;
        scriptEl.textContent = scriptContent;
        
        // Add it to the head
        document.head.appendChild(scriptEl);
        return true;
    }
    return false;
}

function loadStyleContent(id, styleContent) {
    // Check if the style with this id already exists
    if (!document.getElementById(id)) {
        // Create a new style element
        const styleEl = document.createElement('style');
        styleEl.id = id;
        styleEl.textContent = styleContent;
        
        // Add it to the head
        document.head.appendChild(styleEl);
        return true;
    }
    return false;
}

function loadScriptFromUrl(id, url) {
    // Check if the script with this id already exists
    if (!document.getElementById(id)) {
        // Create a new script element
        const scriptEl = document.createElement('script');
        scriptEl.id = id;
        scriptEl.src = url;
        
        // Add it to the head
        document.head.appendChild(scriptEl);
        return true;
    }
    return false;
}

function loadStyleFromUrl(id, url) {
    // Check if the link with this id already exists
    if (!document.getElementById(id)) {
        // Create a new link element
        const linkEl = document.createElement('link');
        linkEl.id = id;
        linkEl.rel = 'stylesheet';
        linkEl.href = url;
        
        // Add it to the head
        document.head.appendChild(linkEl);
        return true;
    }
    return false;
}