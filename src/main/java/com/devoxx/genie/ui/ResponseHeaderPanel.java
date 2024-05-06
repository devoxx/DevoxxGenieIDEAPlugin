package com.devoxx.genie.ui;

import com.devoxx.genie.model.request.PromptContext;
import com.devoxx.genie.ui.component.JHoverButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.TrashIcon;

public class ResponseHeaderPanel extends JBPanel<ResponseHeaderPanel> {

    private final JComponent container;

    public ResponseHeaderPanel(PromptContext promptContext,
                               JComponent container) {
        super(new BorderLayout());
        this.container = container;

        andTransparent()
        .withMaximumSize(500, 30)
        .withPreferredHeight(30)
        .withPreferredWidth(500);

        String modelInfo = (promptContext.getLlmProvider() != null ? promptContext.getLlmProvider() : "") +
            (promptContext.getModelName() != null ? " - " + promptContext.getModelName() : "");

        String label = promptContext.getCreatedOn().format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")) + " : " + modelInfo;
        JBLabel createdOnLabel = new JBLabel(label);
        createdOnLabel.setFont(createdOnLabel.getFont().deriveFont(12f));
        createdOnLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        add(createdOnLabel, BorderLayout.WEST);

        if (container != null) {
            JButton deleteButton = getDeleteButton(promptContext.getName());
            add(deleteButton, BorderLayout.EAST);
        }
    }

    private @NotNull JButton getDeleteButton(String name) {
        JButton deleteButton = new JHoverButton(TrashIcon, true);
        deleteButton.setToolTipText("Remove the prompt & response");
        deleteButton.addActionListener(e -> removeComponent(name));
        return deleteButton;
    }

    private void removeComponent(String name) {
        // Get all children of container and delete by name
        Arrays.stream(container.getComponents())
            .filter(c -> c.getName() != null && c.getName().equals(name))
            .forEach(container::remove);

        container.revalidate();
        container.repaint();

        // Delete from message list
    }
}
