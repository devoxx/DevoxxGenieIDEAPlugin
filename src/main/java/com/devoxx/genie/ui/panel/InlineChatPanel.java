package com.devoxx.genie.ui.panel;

import com.devoxx.genie.controller.ActionButtonsPanelController;
import com.devoxx.genie.controller.ProjectContextController;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.input.InlineChatPromptInputArea;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentContainer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.IconUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.devoxx.genie.model.Constant.MESSAGES;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.SubmitIcon;

public class InlineChatPanel extends JPanel implements PromptSubmissionListener, Disposable {

    private final ResourceBundle resourceBundle = ResourceBundle.getBundle(MESSAGES);
    private final MessageBusConnection messageBusConnection;
    private JButton submitButton;
    private Project project;
    private InlineChatPromptInputArea inlineChatPromptInputArea;
    private  ActionButtonsPanelController controller;
    private ProjectContextController projectContextController;
    private JBPopup popup;

    // Empty constructor for use by the Action
    public InlineChatPanel() {
        setLayout(new BorderLayout());
        messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        messageBusConnection.subscribe(AppTopics.PROMPT_SUBMISSION_TOPIC, this);
    }

    public void initialize(@NotNull AnActionEvent e) {
        this.project = Objects.requireNonNull(e.getProject(), "Project cannot be null");

        DevoxxGenieToolWindowContent devoxxGenieToolWindowContent = getDevoxxGenieToolWindowContent(e);

        this.inlineChatPromptInputArea = new InlineChatPromptInputArea(project, resourceBundle);
        this.inlineChatPromptInputArea.setPreferredSize(new Dimension(400, 25));

        this.controller = new ActionButtonsPanelController(project, inlineChatPromptInputArea,
                devoxxGenieToolWindowContent.getConversationPanel().getPromptOutputPanel(),
                devoxxGenieToolWindowContent.getLlmProviderPanel().getModelProviderComboBox(),
                devoxxGenieToolWindowContent.getLlmProviderPanel().getModelNameComboBox(),
                devoxxGenieToolWindowContent.getConversationPanel().getSubmitPanel().getActionButtonsPanel());
        this.projectContextController = new ProjectContextController(project, devoxxGenieToolWindowContent.getLlmProviderPanel().getModelProviderComboBox(),
                devoxxGenieToolWindowContent.getLlmProviderPanel().getModelNameComboBox(),
                devoxxGenieToolWindowContent.getConversationPanel().getSubmitPanel().getActionButtonsPanel());

        setupUI(e);
        setupAccessibility();
    }

    private @NotNull DevoxxGenieToolWindowContent getDevoxxGenieToolWindowContent(@NotNull AnActionEvent e) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(Objects.requireNonNull(e.getProject()));
        ToolWindow toolWindow = Objects.requireNonNull(toolWindowManager.getToolWindow(Constant.TOOL_WINDOW_ID), "Tool window not found");
        return (DevoxxGenieToolWindowContent) Optional.ofNullable(toolWindow.getContentManager().getContent(0))
                .map(ComponentContainer::getComponent)
                .orElseThrow(() -> new IllegalStateException("Tool window content not found"));
    }

    private void setupUI(AnActionEvent e) {
        JPanel contentPanel = createContentPanel();
        this.popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(contentPanel, inlineChatPromptInputArea)
                .setTitle(getTitleLabel().getText())
                .setResizable(true)
                .setMovable(true)
                .setFocusable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup();
        popup.getContent().setBorder(JBUI.Borders.empty());
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIManager.getColor("ToolTip.background"));
        Border border = BorderFactory.createLineBorder(UIManager.getColor("ToolTip.borderColor"), 1);
        panel.setBorder(JBUI.Borders.merge(border, JBUI.Borders.empty(5), true));

        JPanel inputPanel = new JPanel(new BorderLayout());
        JLabel iconLabel = new JLabel(IconUtil.scale(DevoxxGenieIconsUtil.DevoxxIcon, null, 1.25f));
        inputPanel.add(iconLabel, BorderLayout.WEST);
        inputPanel.add(this.inlineChatPromptInputArea, BorderLayout.CENTER);
        inputPanel.add(createSubmitButton(), BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.NORTH);

        return panel;
    }

    private JLabel getTitleLabel() {
        JLabel titleLabel = new JLabel("Axa Ai Secure Gpt");
        titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(9f));
        return titleLabel;
    }

    private @NotNull JButton createSubmitButton() {
        Icon scaledIcon = IconUtil.scale(SubmitIcon, null, 1f);
        submitButton = new JHoverButton(scaledIcon, false);
        submitButton.setActionCommand(Constant.SUBMIT_ACTION);
        submitButton.addActionListener(this::onSubmitPrompt);

        Dimension iconSize = new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight());
        submitButton.setPreferredSize(iconSize);
        submitButton.setMinimumSize(iconSize);
        submitButton.setMaximumSize(iconSize);

        submitButton.setBorderPainted(false);
        submitButton.setContentAreaFilled(false);
        submitButton.setFocusPainted(false);
        submitButton.setOpaque(false);

        return submitButton;
    }

    @Override
    public void onPromptSubmitted(@NotNull Project projectPrompt, String prompt) {
        if (!this.project.getName().equals(projectPrompt.getName())) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            inlineChatPromptInputArea.setText(prompt);
            onSubmitPrompt(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Constant.SUBMIT_ACTION));
        });
    }

    private void onSubmitPrompt(ActionEvent actionEvent) {
        if (controller.isPromptRunning()) {
            controller.stopPromptExecution();
            return;
        }
        handlePromptSubmission(actionEvent.getActionCommand());
    }

    private void handlePromptSubmission(String actionCommand) {
        boolean response = controller.handlePromptSubmission(actionCommand,
                projectContextController.isProjectContextAdded(),
                projectContextController.getProjectContext());

        if (!response) {
            controller.endPromptExecution();
        }
    }

    public void showPopup(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        if (popup == null || popup.isDisposed()) {
            setupUI(e);
        }
        popup.show(calculatePopupPosition(editor));
    }

    private RelativePoint calculatePopupPosition(Editor editor) {
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        Point caretPosition = editor.visualPositionToXY(caret.getVisualPosition());
        return new RelativePoint(editor.getContentComponent(), caretPosition);
    }

    private void setupAccessibility() {
        submitButton.getAccessibleContext().setAccessibleDescription("Submit prompt to AI");
        submitButton.setMnemonic('S');
    }

    @Override
    public void dispose() {
        Disposer.dispose(popup);
        messageBusConnection.disconnect();
    }
}
