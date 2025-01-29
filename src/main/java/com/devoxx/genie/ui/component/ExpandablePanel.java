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

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
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

        toggleButton = createActionButton("File references", ArrowExpand, e -> toggleContent());
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
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

        add(scrollPane, BorderLayout.CENTER);
    }

    public ExpandablePanel(Project project, @NotNull List<SemanticFile> semanticFiles) {
        this();
        toggleButton.setText("Referenced Files (" + semanticFiles.size() + ")");

        for (SemanticFile file : semanticFiles) {
            contentPanel.add(new FileEntryComponent(project, file));
        }
        contentPanel.setVisible(isExpanded);
    }

    public ExpandablePanel(ChatMessageContext chatMessageContext,
                           @NotNull List<VirtualFile> files) {
        this();

        toggleButton.setText("Referenced Files (" + files.size() + ")");

        for (VirtualFile file : files) {
            contentPanel.add(new FileEntryComponent(chatMessageContext.getProject(), file, null));
        }
        contentPanel.setVisible(isExpanded);
    }

    private void toggleContent() {
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        toggleButton.setIcon(isExpanded ? ArrowExpanded : ArrowExpand);

        // Recompute the size of the panel
        revalidate();
        repaint();

        // Update the parent container
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent.repaint();
            parent = parent.getParent();
        }

        if (isExpanded) {
            Rectangle rectangle = SwingUtilities.calculateInnerArea(contentPanel, contentPanel.getBounds());
            contentPanel.scrollRectToVisible(rectangle);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        if (isExpanded) {
            int contentHeight = contentPanel.getComponentCount() * 30;
            return new Dimension(0, Math.min(300, contentHeight));
        } else {
            return new Dimension(0, toggleButton.getPreferredSize().height + 10);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isExpanded) {
            int contentHeight = contentPanel.getComponentCount() * 30;
            return new Dimension(super.getPreferredSize().width, Math.min(300, contentHeight + toggleButton.getPreferredSize().height + 10));
        } else {
            return new Dimension(super.getPreferredSize().width, toggleButton.getPreferredSize().height + 10);
        }
    }

    @Override
    public Dimension getMaximumSize() {
        if (isExpanded) {
            int contentHeight = contentPanel.getComponentCount() * 30;
            return new Dimension(Integer.MAX_VALUE, Math.min(300, contentHeight + toggleButton.getPreferredSize().height + 10));
        } else {
            return new Dimension(Integer.MAX_VALUE, toggleButton.getPreferredSize().height + 10);
        }
    }
}
