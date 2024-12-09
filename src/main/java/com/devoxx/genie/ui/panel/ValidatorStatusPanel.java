package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.rag.validator.ValidationActionType;
import com.devoxx.genie.service.rag.validator.ValidatorStatus;
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

    private static final int VERTICAL_PADDING = 1;
    private static final int HORIZONTAL_PADDING = 5;

    public ValidatorStatusPanel(@NotNull ValidatorStatus validatorStatus, ActionListener actionListener) {
        super(new BorderLayout(HORIZONTAL_PADDING, 0));

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING));

        // Left panel with icon and name
        JPanel leftPanel = createLeftPanel(validatorStatus);
        add(leftPanel, BorderLayout.WEST);

        // Center/right panel with message and action button
        JPanel rightPanel = createRightPanel(validatorStatus, actionListener);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel(ValidatorStatus validatorStatus) {
        JPanel leftPanel = new JPanel();
        // Using BoxLayout for better control over component spacing
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);

        // Status icon
        JBLabel statusIconLabel = new JBLabel(validatorStatus.isValid() ? "✓" : "✗");
        statusIconLabel.setFont(ICON_FONT);
        statusIconLabel.setForeground(validatorStatus.isValid() ? SUCCESS_COLOR : ERROR_COLOR);
        statusIconLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        leftPanel.add(statusIconLabel);
        leftPanel.add(Box.createHorizontalStrut(5));

        // Validator name
        if (validatorStatus.validatorType() != null) {
            JBLabel nameLabel = new JBLabel(validatorStatus.validatorType().getName());
            nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            leftPanel.add(nameLabel);
        }

        return leftPanel;
    }

    private JPanel createRightPanel(ValidatorStatus validatorStatus, ActionListener actionListener) {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        // Message label with proper vertical alignment
        JBLabel messageLabel = new JBLabel(validatorStatus.message());
        messageLabel.setForeground(validatorStatus.isValid() ? SUCCESS_COLOR : ERROR_COLOR);
        messageLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        rightPanel.add(messageLabel);

        // Add flexible space between message and button
        rightPanel.add(Box.createHorizontalGlue());

        // Action button if needed
        if (!validatorStatus.isValid() && validatorStatus.action() != ValidationActionType.OK) {
            JButton actionButton = createActionButton(validatorStatus, actionListener);
            rightPanel.add(Box.createHorizontalStrut(HORIZONTAL_PADDING));
            rightPanel.add(actionButton);
        }

        return rightPanel;
    }

    private JButton createActionButton(ValidatorStatus validatorStatus, ActionListener actionListener) {
        JButton actionButton = new JButton(getActionButtonText(validatorStatus.action()));
        actionButton.addActionListener(actionListener);
        actionButton.putClientProperty("validatorType", validatorStatus.validatorType());
        actionButton.putClientProperty("action", validatorStatus.action());
        return actionButton;
    }

    private String getActionButtonText(@NotNull ValidationActionType action) {
        return switch (action) {
            case PULL_CHROMA_DB -> "Pull Image";
            case START_CHROMA_DB -> "Start ChromaDB";
            default -> "Fix";
        };
    }
}