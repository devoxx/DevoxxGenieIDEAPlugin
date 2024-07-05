package com.devoxx.genie.ui.component;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.devoxx.genie.ui.util.DevoxxGenieColorsUtil.PROMPT_BG_COLOR;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.ArrowExpand;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.ArrowExpanded;

public class ExpandablePanel extends JBPanel<ExpandablePanel> {

    private boolean isExpanded = true;
    private final JButton toggleButton;
    private final JPanel contentPanel;

    public ExpandablePanel(ChatMessageContext chatMessageContext,
                           @NotNull List<VirtualFile> files) {
        setLayout(new BorderLayout());

        andTransparent();
        withBackground(PROMPT_BG_COLOR);
        withBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        int fileCount = files.size();
        String referenceText = "Using " + fileCount + " reference" + (fileCount > 1 ? "s" : "");

        toggleButton = new JButton(referenceText, ArrowExpand);
        toggleButton.addActionListener(e -> toggleContent());
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
        toggleButton.setOpaque(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorderPainted(false);
        add(toggleButton, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(PROMPT_BG_COLOR);

        isExpanded = files.size() <= 3;

        for (VirtualFile file : files) {
            contentPanel.add(new FileEntryComponent(chatMessageContext.getProject(), file, null));
        }
        contentPanel.setVisible(true);

        JScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(0, Math.min(300, 30 * fileCount)));
    }

    private void toggleContent() {
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        toggleButton.setIcon(isExpanded ? ArrowExpanded : ArrowExpand);

        contentPanel.getParent().revalidate();
        contentPanel.getParent().repaint();

        Rectangle rectangle = SwingUtilities.calculateInnerArea(contentPanel, contentPanel.getBounds());
        contentPanel.scrollRectToVisible(rectangle);
    }
}
