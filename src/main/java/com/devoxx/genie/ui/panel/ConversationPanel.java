package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.listener.ConversationEventListener;
import com.devoxx.genie.ui.listener.ConversationSelectionListener;
import com.devoxx.genie.ui.listener.ConversationStarter;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.panel.conversationhistory.ConversationHistoryPanel;
import com.devoxx.genie.ui.util.SettingsDialogUtil;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;
import static com.devoxx.genie.ui.util.TimestampUtil.getCurrentTimestamp;

/**
 * A panel containing a single WebView that displays the entire conversation.
 * This replaces the previous approach of creating separate panels for each message.
 */
@Slf4j
public class ConversationPanel
        extends JBPanel<ConversationPanel>
        implements CustomPromptChangeListener, ConversationSelectionListener, ConversationEventListener, ConversationStarter {

    private final ConversationWebViewController webViewController = new ConversationWebViewController();

    private final Project project;
    private final ResourceBundle resourceBundle;
    private JButton settingsBtn;
    private final ConversationHistoryPanel historyPanel;
    private final ChatService chatService;
    private JLabel conversationLabel;

    /**
     * Creates a new conversation panel.
     *
     * @param resourceBundle The resource bundle for i18n
     */
    public ConversationPanel(Project project, ResourceBundle resourceBundle) {
        super(new BorderLayout());

        this.project = project;
        this.resourceBundle = resourceBundle;

        add(createButtonPanel(), BorderLayout.NORTH);

        historyPanel = new ConversationHistoryPanel(project);
        chatService = new ChatService(project);

        // Create the WebViewController for managing the conversation content
        JBCefBrowser browser = webViewController.getBrowser();

        JComponent browserComponent = browser.getComponent();
        browserComponent.setOpaque(true);
        browserComponent.setBackground(Color.BLACK);
        
        // Set browser layout and sizing
        add(browserComponent, BorderLayout.CENTER);
        
        // Set sizes for the panel to ensure proper display
        setMinimumSize(new Dimension(400, 300));
        setPreferredSize(new Dimension(800, 600));
        
        // Ensure the component is visible
        setOpaque(true);
        setBackground(Color.BLACK);
        setVisible(true);
    }

    /**
     * Show the welcome content.
     */
    public void showWelcome() {
        webViewController.loadWelcomeContent(resourceBundle);
    }

    /**
     * Add a chat message to the conversation.
     *
     * @param chatMessageContext The chat message context
     */
    public void addChatMessage(ChatMessageContext chatMessageContext) {
        webViewController.addChatMessage(chatMessageContext);
    }

    /**
     * Clear the conversation content.
     */
    public void clear() {
        webViewController.clearConversation();
        showWelcome();
    }

    public void loadConversationHistory() {
        historyPanel.loadConversations();
    }

    /**
     * Called when custom prompts change - updates the welcome content if it's visible.
     */
    @Override
    public void onCustomPromptsChanged() {
        webViewController.updateCustomPrompts(resourceBundle);
    }

    /**
     * Create the button panel.
     *
     * @return the button panel
     */
    private @NotNull JPanel createButtonPanel() {
        JPanel headerButtonsPanel = new JPanel();

        conversationLabel = new JLabel("New conversation " + getCurrentTimestamp());

        headerButtonsPanel.add(conversationLabel, BorderLayout.WEST);

        JPanel conversationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(10), 0));
        conversationButtonPanel.add(createActionButton(PlusIcon, e -> startNewConversation()));
        conversationButtonPanel.add(createActionButton(ClockIcon, e -> showConversationHistory()));

        settingsBtn = createActionButton(CogIcon, e -> SettingsDialogUtil.showSettingsDialog(project));
        conversationButtonPanel.add(settingsBtn);

        headerButtonsPanel.add(conversationButtonPanel, BorderLayout.EAST);
        return headerButtonsPanel;
    }

    /**
     * Update the conversation label with new timestamp.
     */
    public void updateNewConversationLabel() {
        conversationLabel.setText("New conversation " + getCurrentTimestamp());
    }

    private void showConversationHistory() {
        JBPopup historyPopup = JBPopupFactory.getInstance()
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
        FileListManager.getInstance().clear(project);
        ChatMemoryService.getInstance().clearMemory(project);

        chatService.startNewConversation("");

        ApplicationManager.getApplication().invokeLater(() -> {
            updateNewConversationLabel();

            // TODO Clear conversation
//            submitPanel.getPromptInputArea().clear();
//            promptOutputPanel.clear();
//            submitPanel.getActionButtonsPanel().resetProjectContext();
//            submitPanel.getActionButtonsPanel().enableButtons();
//            submitPanel.getActionButtonsPanel().resetTokenUsageBar();
//            submitPanel.getPromptInputArea().requestFocusInWindow();
        });
    }

    @Override
    public void onConversationSelected(Conversation conversation) {
        // TODO
    }
}
