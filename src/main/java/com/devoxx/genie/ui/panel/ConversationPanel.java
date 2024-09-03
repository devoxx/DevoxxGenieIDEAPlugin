package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.ConversationStorageService;
import com.devoxx.genie.ui.ConversationStarter;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.util.SettingsDialogUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;
import static com.devoxx.genie.ui.util.TimestampUtil.getCurrentTimestamp;

@Getter
public class ConversationPanel extends JPanel implements ConversationSelectionListener {

    private final JButton newConversationBtn = new JHoverButton(PlusIcon, true);
    private final JButton settingsBtn = new JHoverButton(CogIcon, true);
    private final JButton historyButton = new JHoverButton(ClockIcon, true);

    private final Project project;
    private final ConversationStarter conversationStarter;
    private final JLabel newConversationLabel;
    private final ConversationHistoryPanel historyPanel;
    private final PromptOutputPanel promptOutputPanel;

    /**
     * The conversation panel constructor.
     *
     * @param project             the project
     * @param conversationStarter the conversation starter
     * @param storageService      the storage service
     */
    public ConversationPanel(Project project,
                             ConversationStarter conversationStarter,
                             ConversationStorageService storageService,
                             PromptOutputPanel promptOutputPanel) {

        super(new BorderLayout());
        this.project = project;
        this.conversationStarter = conversationStarter;
        this.promptOutputPanel = promptOutputPanel;

        setPreferredSize(new Dimension(0, 30));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        newConversationLabel = new JLabel("New conversation " + getCurrentTimestamp());
        newConversationLabel.setForeground(JBColor.GRAY);
        newConversationLabel.setPreferredSize(new Dimension(0, 30));

        historyPanel = new ConversationHistoryPanel(storageService, this);

        setupConversationButtons();

        add(newConversationLabel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.EAST);
    }

    public void onConversationSelected(Conversation conversation) {
        promptOutputPanel.displayConversation(project, conversation);
    }

    public void loadConversationHistory() {
        historyPanel.loadConversations();
    }

    /**
     * Set up the conversation buttons.
     */
    private void setupConversationButtons() {
        historyButton.setToolTipText("View conversation history");
        historyButton.addActionListener(e -> showConversationHistory());

        settingsBtn.setPreferredSize(new Dimension(25, 30));
        settingsBtn.setToolTipText("Plugin settings");
        settingsBtn.addActionListener(e -> SettingsDialogUtil.showSettingsDialog(project));

        newConversationBtn.setPreferredSize(new Dimension(25, 30));
        newConversationBtn.setToolTipText("Start a new conversation");
        newConversationBtn.addActionListener(e -> conversationStarter.startNewConversation());
    }

    /**
     * Create the button panel.
     *
     * @return the button panel
     */
    private @NotNull JPanel createButtonPanel() {
        JPanel conversationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        conversationButtonPanel.add(newConversationBtn);
        conversationButtonPanel.add(historyButton);
        conversationButtonPanel.add(settingsBtn);
        conversationButtonPanel.setPreferredSize(new Dimension(90, 30));
        conversationButtonPanel.setMinimumSize(new Dimension(90, 30));
        return conversationButtonPanel;
    }

    /**
     * Update the conversation label with new timestamp.
     */
    public void updateNewConversationLabel() {
        newConversationLabel.setText("New conversation " + getCurrentTimestamp());
    }

    private void showConversationHistory() {
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(historyPanel, null)
            .setTitle("Conversation History")
            .setMovable(true)
            .setResizable(true)
            .setMinSize(new Dimension(500, 400))
            .createPopup();

        // Calculate the position for the popup
        int x = settingsBtn.getX() + settingsBtn.getWidth() - 500;
        int y = settingsBtn.getY() + settingsBtn.getHeight();

        // Convert to screen coordinates
        Point screenPoint = new Point(x, y);
        SwingUtilities.convertPointToScreen(screenPoint, settingsBtn.getParent());

        // Show the popup at the calculated position
        popup.show(new RelativePoint(screenPoint));
    }
}
