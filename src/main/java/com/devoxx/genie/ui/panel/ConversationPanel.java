package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatMemoryService;
import com.devoxx.genie.service.ChatService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.ConversationStarter;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.SubmitPanel;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.util.SettingsDialogUtil;
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
public class ConversationPanel extends JPanel implements ConversationSelectionListener, ConversationEventListener, ConversationStarter {

    private final DevoxxGenieToolWindowContent toolWindowContent;

    private final JButton newConversationBtn = new JHoverButton(PlusIcon, true);
    private final JButton settingsBtn = new JHoverButton(CogIcon, true);
    private final JButton historyButton = new JHoverButton(ClockIcon, true);

    private final Project project;
    private final JLabel newConversationLabel;
    private final ConversationHistoryPanel historyPanel;
    private final PromptOutputPanel promptOutputPanel;
    private final SubmitPanel submitPanel;
    private final JPanel conversationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    private JBPopup historyPopup;

    private final ChatService chatService;

    /**
     * The conversation panel constructor.
     *
     * @param toolWindowContent the tool window content
     */
    public ConversationPanel(DevoxxGenieToolWindowContent toolWindowContent) {
        super(new BorderLayout());

        this.toolWindowContent = toolWindowContent;
        this.project = toolWindowContent.getProject();
        this.promptOutputPanel = toolWindowContent.getPromptOutputPanel();

        setPreferredSize(new Dimension(0, 30));

        newConversationLabel = new JLabel("New conversation " + getCurrentTimestamp());
        newConversationLabel.setForeground(JBColor.GRAY);
        newConversationLabel.setPreferredSize(new Dimension(0, 30));

        historyPanel = new ConversationHistoryPanel(toolWindowContent.getStorageService(), this, project);

        setupConversationButtons();

        add(newConversationLabel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.EAST);

        chatService = new ChatService(toolWindowContent.getStorageService(), project);

        ApplicationManager.getApplication().getMessageBus().connect()
            .subscribe(LafManagerListener.TOPIC, (LafManagerListener) source -> updateFontSize());
        submitPanel = toolWindowContent.getSubmitPanel();
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
        newConversationBtn.addActionListener(e -> startNewConversation());

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

    @Override
    public void onNewConversation(ChatMessageContext chatMessageContext) {
        loadConversationHistory();
    }

    /**
     * Start a new conversation.
     * Clear the conversation panel, prompt input area, prompt output panel, file list and chat memory.
     */
    @Override
    public void startNewConversation() {
        FileListManager.getInstance().clear();
        ChatMemoryService.getInstance().clear(project);

        chatService.startNewConversation("");

        ApplicationManager.getApplication().invokeLater(() -> {
            updateNewConversationLabel();
            submitPanel.getPromptInputArea().clear();
            promptOutputPanel.clear();
            submitPanel.getActionButtonsPanel().resetProjectContext();
            submitPanel.getActionButtonsPanel().enableButtons();
            submitPanel.getActionButtonsPanel().resetTokenUsageBar();
            submitPanel.getPromptInputArea().requestFocusInWindow();
        });
    }
}
