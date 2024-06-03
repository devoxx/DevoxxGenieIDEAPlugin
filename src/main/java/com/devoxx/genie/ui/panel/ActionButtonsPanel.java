package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.ChatPromptExecutor;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.ui.DevoxxGenieToolWindowContent;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.component.ContextPopupMenu;
import com.devoxx.genie.ui.component.JHoverButton;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.settings.llm.LLMStateService;
import com.devoxx.genie.ui.settings.llmconfig.LLMConfigStateService;
import com.devoxx.genie.ui.util.EditorUtil;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.UserMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;
import static com.devoxx.genie.model.Constant.ADD_FILE_S_TO_PROMPT_CONTEXT;
import static com.devoxx.genie.ui.util.DevoxxGenieIcons.*;
import static com.devoxx.genie.ui.util.SettingsDialogUtil.showSettingsDialog;
import static javax.swing.SwingUtilities.invokeLater;

public class ActionButtonsPanel extends JPanel {
    private final Project project;

    private final ChatPromptExecutor chatPromptExecutor;
    private final EditorFileButtonManager editorFileButtonManager;

    private final JButton addFileBtn = new JHoverButton(AddFileIcon, true);
    private final JButton submitBtn = new JHoverButton(SubmitIcon, true);
    private final JButton tavilySearchBtn = new JHoverButton(WebSearchIcon, true);
    private final JButton googleSearchBtn = new JHoverButton(GoogleIcon, true);

    private final PromptInputArea promptInputComponent;
    private final PromptOutputPanel promptOutputPanel;
    private final ComboBox<String> llmProvidersComboBox;
    private final ComboBox<String> modelNameComboBox;
    private final DevoxxGenieToolWindowContent devoxxGenieToolWindowContent;
    private final ChatModelProvider chatModelProvider = new ChatModelProvider();

    public ActionButtonsPanel(Project project,
                              PromptInputArea promptInputComponent,
                              PromptOutputPanel promptOutputPanel,
                              ComboBox<String> llmProvidersComboBox,
                              ComboBox<String> modelNameComboBox,
                              DevoxxGenieToolWindowContent devoxxGenieToolWindowContent) {
        setLayout(new BorderLayout());

        this.project = project;
        this.promptInputComponent = promptInputComponent;
        this.promptOutputPanel = promptOutputPanel;
        this.chatPromptExecutor = new ChatPromptExecutor();
        this.editorFileButtonManager = new EditorFileButtonManager(project, addFileBtn);
        this.llmProvidersComboBox = llmProvidersComboBox;
        this.modelNameComboBox = modelNameComboBox;
        this.devoxxGenieToolWindowContent = devoxxGenieToolWindowContent;

        setupUI();

        configureSearchButtonsVisibility();
    }
    /**
     * Create the Action button panel with Submit, the Web Search and Add file buttons.
     */
    public void setupUI() {

        submitBtn.setToolTipText(SUBMIT_THE_PROMPT);
        submitBtn.setActionCommand(Constant.SUBMIT_ACTION);
        submitBtn.addActionListener(this::onSubmitPrompt);
        add(submitBtn, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout());
        createSearchButton(searchPanel, tavilySearchBtn, TAVILY_SEARCH_ACTION, SEARCH_THE_WEB_WITH_TAVILY_FOR_AN_ANSWER);
        createSearchButton(searchPanel, googleSearchBtn, GOOGLE_SEARCH_ACTION, SEARCH_GOOGLE_FOR_AN_ANSWER);
        add(searchPanel, BorderLayout.CENTER);

        addFileBtn.setToolTipText(ADD_FILE_S_TO_PROMPT_CONTEXT);
        addFileBtn.addActionListener(this::selectFilesForPromptContext);

        add(addFileBtn, BorderLayout.EAST);
    }

    /**
     * Create the search button.
     * @param panel the panel
     * @param searchBtn the search button
     * @param searchAction the search action
     * @param tooltipText the tooltip text
     */
    private void createSearchButton(@NotNull JPanel panel,
                                    @NotNull JButton searchBtn,
                                    String searchAction,
                                    String tooltipText) {
        searchBtn.setMaximumSize(new Dimension(30, 30));
        searchBtn.setActionCommand(searchAction);
        searchBtn.setToolTipText(tooltipText);
        searchBtn.addActionListener(this::onSubmitPrompt);
        panel.add(searchBtn);
    }

