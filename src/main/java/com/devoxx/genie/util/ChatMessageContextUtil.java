package com.devoxx.genie.util;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.ChatContextParameters;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.EditorUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.devoxx.genie.action.AddSnippetAction.*;

public class ChatMessageContextUtil {

    public static final int ZERO_SECONDS = 0;
    public static final int SIXTY_SECONDS = 60;

    private ChatMessageContextUtil() {
    }

    public static @NotNull ChatMessageContext createContext(@NotNull ChatContextParameters chatContextParameters) {

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        ChatMessageContext chatMessageContext = ChatMessageContext.builder()
                .project(chatContextParameters.project())
                .id(String.valueOf(System.currentTimeMillis()))
                .userPrompt(chatContextParameters.userPromptText())
                .languageModel(chatContextParameters.languageModel())
                .webSearchRequested(stateService.getWebSearchActivated() && (stateService.isGoogleSearchEnabled() || stateService.isTavilySearchEnabled()))
                .executionTimeMs(0)
                .cost(0)
                .build();

        if (stateService.getStreamMode()) {
            chatMessageContext.setStreamingChatLanguageModel(chatContextParameters.chatModelProvider().getStreamingChatLanguageModel(chatMessageContext));
        } else {
            chatMessageContext.setChatLanguageModel(chatContextParameters.chatModelProvider().getChatLanguageModel(chatMessageContext));
        }

        chatMessageContext.setTimeout(stateService.getTimeout() == ZERO_SECONDS ? SIXTY_SECONDS : stateService.getTimeout());

        setWindowContext(chatMessageContext,
                         chatContextParameters.editorFileButtonManager(),
                         chatContextParameters.projectContext(),
                         chatContextParameters.isProjectContextAdded());

        return chatMessageContext;
    }

    /**
     * Set the window context.
     *
     * @param chatMessageContext      the chat message context
     * @param editorFileButtonManager the editor file button manager
     * @param projectContext          the project context
     * @param isProjectContextAdded   the is project context added
     */
    private static void setWindowContext(@NotNull ChatMessageContext chatMessageContext,
                                         EditorFileButtonManager editorFileButtonManager,
                                         String projectContext,
                                         boolean isProjectContextAdded) {

        // First handle any existing project context
        if (projectContext != null && isProjectContextAdded) {
            // If the full project is added as context, set it and ignore any attached files
            chatMessageContext.setFilesContext(projectContext);
        } else {
            // We don't include separate added files to the context if the full project is already included
            processAttachedFiles(chatMessageContext);

            // Set editor info if available
            Editor selectedTextEditor = editorFileButtonManager.getSelectedTextEditor();
            if ((chatMessageContext.getFilesContext() == null || chatMessageContext.getFilesContext().isEmpty()) &&
                selectedTextEditor != null) {
                addDefaultEditorInfoToMessageContext(selectedTextEditor, chatMessageContext);
            }
        }
    }

    /**
     * Process attached files.
     * We only include newly added files to the conversation to avoid duplicate content!
     * The previously added files are stored in the FileListManager to support this logic.
     * @param chatMessageContext the chat message context
     */
    private static void processAttachedFiles(@NotNull ChatMessageContext chatMessageContext) {
        FileListManager fileListManager = FileListManager.getInstance();
        List<VirtualFile> files = fileListManager.getFiles(chatMessageContext.getProject());
        List<VirtualFile> previouslyAddedFiles = fileListManager.getPreviouslyAddedFiles(chatMessageContext.getProject());

        if (!previouslyAddedFiles.isEmpty()) {
            // Create a new list containing only new files
            files = files.stream()
                    .filter(file -> !previouslyAddedFiles.contains(file))
                    .toList();
        }

        if (!files.isEmpty()) {
            try {
                String newContext = MessageCreationService
                        .getInstance()
                        .createAttachedFilesContext(chatMessageContext.getProject(), files);

                chatMessageContext.setFilesContext(newContext);
            } catch (Exception ex) {
                ErrorHandler.handleError(chatMessageContext.getProject(), ex);
            }
        }
    }

    private static void addDefaultEditorInfoToMessageContext(Editor editor,
                                                             @NotNull ChatMessageContext chatMessageContext) {

        FileListManager.getInstance().clear(chatMessageContext.getProject());

        EditorInfo editorInfo = EditorUtil.getEditorInfo(editor);
        chatMessageContext.setEditorInfo(editorInfo);

        Document document = editor.getDocument();

        VirtualFile originalFile = editor.getVirtualFile();

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            int startOffset = selectionModel.getSelectionStart();
            int endOffset = selectionModel.getSelectionEnd();
            int startLine = document.getLineNumber(startOffset);
            int endLine = document.getLineNumber(endOffset);
            originalFile.putUserData(ORIGINAL_FILE_KEY, originalFile);
            originalFile.putUserData(SELECTED_TEXT_KEY, selectedText);
            originalFile.putUserData(SELECTION_START_KEY, startOffset);
            originalFile.putUserData(SELECTION_END_KEY, endOffset);
            originalFile.putUserData(SELECTION_START_LINE_KEY, startLine);
            originalFile.putUserData(SELECTION_END_LINE_KEY, endLine);
        } else {
            originalFile.putUserData(SELECTED_TEXT_KEY, "");
        }

        FileListManager.getInstance().addFile(chatMessageContext.getProject(), editor.getVirtualFile());
    }

    /**
     * Check if the language model is an OpenAI O1 model because that doesn't support system prompts.
     *
     * @param languageModel the language model
     * @return true if the language model is an OpenAI O1 model
     */
    public static boolean isOpenAIo1Model(LanguageModel languageModel) {
        return languageModel != null &&
                languageModel.getProvider() == ModelProvider.OpenAI &&
                languageModel.getModelName() != null &&
                languageModel.getModelName().toLowerCase().startsWith("o1");
    }
}
