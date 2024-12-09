package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.devoxx.genie.service.rag.validator.ValidationActionType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ValidatorStatusPanel extends JBPanel<ValidatorStatusPanel> {
    private static final Logger LOG = Logger.getInstance(ValidatorStatusPanel.class);

    private static final Color SUCCESS_COLOR = new Color(0, 153, 51);  // Green
    private static final Color ERROR_COLOR = new Color(204, 0, 0);     // Red
    private static final Font ICON_FONT = new Font("Dialog", Font.BOLD, 14);

    public ValidatorStatusPanel(@NotNull ValidatorStatus validatorStatus, ActionListener actionListener) {
        super(new BorderLayout(10, 0));

        LOG.debug("Creating ValidatorStatusPanel for " +
                (validatorStatus.validatorType() == null ? "status validatorType is null" : validatorStatus.validatorType().getName()));

        setOpaque(false);

        // Status icon and validator name
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.setOpaque(false);

        // Status icon (✓ or ✗)
        JBLabel statusIcon = new JBLabel(validatorStatus.isValid() ? "✓" : "✗");
        statusIcon.setFont(ICON_FONT);
        statusIcon.setForeground(validatorStatus.isValid() ? SUCCESS_COLOR : ERROR_COLOR);
        leftPanel.add(statusIcon);

        // Validator name
        if (validatorStatus.validatorType() != null) {
            JBLabel nameLabel = new JBLabel(validatorStatus.validatorType().getName());
            leftPanel.add(nameLabel);
            add(leftPanel, BorderLayout.WEST);
        }

        // Message and action button panel
        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.setOpaque(false);

        // Error/success message
        JBLabel messageLabel = new JBLabel(validatorStatus.message());
        messageLabel.setForeground(validatorStatus.isValid() ? SUCCESS_COLOR : ERROR_COLOR);
        rightPanel.add(messageLabel, BorderLayout.CENTER);

        // Action button if needed
        if (!validatorStatus.isValid() && validatorStatus.action() != ValidationActionType.OK) {
            JButton actionButton = new JButton(getActionButtonText(validatorStatus.action()));
            actionButton.addActionListener(actionListener);
            actionButton.putClientProperty("validatorType", validatorStatus.validatorType());
            actionButton.putClientProperty("action", validatorStatus.action());
            rightPanel.add(actionButton, BorderLayout.EAST);
        }

        add(rightPanel, BorderLayout.CENTER);
    }

    private String getActionButtonText(@NotNull ValidationActionType action) {
        return switch (action) {
            case PULL_CHROMA_DB -> "Pull Image";
            case START_CHROMA_DB -> "Start ChromaDB";
            default -> "Fix";
        };
    }
}