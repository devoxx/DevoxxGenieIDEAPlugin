package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import org.jetbrains.annotations.NotNull;

public class SemanticSearchReferencesPanel extends ExpandablePanel {

    public SemanticSearchReferencesPanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getProject(), chatMessageContext.getSemanticReferences());
        setName(chatMessageContext.getId() + "_semantic");
    }
}
