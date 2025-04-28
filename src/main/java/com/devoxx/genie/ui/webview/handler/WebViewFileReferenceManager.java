package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.util.ThemeDetector;
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
                    .append(jsExecutor.escapeJS(fileId)) // Use escapeJS here since it's inside JavaScript
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
                
        // JavaScript to add the file references after the message pair
        String js = "try {\n" +
                    "  const messagePair = document.getElementById('" + jsExecutor.escapeJS(chatMessageContext.getId()) + "');\n" +
                    "  if (messagePair) {\n" +
                    "    const fileRefsContainer = document.createElement('div');\n" +
                    "    fileRefsContainer.innerHTML = `" + jsExecutor.escapeJS(fileReferencesHtml.toString()) + "`;\n" +
                    "    \n" +
                    "    messagePair.parentNode.insertBefore(fileRefsContainer, messagePair.nextSibling);\n" +
                    "    \n" +
                    "    if (!window.openFile) {\n" +
                    "      window.openFile = function(fileId) {\n" +
                    "        const fileElement = document.getElementById(fileId);\n" +
                    "        if (fileElement && fileElement.dataset.filePath) {\n" +
                    "          console.log('Opening file: ' + fileElement.dataset.filePath);\n" +
                    "          openFileFromJava(fileElement.dataset.filePath);\n" +
                    "        }\n" +
                    "      };\n" +
                    "    }\n" +
                    "    \n" +
                    "    if (!window.toggleFileReferences) {\n" +
                    "      window.toggleFileReferences = function(header) {\n" +
                    "        const content = header.nextElementSibling;\n" +
                    "        const toggle = header.querySelector('.file-references-toggle');\n" +
                    "        if (content.style.display === 'none') {\n" +
                    "          content.style.display = 'block';\n" +
                    "          toggle.textContent = '▼';\n" +
                    "        } else {\n" +
                    "          content.style.display = 'none';\n" +
                    "          toggle.textContent = '▶';\n" +
                    "        }\n" +
                    "      };\n" +
                    "    }\n" +
                    "    \n" +
                    "    if (!document.getElementById('file-references-styles')) {\n" +
                    "      const styleEl = document.createElement('style');\n" +
                    "      styleEl.id = 'file-references-styles';\n" +
                    "      styleEl.textContent = `\n" +
                    "        .file-references-container { margin: 10px 0; background-color: " + (ThemeDetector.isDarkTheme() ? "#1e1e1e" : "#f5f5f5") + "; border-radius: 4px; border-left: 4px solid " + (ThemeDetector.isDarkTheme() ? "#64b5f6" : "#2196F3") + "; }\n" +
                    "        .file-references-header { padding: 10px 8px; cursor: pointer; display: flex; align-items: center; }\n" +
                    "        .file-references-header:hover { background-color: " + (ThemeDetector.isDarkTheme() ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.05)") + "; }\n" +
                    "        .file-references-icon { margin-right: 8px; }\n" +
                    "        .file-references-title { flex-grow: 1; font-weight: bold; }\n" +
                    "        .file-references-toggle { margin-left: 8px; }\n" +
                    "        .file-references-content { padding: 10px 8px; border-top: 1px solid " + (ThemeDetector.isDarkTheme() ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)") + "; }\n" +
                    "        .file-list { list-style-type: none; padding: 0; margin: 0; }\n" +
                    "        .file-item { padding: 5px 0; }\n" +
                    "        .file-name { font-weight: bold; margin-right: 8px; }\n" +
                    "        .file-path { color: " + (ThemeDetector.isDarkTheme() ? "#aaaaaa" : "#666666") + "; font-style: italic; font-size: 0.9em; }\n" +
                    "      `;\n" +
                    "      document.head.appendChild(styleEl);\n" +
                    "    }\n" +
                    "    \n" +
                    "    window.scrollTo(0, document.body.scrollHeight);\n" +
                    "  } else {\n" +
                    "    console.error('Message pair not found: " + jsExecutor.escapeJS(chatMessageContext.getId()) + "');\n" +
                    "  }\n" +
                    "} catch (error) {\n" +
                    "  console.error('Error adding file references:', error);\n" +
                    "}";

        log.info("Executing JavaScript to add file references");
        jsExecutor.executeJavaScript(js);
    }
}