    /**
     * Add files to the prompt context.
     */
    private void selectFilesForPromptContext(ActionEvent e) {
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(FileSelectionPanelFactory.createPanel(project), null)
            .setTitle("Double-Click To Add To Prompt Context")
            .setRequestFocus(true)
            .setResizable(true)
            .setMovable(false)
            .createPopup();

        if (addFileBtn.isShowing()) {
            new ContextPopupMenu().show(submitBtn,
                popup,
                devoxxGenieToolWindowContent.getContentPanel().getSize().width,
                promptInputComponent.getLocationOnScreen().y);
        }
    }

    /**
     * Submit the user prompt.
     */
    private void onSubmitPrompt(ActionEvent actionEvent) {
        String userPromptText = isUserPromptProvided();
        if (userPromptText == null) return;

        if (isWebSearchTriggeredAndConfigured(actionEvent)) return;

        disableSubmitBtn();

        ChatMessageContext chatMessageContext =
            createChatMessageContext(actionEvent, userPromptText, editorFileButtonManager.getSelectedTextEditor());

        disableButtons();

        chatPromptExecutor.updatePromptWithCommandIfPresent(chatMessageContext, promptOutputPanel);
        chatPromptExecutor.executePrompt(chatMessageContext, promptOutputPanel, this::enableButtons);
    }

    /**
     * Enable the prompt input component and reset the Submit button icon.
     */
    public void enableButtons() {
        submitBtn.setIcon(SubmitIcon);
        submitBtn.setEnabled(true);
        submitBtn.setToolTipText(SUBMIT_THE_PROMPT);
        promptInputComponent.setEnabled(true);
    }

    /**
     * Disable the Submit button.
     */
    private void disableSubmitBtn() {
        invokeLater(() -> {
            if (LLMStateService.getInstance().getStreamMode()) {
                submitBtn.setEnabled(false);
            }
            submitBtn.setIcon(StopIcon);
            submitBtn.setToolTipText(PROMPT_IS_RUNNING_PLEASE_BE_PATIENT);
        });
    }

    /**
     * Check if web search is triggered and configured, if not show Settings page.
     * @param actionEvent the action event
     * @return true if the web search is triggered and configured
     */
    private boolean isWebSearchTriggeredAndConfigured(@NotNull ActionEvent actionEvent) {
        if (actionEvent.getActionCommand().toLowerCase().contains("search") && !isWebSearchEnabled()) {
            SwingUtilities.invokeLater(() ->
                NotificationUtil.sendNotification(project, "No Search API keys found, please add one in the settings.")
            );
            showSettingsDialog(project);
            return true;
        }
        return false;
    }

    /**
     * Check if the user prompt is provided.
     * @return the user prompt text
     */
    private @Nullable String isUserPromptProvided() {
        String userPromptText = promptInputComponent.getText();
        if (userPromptText.isEmpty()) {
            NotificationUtil.sendNotification(project, "Please enter a prompt.");
            return null;
        }
        return userPromptText;
    }

