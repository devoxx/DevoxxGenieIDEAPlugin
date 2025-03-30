package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.panel.chatresponse.*;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ChatResponsePanel extends BackgroundPanel {

    public ChatResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getId());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new ResponseHeaderPanel(chatMessageContext));
        add(new ResponseDocumentPanel(chatMessageContext));

        if (!FileListManager.getInstance().isEmpty(chatMessageContext.getProject())) {
            add(new FileListPanel(chatMessageContext));
        }

        if (chatMessageContext.getSemanticReferences() != null && !chatMessageContext.getSemanticReferences().isEmpty()) {
            add(new SemanticSearchReferencesPanel(chatMessageContext));
        }
        
        // Add MCP message panel if MCP is enabled
        if (MCPService.shouldShowMCPMessages()) {
            add(new MCPMessagePanel(chatMessageContext));
        }

        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowExecutionTime())) {
            add(new MetricExecutionInfoPanel(chatMessageContext));
        }
    }
}
