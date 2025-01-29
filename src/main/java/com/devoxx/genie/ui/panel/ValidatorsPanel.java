package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class ValidatorsPanel extends JBPanel<ValidatorsPanel> {

    public ValidatorsPanel(@NotNull List<ValidatorStatus> validatorStatuses,
                           ActionListener actionListener) {
        super(new BorderLayout());

        // Create panel to hold validator rows
        JPanel validatorsContainer = new JPanel();
        validatorsContainer.setLayout(new BoxLayout(validatorsContainer, BoxLayout.Y_AXIS));

        // Add each validator status panel to the container
        for (ValidatorStatus status : validatorStatuses) {
            ValidatorStatusPanel statusPanel = new ValidatorStatusPanel(status, actionListener);
            validatorsContainer.add(statusPanel);
        }

        JBScrollPane scrollPane = getScrollPane(validatorsContainer);

        add(scrollPane, BorderLayout.CENTER);
    }

    private static @NotNull JBScrollPane getScrollPane(JPanel validatorsContainer) {
        JBScrollPane scrollPane = new JBScrollPane(validatorsContainer);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }
}
