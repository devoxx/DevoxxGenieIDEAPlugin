package com.devoxx.genie.ui.component;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.ResourceBundle;

public class PromptInputComponent extends JPanel {

    private final PlaceholderTextArea promptInputArea;
    private final PromptContextFileListPanel contextFilePanel;

    public PromptInputComponent(Project project) {
        super();

        contextFilePanel = new PromptContextFileListPanel(project);
        setLayout(new BorderLayout());

        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));

        // The prompt input area
        this.promptInputArea = new PlaceholderTextArea();
        this.promptInputArea.setLineWrap(true);
        this.promptInputArea.setWrapStyleWord(true);
        this.promptInputArea.setRows(3);
        this.promptInputArea.setAutoscrolls(false);
        this.promptInputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.add(promptInputArea, BorderLayout.CENTER);

        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
        this.promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

        // Instead of adding filesPanel directly, add the JScrollPane
        this.add(contextFilePanel, BorderLayout.NORTH);

        promptInputArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                promptInputArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new JBColor(new Color(37, 150, 190), new Color(37, 150, 190))),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));

                promptInputArea.requestFocusInWindow();
            }

            @Override
            public void focusLost(FocusEvent e) {
                promptInputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        });
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
