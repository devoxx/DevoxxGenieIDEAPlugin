package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.ConversationStorageService;
import com.devoxx.genie.ui.ConversationStarter;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.util.SettingsDialogUtil;
import com.devoxx.genie.ui.util.WelcomeUtil;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
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
    private final JPanel conversationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private JBPopup historyPopup;

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

        newConversationLabel = new JLabel("New conversation " + getCurrentTimestamp());
        newConversationLabel.setForeground(JBColor.GRAY);
        newConversationLabel.setPreferredSize(new Dimension(0, 30));

        historyPanel = new ConversationHistoryPanel(storageService, this, project);

        setupConversationButtons();

        add(newConversationLabel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.EAST);

        ApplicationManager.getApplication().getMessageBus().connect()
            .subscribe(LafManagerListener.TOPIC, (LafManagerListener) source -> updateFontSize());
    }

    private void updateFontSize() {
        int fontSize = (int)JBUIScale.scale(14f) + 6;
        settingsBtn.setPreferredSize(new Dimension(fontSize, 30));
        historyButton.setPreferredSize(new Dimension(fontSize, 30));
        newConversationBtn.setPreferredSize(new Dimension(fontSize, 30));

        setMaximumSize(new Dimension(fontSize * 3, 30));

        conversationButtonPanel.setPreferredSize(new Dimension(fontSize * 3, 30));
        conversationButtonPanel.setMinimumSize(new Dimension(fontSize * 3, 30));

        revalidate();
        repaint();
    }

    public void onConversationSelected(Conversation conversation) {
        if (historyPopup != null && historyPopup.isVisible()) {
            historyPopup.closeOk(null);
        }
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

        settingsBtn.setToolTipText("Plugin settings");
        settingsBtn.addActionListener(e -> SettingsDialogUtil.showSettingsDialog(project));

        newConversationBtn.setToolTipText("Start a new conversation");
        newConversationBtn.addActionListener(e -> conversationStarter.startNewConversation());

        updateFontSize();
    }

    /**
     * Create the button panel.
     *
     * @return the button panel
     */
    private @NotNull JPanel createButtonPanel() {
        conversationButtonPanel.add(newConversationBtn);
        conversationButtonPanel.add(historyButton);
        conversationButtonPanel.add(settingsBtn);
        return conversationButtonPanel;
    }

    /**
     * Update the conversation label with new timestamp.
     */
    public void updateNewConversationLabel() {
        newConversationLabel.setText("New conversation " + getCurrentTimestamp());
    }

    private void showConversationHistory() {
        historyPopup = JBPopupFactory.getInstance()
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
        historyPopup.show(new RelativePoint(screenPoint));
    }
}
