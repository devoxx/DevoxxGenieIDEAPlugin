package com.devoxx.genie.util;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.ChatContextParameters;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import dev.langchain4j.data.message.UserMessage;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.ui.component.button.EditorFileButtonManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.EditorUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.devoxx.genie.action.AddSnippetAction.*;

@Slf4j
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

        // CLI Runners bypass Langchain4J — no chat model needed
        if (chatContextParameters.languageModel().getProvider() != ModelProvider.CLIRunners) {
            if (Boolean.TRUE.equals(stateService.getStreamMode())) {
                chatMessageContext.setStreamingChatModel(chatContextParameters.chatModelProvider().getStreamingChatLanguageModel(chatMessageContext));
            } else {
                chatMessageContext.setChatModel(chatContextParameters.chatModelProvider().getChatLanguageModel(chatMessageContext));
            }
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

            // Set editor info if available and if useFileInEditor setting is enabled
            Editor selectedTextEditor = editorFileButtonManager.getSelectedTextEditor();

            // Include selected text (if any)
            if ((chatMessageContext.getFilesContext() == null || chatMessageContext.getFilesContext().isEmpty()) &&
                selectedTextEditor != null) {
                addDefaultEditorInfoToMessageContext(selectedTextEditor, chatMessageContext);
            }
        }
    }

    /**
     * Process attached files.
     * Include all non-image files that have been added to the current conversation.
     * Image files are handled separately by MessageCreationService.addImages().
     * Files are only cleared when starting a new conversation.
     * @param chatMessageContext the chat message context
     */
    private static void processAttachedFiles(@NotNull ChatMessageContext chatMessageContext) {
        FileListManager fileListManager = FileListManager.getInstance();
        // Only process non-image files for text context; images are handled by addImages()
        List<VirtualFile> files = fileListManager.getNonImageFiles(chatMessageContext.getProject());

        log.debug("processAttachedFiles: {} non-image files, {} image files",
                files.size(),
                fileListManager.getImageFiles(chatMessageContext.getProject()).size());

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

        // Only clear non-image files; image files are preserved for addImages() processing
        FileListManager.getInstance().clearNonImageFiles(chatMessageContext.getProject());

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
            // When text is selected, always add the file to FileListManager
            FileListManager.getInstance().addFile(chatMessageContext.getProject(), originalFile);
        } else {
            originalFile.putUserData(SELECTED_TEXT_KEY, "");
            // When no text is selected, only add file if useFileInEditor is enabled
            if (originalFile != null && Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getUseFileInEditor())) {
                FileListManager.getInstance().addFile(chatMessageContext.getProject(), originalFile);
            }
        }

    }

    /**
     * Check if the chat message context contains multimodal content (e.g., images).
     *
     * @param context the chat message context
     * @return true if the user message contains non-text content
     */
    public static boolean hasMultimodalContent(@NotNull ChatMessageContext context) {
        UserMessage userMessage = context.getUserMessage();
        return userMessage != null && !userMessage.hasSingleText();
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
