package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.ConversationStarter;
import com.devoxx.genie.ui.component.JHoverButton;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.CogIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.PlusIcon;
import static com.devoxx.genie.ui.util.TimestampUtil.getCurrentTimestamp;

@Getter
public class ConversationPanel extends JPanel {

    private final JButton newConversationBtn = new JHoverButton(PlusIcon, true);
    private final JButton configBtn = new JHoverButton(CogIcon, true);

    private final Project project;
    private final ConversationStarter conversationStarter;
    private final JLabel newConversationLabel;

    /**
     * The conversation panel constructor.
     * @param project the project
     * @param conversationStarter the conversation starter
     */
    public ConversationPanel(Project project, ConversationStarter conversationStarter) {
        super(new BorderLayout());
        this.project = project;
        this.conversationStarter = conversationStarter;

        setPreferredSize(new Dimension(0, 30));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        newConversationLabel = new JLabel("New conversation " + getCurrentTimestamp());
        newConversationLabel.setForeground(JBColor.GRAY);
        newConversationLabel.setPreferredSize(new Dimension(0, 30));

        setupConversationButtons();

        add(newConversationLabel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.EAST);
    }

    /**
     * Setup the conversation buttons.
     */
    private void setupConversationButtons() {
        newConversationBtn.setPreferredSize(new Dimension(25, 30));
        configBtn.setPreferredSize(new Dimension(25, 30));

        newConversationBtn.setToolTipText("Start a new conversation");
        configBtn.setToolTipText("Plugin settings");

        configBtn.addActionListener(e -> showSettingsDialog());
        newConversationBtn.addActionListener(e -> conversationStarter.startNewConversation());
    }

    /**
     * Create the button panel.
     * @return the button panel
     */
    private @NotNull JPanel createButtonPanel() {
        JPanel conversationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        conversationButtonPanel.add(newConversationBtn);
        conversationButtonPanel.add(configBtn);
        conversationButtonPanel.setPreferredSize(new Dimension(60, 30));
        conversationButtonPanel.setMinimumSize(new Dimension(60, 30));
        return conversationButtonPanel;
    }

    /**
     * Update the conversation label with new timestamp.
     */
    public void updateNewConversationLabel() {
        newConversationLabel.setText("New conversation " + getCurrentTimestamp());
    }

    /**
     * Show the settings dialog.
     */
    private void showSettingsDialog() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Devoxx Genie Settings");
    }
}
