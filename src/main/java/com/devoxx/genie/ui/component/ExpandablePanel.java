package com.devoxx.genie.ui.component;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.SemanticFile;
import com.intellij.openapi.project.Project;
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

    private boolean isExpanded = false;
    private final JButton toggleButton;
    private final JPanel contentPanel;

    public ExpandablePanel() {
        setLayout(new BorderLayout());

        andTransparent();
        withBackground(PROMPT_BG_COLOR);
        withBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        toggleButton = new JButton("File references", ArrowExpand);
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
        contentPanel.setVisible(isExpanded);

        JScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        add(scrollPane, BorderLayout.CENTER);

        if (isExpanded) {
            setMinimumSize(new Dimension(0, 300));
        }
    }

    public ExpandablePanel(Project project, @NotNull List<SemanticFile> semanticFiles) {
        this();
        toggleButton.setText("Referenced Files (" + semanticFiles.size() + ")");

        for (SemanticFile file : semanticFiles) {
            contentPanel.add(new FileEntryComponent(project, file));
        }
        contentPanel.setVisible(isExpanded);

        if (isExpanded) {
            setMinimumSize(new Dimension(0, Math.min(300, 30 * semanticFiles.size())));
        }
    }

    public ExpandablePanel(ChatMessageContext chatMessageContext,
                           @NotNull List<VirtualFile> files) {
        this();

        for (VirtualFile file : files) {
            contentPanel.add(new FileEntryComponent(chatMessageContext.getProject(), file, null));
        }
        contentPanel.setVisible(isExpanded);

        if (isExpanded) {
            setMinimumSize(new Dimension(0, Math.min(300, 30 * files.size())));
        }
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
