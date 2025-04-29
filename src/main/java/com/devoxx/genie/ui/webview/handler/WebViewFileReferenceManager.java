package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.template.ResourceLoader;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Manages file references in the WebView.
 * This class handles adding file references to the conversation.
 */
@Slf4j
public class WebViewFileReferenceManager {

    private final WebViewJavaScriptExecutor jsExecutor;
    private boolean fileReferencesScriptAdded = false;
    
    public WebViewFileReferenceManager(WebViewJavaScriptExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }
    
    /**
     * Add file references to the conversation view.
     *
     * @param chatMessageContext The chat message context
     * @param files The list of files to reference
     */
    public void addFileReferences(ChatMessageContext chatMessageContext, List<VirtualFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        
        log.info("Adding file references to conversation: " + files.size() + " files");
        
        // Create HTML for the expandable file references component
        StringBuilder fileReferencesHtml = new StringBuilder();
        fileReferencesHtml.append("<div class=\"file-references-container\" id=\"file-refs-")
                .append(jsExecutor.escapeHtml(chatMessageContext.getId()))
                .append("\">\n");
                
        // Add the collapsible header
        fileReferencesHtml.append("  <div class=\"file-references-header\" onclick=\"toggleFileReferences(this)\">\n")
                .append("    <span class=\"file-references-icon\">📂</span>\n")
                .append("    <span class=\"file-references-title\">Referenced Files (")
                .append(files.size())
                .append(")</span>\n")
                .append("    <span class=\"file-references-toggle\">▶</span>\n")
                .append("  </div>\n");
                
        // Add the file list container (initially collapsed)
        fileReferencesHtml.append("  <div class=\"file-references-content\" style=\"display: none;\">\n")
                .append("    <ul class=\"file-list\">\n");
                
        // Add each file as a list item - make items clickable with data attributes to store path
        for (int i = 0; i < files.size(); i++) {
            VirtualFile file = files.get(i);
            String fileId = "file-" + jsExecutor.escapeHtml(chatMessageContext.getId()) + "-" + i;
            fileReferencesHtml.append("      <li class=\"file-item\" id=\"")
                    .append(fileId)
                    .append("\" data-file-path=\"")
                    .append(jsExecutor.escapeHtml(file.getPath()))
                    .append("\" style=\"cursor: pointer;\" onclick=\"openFile('")
                    .append(jsExecutor.escapeJS(fileId))
                    .append("')\">\n")
                    .append("        <span class=\"file-name\">")
                    .append(jsExecutor.escapeHtml(file.getName()))
                    .append("</span>\n")
                    .append("        <span class=\"file-path\">")
                    .append(jsExecutor.escapeHtml(file.getPath().replace(chatMessageContext.getProject().getBasePath() + "/", "")))
                    .append("</span>\n")
                    .append("      </li>\n");
        }
        
        fileReferencesHtml.append("    </ul>\n")
                .append("  </div>\n")
                .append("</div>\n");
         
        // Make sure the file references script is loaded
        ensureFileReferencesScriptLoaded();
                
        // Call the JavaScript function to add file references and apply styles
        String js = String.format(
            "if (typeof addFileReferencesToConversation === 'function') {\n" +
            "  addFileReferencesToConversation('%s', `%s`);\n" +
            "  addFileReferencesStyles(%s);\n" +
            "} else {\n" +
            "  console.error('File references functions not loaded properly');\n" +
            "}",
            jsExecutor.escapeJS(chatMessageContext.getId()),
            jsExecutor.escapeJS(fileReferencesHtml.toString()),
            ThemeDetector.isDarkTheme()
        );

        log.info("Executing JavaScript to add file references");
        jsExecutor.executeJavaScript(js);
    }
    
    /**
     * Ensure the file references JavaScript is loaded.
     * This uses the script-loader.js utility to dynamically load scripts.
     */
    private void ensureFileReferencesScriptLoaded() {
        if (!fileReferencesScriptAdded) {
            // First ensure script loader is available
            ensureScriptLoaderAvailable();
            
            // Load the file references script using the script loader
            String fileReferencesScript = ResourceLoader.loadResource("webview/js/file-references.js");
            
            // Use the script loader to add the file references script
            String js = "if (typeof loadScriptContent === 'function') {\n" +
                        "  loadScriptContent('file-references-script', `" + jsExecutor.escapeJS(fileReferencesScript) + "`);\n" +
                        "} else {\n" +
                        "  console.error('Script loader not available');\n" +
                        "}";
            
            jsExecutor.executeJavaScript(js);
            fileReferencesScriptAdded = true;
        }
    }
    
    /**
     * Ensure the script loader utility is available.
     */
    private void ensureScriptLoaderAvailable() {
        // Load the script loader if it doesn't exist in the page
        String scriptLoaderJs = ResourceLoader.loadResource("webview/js/script-loader.js");
        
        String js = "if (!document.getElementById('script-loader')) {\n" +
                    "  const scriptEl = document.createElement('script');\n" +
                    "  scriptEl.id = 'script-loader';\n" +
                    "  scriptEl.textContent = `" + jsExecutor.escapeJS(scriptLoaderJs) + "`;\n" +
                    "  document.head.appendChild(scriptEl);\n" +
                    "}";
        
        jsExecutor.executeJavaScript(js);
    }
}