package com.devoxx.genie.service.prompt.command;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.util.FileUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Central processor for all prompt commands.
 */
public class PromptCommandProcessor {

    private final List<PromptCommand> commands;

    public static PromptCommandProcessor getInstance() {
        return ApplicationManager.getApplication().getService(PromptCommandProcessor.class);
    }

    public PromptCommandProcessor() {
        // Initialize all command processors
        this.commands = List.of(
            new FindCommand(),
            new HelpCommand(),
            new CustomPromptCommand()
        );
    }

    /**
     * Process the prompt for any commands, updating the context as needed.
     *
     * @param chatMessageContext The context to process
     * @param promptOutputPanel The panel for displaying output
     * @return Optional containing processed prompt if execution should continue,
     *         or empty if execution should stop
     */
    public Optional<String> processCommands(@NotNull ChatMessageContext chatMessageContext,
                                          @NotNull PromptOutputPanel promptOutputPanel) {
        // Ensure editor info is populated, but check if MCP is enabled first
        if (chatMessageContext.getEditorInfo() == null && !MCPService.isMCPEnabled()) {
            chatMessageContext.setEditorInfo(getEditorInfo(chatMessageContext.getProject()));
        }
        
        String prompt = chatMessageContext.getUserPrompt().trim();
        
        // Find the first matching command
        for (PromptCommand command : commands) {
            if (command.matches(prompt)) {
                // Process the command and return its result
                Optional<String> result = command.process(chatMessageContext, promptOutputPanel);

                // Update the user prompt with the processed version
                result.ifPresent(chatMessageContext::setUserPrompt);
                
                return result;
            }
        }
        
        // No command matched, continue with normal processing
        return Optional.of(prompt);
    }

    /**
     * Get editor information for context enrichment.
     *
     * @param project The current project
     * @return EditorInfo containing selected text, files, and language
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
}
