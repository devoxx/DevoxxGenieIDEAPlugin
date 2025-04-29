package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.util.SettingsDialogUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;
import static com.devoxx.genie.ui.util.TimestampUtil.getCurrentTimestamp;

/**
 * Manages UI components and interactions for the conversation panel.
 */
public class ConversationUIController implements CustomPromptChangeListener {

    private final ResourceBundle resourceBundle;
    private final MessageRenderer messageRenderer;
    private final ConversationManager conversationManager;
    private final ConversationHistoryManager historyManager;
    
    @Getter
    private final JLabel conversationLabel;
    
    @Getter
    private final JButton settingsButton;
    
    /**
     * Creates a new conversation UI controller.
     *
     * @param project The active project
     * @param resourceBundle The resource bundle
     * @param messageRenderer The message renderer
     * @param conversationManager The conversation manager
     * @param historyManager The history manager
     */
    public ConversationUIController(Project project, 
                                   ResourceBundle resourceBundle,
                                   MessageRenderer messageRenderer,
                                   ConversationManager conversationManager,
                                   ConversationHistoryManager historyManager) {
        this.resourceBundle = resourceBundle;
        this.messageRenderer = messageRenderer;
        this.conversationManager = conversationManager;
        this.historyManager = historyManager;
        
        // Initialize UI components
        this.conversationLabel = new JLabel("New conversation " + getCurrentTimestamp());
        this.settingsButton = createActionButton(CogIcon, e -> SettingsDialogUtil.showSettingsDialog(project));
    }

    /**
     * Create the button panel for the conversation panel.
     *
     * @return The button panel
     */
    public @NotNull JPanel createButtonPanel() {
        JPanel headerButtonsPanel = new JPanel(new BorderLayout());

        // Wrap the label in a panel to prevent it from being compressed too much
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.add(conversationLabel, BorderLayout.WEST);
        // Add some minimum size to ensure the label has space
        labelPanel.setMinimumSize(new Dimension(100, 30));
        headerButtonsPanel.add(labelPanel, BorderLayout.CENTER);

        // Use a fixed layout for buttons to prevent them from wrapping
        JPanel conversationButtonPanel = new JPanel();
        conversationButtonPanel.setLayout(new BoxLayout(conversationButtonPanel, BoxLayout.X_AXIS));
        conversationButtonPanel.add(createActionButton(PlusIcon, e -> conversationManager.startNewConversation()));
        conversationButtonPanel.add(Box.createHorizontalStrut(JBUI.scale(5)));
        conversationButtonPanel.add(createActionButton(ClockIcon, e -> historyManager.showConversationHistoryPopup(settingsButton)));
        conversationButtonPanel.add(Box.createHorizontalStrut(JBUI.scale(5)));
        conversationButtonPanel.add(settingsButton);

        headerButtonsPanel.add(conversationButtonPanel, BorderLayout.EAST);
        return headerButtonsPanel;
    }

    /**
     * Show the welcome content.
     */
    public void showWelcome() {
        if (resourceBundle != null) {
            messageRenderer.showWelcome(resourceBundle);
        }
    }

    /**
     * Clear the conversation content.
     */
    public void clear() {
        messageRenderer.clear();
        showWelcome();
    }
    
    /**
     * Clear the conversation content without showing the welcome message.
     * Used when restoring conversation history or submitting the first prompt.
     */
    public void clearWithoutWelcome() {
        messageRenderer.clear();
    }

    /**
     * Called when custom prompts change - updates the welcome content if it's visible.
     */
    @Override
    public void onCustomPromptsChanged() {
        if (resourceBundle != null) {
            messageRenderer.updateCustomPrompts(resourceBundle);
        }
    }
}
