package com.devoxx.genie.service;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.streaming.StreamingPromptExecutor;
import com.devoxx.genie.service.websearch.WebSearchExecutor;
import com.devoxx.genie.ui.component.PromptInputArea;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.util.FileTypeUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the Enable buttons
     */
    public void executePrompt(@NotNull ChatMessageContext chatMessageContext,
                              PromptOutputPanel promptOutputPanel,
                              Runnable enableButtons) {

        Project project = chatMessageContext.getProject();
        if (isRunningMap.getOrDefault(project, false)) {
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
                        SwingUtilities.invokeLater(() -> {
                            promptInputArea.clear();
                            promptInputArea.requestInputFocus();
                        });
                    });
                } else if (DevoxxGenieSettingsServiceProvider.getInstance().getStreamMode()) {
                    streamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunningMap.put(project, false);
                        enableButtons.run();
                        SwingUtilities.invokeLater(() -> {
                            promptInputArea.clear();
                            promptInputArea.requestInputFocus();
                        });
                    });
                } else {
                    nonStreamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunningMap.put(project, false);
                        enableButtons.run();
                        SwingUtilities.invokeLater(() -> {
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
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     */
    public Optional<String> updatePromptWithCommandIfPresent(@NotNull ChatMessageContext chatMessageContext,
                                                             PromptOutputPanel promptOutputPanel) {
        Optional<String> commandFromPrompt = getCommandFromPrompt(chatMessageContext.getUserPrompt().trim(), promptOutputPanel);
        chatMessageContext.setUserPrompt(commandFromPrompt.orElse(chatMessageContext.getUserPrompt()));

        // Ensure that EditorInfo is set in the ChatMessageContext
        if (chatMessageContext.getEditorInfo() == null) {
            chatMessageContext.setEditorInfo(getEditorInfo(chatMessageContext.getProject()));
        }

        return commandFromPrompt;
    }

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
     *
     * @param project the project
     */
    public void stopPromptExecution(Project project) {
        if (isRunningMap.getOrDefault(project, false)) {
            isRunningMap.put(project, false);
            streamingPromptExecutor.stopStreaming();
            nonStreamingPromptExecutor.stopExecution();
        }
    }

    /**
     * Get the command from the prompt.
     *
     * @param prompt            the prompt
     * @param promptOutputPanel the prompt output panel
     * @return the command
     */
    private Optional<String> getCommandFromPrompt(@NotNull String prompt,
                                                  PromptOutputPanel promptOutputPanel) {
        if (prompt.startsWith("/")) {
            DevoxxGenieSettingsService settings = DevoxxGenieSettingsServiceProvider.getInstance();

            // Check for custom prompts
            for (CustomPrompt customPrompt : settings.getCustomPrompts()) {
                if (prompt.equalsIgnoreCase("/" + customPrompt.getName())) {
                    prompt = customPrompt.getPrompt();
                    return Optional.of(prompt);
                }
            }
            promptOutputPanel.showHelpText();
            return Optional.empty();
        }
        return Optional.of(prompt);
    }
}
