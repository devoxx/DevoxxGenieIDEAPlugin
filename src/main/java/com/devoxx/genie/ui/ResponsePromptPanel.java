package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.PromptContext;
import com.devoxx.genie.ui.component.ExpandablePanel;

import java.awt.*;

public class ResponsePromptPanel extends BackgroundPanel {

    public ResponsePromptPanel(PromptContext promptContext) {
        super(promptContext.getName());

        if (promptContext.getEditorInfo() != null &&
            promptContext.getEditorInfo().getSelectedFiles() != null &&
            !promptContext.getEditorInfo().getSelectedFiles().isEmpty()) {
            ExpandablePanel fileListPanel = new ExpandablePanel(promptContext);
            add(fileListPanel, BorderLayout.SOUTH);
        }
    }
}
