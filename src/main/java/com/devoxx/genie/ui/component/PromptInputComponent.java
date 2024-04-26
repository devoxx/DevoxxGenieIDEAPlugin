package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;

public class PromptInputComponent extends JPanel {

    private final PlaceholderTextArea promptInputArea;
    private final PromptContextFileListPanel contextFilePanel;

    public PromptInputComponent(Project project) {
        super();

        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(0, getPreferredSize().height));

        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");

        // The prompt input area
        promptInputArea = new PlaceholderTextArea();
        promptInputArea.setLineWrap(true);
        promptInputArea.setWrapStyleWord(true);
        promptInputArea.setRows(3);
        promptInputArea.setAutoscrolls(false);
        promptInputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        promptInputArea.addFocusListener(new PromptInputFocusListener(promptInputArea));
        promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));
        this.add(promptInputArea, BorderLayout.CENTER);

        contextFilePanel = new PromptContextFileListPanel(project);
        this.add(contextFilePanel, BorderLayout.NORTH);
    }

    public List<VirtualFile> getFiles() {
        return contextFilePanel.getFiles();
    }

    public String getText() {
        return this.promptInputArea.getText();
    }

    public void setText(String text) {
        this.promptInputArea.setText(text);
    }
}