    /**
     * Get the chat message context.
     * @param actionEvent the action event
     * @param userPrompt the user prompt
     * @param editor     the editor
     * @return the prompt context with language and text
     */
    private @NotNull ChatMessageContext createChatMessageContext(ActionEvent actionEvent,
                                                                 String userPrompt,
                                                                 Editor editor) {
        ChatMessageContext chatMessageContext = new ChatMessageContext();
        chatMessageContext.setProject(project);
        chatMessageContext.setName(String.valueOf(System.currentTimeMillis()));
        chatMessageContext.setUserPrompt(userPrompt);
        chatMessageContext.setUserMessage(UserMessage.userMessage(userPrompt));
        chatMessageContext.setLlmProvider((String) llmProvidersComboBox.getSelectedItem());
        chatMessageContext.setModelName((String) modelNameComboBox.getSelectedItem());

        if (LLMStateService.getInstance().getStreamMode() && actionEvent.getActionCommand().equals(Constant.SUBMIT_ACTION)) {
            chatMessageContext.setStreamingChatLanguageModel(chatModelProvider.getStreamingChatLanguageModel(chatMessageContext));
        } else {
            chatMessageContext.setChatLanguageModel(chatModelProvider.getChatLanguageModel(chatMessageContext));
        }

        setChatTimeout(chatMessageContext);

        if (actionEvent.getActionCommand().equals(Constant.SUBMIT_ACTION)) {
            addSelectedCodeSnippet(userPrompt, editor, chatMessageContext);
        } else {
            chatMessageContext.setContext(actionEvent.getActionCommand());
        }

        return chatMessageContext;
    }

    /**
     * Add the selected code snippet to the chat message context.
     * @param userPrompt         the user prompt
     * @param editor             the editor
     * @param chatMessageContext the chat message context
     */
    private void addSelectedCodeSnippet(String userPrompt,
                                        Editor editor,
                                        ChatMessageContext chatMessageContext) {
        List<VirtualFile> files = FileListManager.getInstance().getFiles();
        if (!files.isEmpty()) {
            addSelectedFiles(chatMessageContext, userPrompt, files);
        } else if (editor != null) {
            EditorInfo editorInfo = createEditorInfo(editor);
            chatMessageContext.setEditorInfo(editorInfo);
        }
    }

    /**
     * Create the editor info.
     * @param editor the editor
     * @return the editor info
     */
    private @NotNull EditorInfo createEditorInfo(Editor editor) {
        EditorInfo editorInfo = EditorUtil.getEditorInfo(project, editor);
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText != null) {
            editorInfo.setSelectedText(selectedText);
        } else {
            editorInfo.setSelectedText(editor.getDocument().getText());
            editorInfo.setSelectedFiles(List.of(editor.getVirtualFile()));
        }
        return editorInfo;
    }

    /**
     * Set the timeout for the chat message context.
     * @param chatMessageContext the chat message context
     */
    private void setChatTimeout(ChatMessageContext chatMessageContext) {
        Integer timeout = LLMConfigStateService.getInstance().getTimeout();
        if (timeout == 0) {
            chatMessageContext.setTimeout(60);
        } else {
            chatMessageContext.setTimeout(timeout);
        }
    }

    /**
     * Check if web search is enabled.
     * @return true if web search is enabled
     */
    private boolean isWebSearchEnabled() {
        return !LLMStateService.getInstance().getTavilySearchKey().isEmpty() ||
               !LLMStateService.getInstance().getGoogleSearchKey().isEmpty();
    }

    /**
     * Get the prompt context from the selected files.
     * @param chatMessageContext the chat message context
     * @param userPrompt         the user prompt
     * @param files              the files
     */
    private void addSelectedFiles(@NotNull ChatMessageContext chatMessageContext,
                                  String userPrompt,
                                  List<VirtualFile> files) {
        chatMessageContext.setEditorInfo(new EditorInfo(files));

        String userPromptWithContext = MessageCreationService
            .getInstance()
            .createUserPromptWithContext(chatMessageContext.getProject(), userPrompt, files);

        chatMessageContext.setContext(userPromptWithContext);
    }

    /**
     * Disable the prompt input component.
     */
    private void disableButtons() {
        promptInputComponent.setEnabled(false);
    }

    /**
     * Set the search buttons visibility based on settings.
     */
    public void configureSearchButtonsVisibility() {
        if (LLMStateService.getInstance().getHideSearchButtonsFlag()) {
            tavilySearchBtn.setVisible(false);
            googleSearchBtn.setVisible(false);
        } else {
            tavilySearchBtn.setVisible(!LLMStateService.getInstance().getTavilySearchKey().isEmpty());
            googleSearchBtn.setVisible(!LLMStateService.getInstance().getGoogleSearchKey().isEmpty() &&
                                       !LLMStateService.getInstance().getGoogleCSIKey().isEmpty());
        }
    }
}
