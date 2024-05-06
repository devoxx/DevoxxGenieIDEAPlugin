package com.devoxx.genie.ui.component;

import com.devoxx.genie.model.request.PromptContext;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieColors.PROMPT_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieFonts.SourceCodeProFontPlan12;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.ArrowExpand;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.ArrowExpanded;

public class ExpandablePanel extends JPanel {

    private boolean isExpanded = false;
    private final JButton toggleButton;
    private final JPanel contentPanel;

    public ExpandablePanel(PromptContext promptContext) {
        setLayout(new BorderLayout());
        setSize(300, 200);
        setOpaque(false);
        setBackground(PROMPT_BG_COLOR);
        List<VirtualFile> selectedFiles = promptContext.getEditorInfo().getSelectedFiles();
        String referenceText = "Using " + selectedFiles.size() + " reference" + (selectedFiles.size() > 1 ? "s" : "");
        toggleButton = new JButton(referenceText, ArrowExpand);
        // toggleButton.setFont(SourceCodeProFontPlan12);
        toggleButton.addActionListener(e -> toggleContent());
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
        toggleButton.setOpaque(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorderPainted(false);
        add(toggleButton, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        selectedFiles.forEach(file -> contentPanel.add(new FileEntryComponent(promptContext.getProject(), file, null, SourceCodeProFontPlan12)));
        contentPanel.setVisible(false);

        setMinimumSize(new Dimension(0, 14 * selectedFiles.size()));
        add(contentPanel, BorderLayout.CENTER);
    }

    private void toggleContent() {
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        toggleButton.setIcon(isExpanded ? ArrowExpanded : ArrowExpand);
    }
}
