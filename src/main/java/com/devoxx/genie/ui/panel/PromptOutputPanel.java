package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.HelpUtil;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

public class PromptOutputPanel extends JBPanel<PromptOutputPanel> {

    private final JPanel container = new JPanel();
    private final WelcomePanel welcomePanel;
    private final HelpPanel helpPanel;
    private final WaitingPanel waitingPanel = new WaitingPanel();
    private final JBScrollPane scrollPane;
    private final ResourceBundle resourceBundle;

    public PromptOutputPanel(ResourceBundle resourceBundle) {
        super();

        this.resourceBundle = resourceBundle;
        welcomePanel = new WelcomePanel(resourceBundle);
        helpPanel = new HelpPanel(HelpUtil.getHelpMessage(resourceBundle));

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        scrollPane = new JBScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        setMinimumSize(new Dimension(200, 200)); // Adjust these values as needed
        showWelcomeText();
    }

    public void clear() {
        container.removeAll();
        showWelcomeText();
        scrollToBottom();
    }

    public void showWelcomeText() {
        welcomePanel.showMsg();
        container.add(welcomePanel);
        scrollToBottom();
    }

    public void showHelpText() {
        container.remove(welcomePanel);
        container.add(helpPanel);
        scrollToBottom();
    }

    private void addFiller(String name) {
        container.add(new FillerPanel(name));
    }

    public void addUserPrompt(ChatMessageContext chatMessageContext) {
        container.remove(welcomePanel);

        UserPromptPanel userPromptPanel = new UserPromptPanel(container, chatMessageContext);

        if (!DevoxxGenieStateService.getInstance().getStreamMode()) {
            waitingPanel.showMsg();
            userPromptPanel.add(waitingPanel, BorderLayout.SOUTH);
        }

        addFiller(chatMessageContext.getName());
        container.add(userPromptPanel);
        scrollToBottom();
    }

    public void addChatResponse(@NotNull ChatMessageContext chatMessageContext) {
        waitingPanel.hideMsg();
        addFiller(chatMessageContext.getName());
        container.add(new ChatResponsePanel(chatMessageContext));
        scrollToBottom();
    }

    public void addStreamResponse(ChatStreamingResponsePanel chatResponseStreamingPanel) {
        container.add(chatResponseStreamingPanel);
        scrollToBottom();
    }

    public void addStreamFileReferencesResponse(ExpandablePanel fileListPanel) {
        container.add(fileListPanel);
        scrollToBottom();
    }

    public void removeLastUserPrompt(ChatMessageContext chatMessageContext) {
        for (Component component : container.getComponents()) {
            if (component instanceof UserPromptPanel && component.getName().equals(chatMessageContext.getName())) {
                container.remove(component);
                break;
            }
        }
        revalidate();
        repaint();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            Timer timer = new Timer(100, e -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    public void updateHelpText() {
        helpPanel.updateHelpText(HelpUtil.getHelpMessage(resourceBundle));
    }
}
