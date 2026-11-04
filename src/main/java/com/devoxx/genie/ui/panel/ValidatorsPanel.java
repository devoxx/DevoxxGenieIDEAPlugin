package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.intellij.ui.components.JBPanel;
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

        // No scroll pane of its own: the rows are few, and the settings page already scrolls.
        add(validatorsContainer, BorderLayout.CENTER);
    }
}
