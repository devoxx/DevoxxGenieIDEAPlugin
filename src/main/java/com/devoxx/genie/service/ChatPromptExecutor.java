package com.devoxx.genie.service;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.streaming.StreamingPromptExecutor;
import com.devoxx.genie.service.websearch.WebSearchExecutor;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.FileTypeUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;

public class ChatPromptExecutor {

    private final StreamingPromptExecutor streamingPromptExecutor;
    private final NonStreamingPromptExecutor nonStreamingPromptExecutor;
    private final PromptInputArea promptInputArea;
    private final ConcurrentHashMap<Project, Boolean> isRunningMap = new ConcurrentHashMap<>();

    public ChatPromptExecutor(PromptInputArea promptInputArea) {
        this.promptInputArea = promptInputArea;
        this.streamingPromptExecutor = new StreamingPromptExecutor();
        this.nonStreamingPromptExecutor = new NonStreamingPromptExecutor();
    }

    /**
     * Execute the prompt.
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
                    new WebSearchExecutor().execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunningMap.put(project, false);
                        enableButtons.run();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            promptInputArea.clear();
                            promptInputArea.requestInputFocus();
                        });
                    });
                } else if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode())) {
                    streamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunningMap.put(project, false);
                        enableButtons.run();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            promptInputArea.clear();
                            promptInputArea.requestInputFocus();
                        });
                    });
                } else {
                    nonStreamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunningMap.put(project, false);
                        enableButtons.run();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            promptInputArea.clear();
                            promptInputArea.requestInputFocus();
                        });
                    });
                }
            }
        }.queue();
    }

    /**
     * Process possible command prompt.
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
            editorInfo.setLanguage(FileTypeUtil.getFileType(fileEditorManager.getSelectedFiles()[0]));
        }

        return editorInfo;
    }

    /**
     * Stop streaming or the non-streaming prompt execution
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
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @return the command
     */
    private Optional<String> getCommandFromPrompt(@NotNull ChatMessageContext chatMessageContext,
                                                  PromptOutputPanel promptOutputPanel) {
        String prompt = chatMessageContext.getUserPrompt().trim();
        if (prompt.startsWith("/")) {
            DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();

            if (prompt.toLowerCase().startsWith("/" + FIND_COMMAND + " ")) {
                if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getRagEnabled())) {
                    NotificationUtil.sendNotification(chatMessageContext.getProject(),
                            "The /find command requires RAG to be enabled in settings");
                    return Optional.empty();
                }
                chatMessageContext.setCommandName(FIND_COMMAND);
                return Optional.of(prompt.substring(6).trim());
            }

            // Check for custom prompts
            for (CustomPrompt customPrompt : settings.getCustomPrompts()) {
                if (prompt.equalsIgnoreCase("/" + customPrompt.getName())) {
                    chatMessageContext.setCommandName(customPrompt.getName());
                    return Optional.of(customPrompt.getPrompt());
                }
            }
            promptOutputPanel.showHelpText();
            return Optional.empty();
        }
        return Optional.of(prompt);
    }
}
