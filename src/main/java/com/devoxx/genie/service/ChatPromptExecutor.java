package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.streaming.StreamingPromptExecutor;
import com.devoxx.genie.service.websearch.WebSearchExecutor;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.PromptOutputPanel;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.FileUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import static com.devoxx.genie.model.Constant.*;

public class ChatPromptExecutor {

    private static final Logger LOG = Logger.getInstance(ChatPromptExecutor.class);

    private final StreamingPromptExecutor streamingPromptExecutor;
    private final NonStreamingPromptExecutor nonStreamingPromptExecutor;
    private final PromptInputArea promptInputArea;
    private final ConcurrentHashMap<Project, Boolean> isRunningMap = new ConcurrentHashMap<>();

    public ChatPromptExecutor(Project project, PromptInputArea promptInputArea) {
        this.promptInputArea = promptInputArea;
        this.streamingPromptExecutor = new StreamingPromptExecutor();
        this.nonStreamingPromptExecutor = new NonStreamingPromptExecutor(project);
    }

    /**
     * Execute the prompt.
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the Enable buttons
     */
    public void executePrompt(@NotNull ChatMessageContext chatMessageContext,
                              PromptOutputPanel promptOutputPanel,
                              Runnable enableButtons) {

        Project project = chatMessageContext.getProject();
        if (Boolean.TRUE.equals(isRunningMap.getOrDefault(project, false))) {
            stopPromptExecution(project);
            return;
        }

        isRunningMap.put(project, true);

        new Task.Backgroundable(chatMessageContext.getProject(), "Working...", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                if (chatMessageContext.isWebSearchRequested()) {
                    new WebSearchExecutor().execute(chatMessageContext, promptOutputPanel, () ->
                            ApplicationManager.getApplication().invokeLater(() -> cleanChat(project, enableButtons)));
                } else if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode())) {
                    streamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () ->
                        ApplicationManager.getApplication().invokeLater(() -> cleanChat(project, enableButtons)));
                } else {
                    nonStreamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () ->
                        ApplicationManager.getApplication().invokeLater(() -> cleanChat(project, enableButtons)));
                }
            }

            @Override
            public void onCancel() {
                super.onCancel();
                // Handle cancellation if needed
                LOG.info("Prompt execution was cancelled.");
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                super.onThrowable(error);
                // Handle other exceptions
                if (!(error instanceof CancellationException)) {
                    LOG.error("Error occurred while processing chat message", error);
                    ErrorHandler.handleError(chatMessageContext.getProject(), error);
                }
            }
        }.queue();
    }

    private void cleanChat(Project project, @NotNull Runnable enableButtons) {
        isRunningMap.put(project, false);
        enableButtons.run();
        FileListManager.getInstance().storeAddedFiles(project);
        promptInputArea.clear();
        promptInputArea.requestInputFocus();
    }

    /**
     * Process possible command prompt.
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     */
    public Optional<String> updatePromptWithCommandIfPresent(@NotNull ChatMessageContext chatMessageContext,
                                                             PromptOutputPanel promptOutputPanel) {
        Optional<String> commandFromPrompt = getCommandFromPrompt(chatMessageContext, promptOutputPanel);
        chatMessageContext.setUserPrompt(commandFromPrompt.orElse(chatMessageContext.getUserPrompt()));

        // Ensure that EditorInfo is set in the ChatMessageContext
        if (chatMessageContext.getEditorInfo() == null) {
            chatMessageContext.setEditorInfo(getEditorInfo(chatMessageContext.getProject()));
        }

        return commandFromPrompt;
    }

    /**
     * Get the editor info.
     *
     * @param project the project
     * @return the editor info
     */
    private @NotNull EditorInfo getEditorInfo(Project project) {
        EditorInfo editorInfo = new EditorInfo();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        Editor editor = fileEditorManager.getSelectedTextEditor();

        if (editor != null) {
            String selectedText = editor.getSelectionModel().getSelectedText();
            if (selectedText != null && !selectedText.isEmpty()) {
                editorInfo.setSelectedText(selectedText);
            } else {
                VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
                if (openFiles.length > 0) {
                    editorInfo.setSelectedFiles(Arrays.asList(openFiles));
                }
            }
            editorInfo.setLanguage(FileUtil.getFileType(fileEditorManager.getSelectedFiles()[0]));
        }

        return editorInfo;
    }

    /**
     * Stop streaming or the non-streaming prompt execution
     *
     * @param project the project
     */
    public void stopPromptExecution(Project project) {
        if (Boolean.TRUE.equals(isRunningMap.getOrDefault(project, false))) {
            isRunningMap.put(project, false);
            streamingPromptExecutor.stopStreaming();
            nonStreamingPromptExecutor.stopExecution();
        }
    }

    /**
     * Get the command from the prompt.
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @return the command
     */
    public Optional<String> getCommandFromPrompt(@NotNull ChatMessageContext chatMessageContext,
                                                  PromptOutputPanel promptOutputPanel) {
        String prompt = chatMessageContext.getUserPrompt().trim();

        if (!isCommand(prompt)) {
            return Optional.of(prompt);
        }

        if (isFindCommand(prompt)) {
            return handleFindCommand(chatMessageContext, prompt);
        }

        return handleCustomCommand(chatMessageContext, promptOutputPanel, prompt);
    }

    private boolean isCommand(@NotNull String prompt) {
        return prompt.startsWith(COMMAND_PREFIX);
    }

    private boolean isFindCommand(@NotNull String prompt) {
        return prompt.startsWith(COMMAND_PREFIX + FIND_COMMAND);
    }

    private Optional<String> handleFindCommand(@NotNull ChatMessageContext chatMessageContext, String prompt) {
        if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getRagEnabled())) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                    "The /find command requires RAG to be enabled in settings");
            return Optional.empty();
        }

        if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getRagActivated())) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                    "The /find command requires RAG to be turned on");
            return Optional.empty();
        }

        chatMessageContext.setCommandName(FIND_COMMAND);
        return Optional.of(prompt.substring(6).trim());
    }

    private Optional<String> handleCustomCommand(@NotNull ChatMessageContext chatMessageContext,
                                                 PromptOutputPanel promptOutputPanel,
                                                 String prompt) {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();
        List<CustomPrompt> customPrompts = settings.getCustomPrompts();

        Optional<CustomPrompt> matchingPrompt = customPrompts.stream()
                .filter(customPrompt -> prompt.startsWith(COMMAND_PREFIX + customPrompt.getName()))
                .findFirst();

        if (matchingPrompt.isPresent()) {
            return processMatchingPrompt(chatMessageContext, promptOutputPanel, prompt, matchingPrompt.get());
        } else {
            LOG.debug("No matching command prompt found");
            return Optional.of(prompt);
        }
    }

    private Optional<String> processMatchingPrompt(@NotNull ChatMessageContext chatMessageContext,
                                                   PromptOutputPanel promptOutputPanel, @NotNull String prompt, @NotNull CustomPrompt matchingPrompt) {
        if (matchingPrompt.getName().equalsIgnoreCase(HELP_COMMAND)) {
            promptOutputPanel.showHelpText();
            return Optional.empty();
        }

        chatMessageContext.setCommandName(matchingPrompt.getName());
        String customPrompt = matchingPrompt.getPrompt() + " " + prompt.replaceFirst(COMMAND_PREFIX + matchingPrompt.getName(), "");
        return Optional.of(customPrompt);
    }
}
